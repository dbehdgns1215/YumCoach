from rag_service.flows.diet_recommend import diet_recommend_flow
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import AsyncOpenAI
import os
import re
from pathlib import Path
from typing import Optional, Dict, Any, List
from dotenv import load_dotenv
import sys
import logging

BASE_DIR = Path(__file__).resolve().parent.parent
ENV_PATH = BASE_DIR / ".env"
load_dotenv(dotenv_path=ENV_PATH)

# Logging 설정 (DEBUG로 강제 설정)
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# RAG Service import
sys.path.insert(0, str(BASE_DIR))

app = FastAPI(
    title="YumCoach Chatbot API",
    version="1.0.0"
)

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    raise RuntimeError(
        "OPENAI_API_KEY is missing. Set it in .env or environment.")

client = AsyncOpenAI(
    base_url="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
    api_key=OPENAI_API_KEY
)

# 해시태그 -> 프롬프트 파일 매핑
HASHTAG_TO_FILE = {
    "#주간리포트": "weekly_report.txt",
    "#일일리포트": "daily_report.txt",
    "#식단": "diet.txt",
    "#상담": "counsel.txt"
}


def load_prompt(filename: str) -> str:
    """프롬프트 파일 로드"""
    prompt_path = Path(__file__).parent / "prompts" / filename
    try:
        with open(prompt_path, 'r', encoding='utf-8') as f:
            return f.read()
    except FileNotFoundError:
        print(f"Warning: {filename} not found, using default")
        return load_prompt("default.txt")


def extract_hashtag(message: str) -> tuple[Optional[str], str]:
    """메시지에서 해시태그 추출 (위치 무관)"""
    pattern = r'#(주간리포트|일일리포트|식단|상담)'
    match = re.search(pattern, message)

    if match:
        hashtag = match.group(0)
        clean_message = re.sub(pattern, '', message).strip()
        return hashtag, clean_message

    return None, message


def format_health_status(user_profile: dict) -> str:
    """건강 상태를 텍스트로 변환"""
    conditions = []

    # 0=없음, 1=있음
    if user_profile.get('diabetes') == 1:
        conditions.append('당뇨')
    if user_profile.get('high_blood_pressure') == 1:
        conditions.append('고혈압')
    if user_profile.get('hyperlipidemia') == 1:
        conditions.append('고지혈증')
    if user_profile.get('kidney_disease') == 1:
        conditions.append('신장질환')

    if not conditions:
        return '특별한 질환 없음'

    return ', '.join(conditions) + ' 보유'


def build_system_prompt(
    hashtag: Optional[str],
    hashtag: Optional[str],
    user_profile: Optional[dict] = None,
    report_data: Optional[dict] = None
) -> str:
    """시스템 프롬프트 생성"""
    # 해시태그에 맞는 프롬프트 파일 로드
    if hashtag and hashtag in HASHTAG_TO_FILE:
        base_prompt = load_prompt(HASHTAG_TO_FILE[hashtag])
    else:
        base_prompt = load_prompt("default.txt")

    # 사용자 프로필 정보 주입
    if user_profile:
        name = user_profile.get('name', '사용자')
        height = user_profile.get('height', '알 수 없음')
        weight = user_profile.get('weight', '알 수 없음')
        health_status = format_health_status(user_profile)

        # 템플릿 치환
        try:
            base_prompt = base_prompt.format(
                name=name,
                height=height,
                weight=weight,
                health_status=health_status
            )
        except KeyError:
            # 템플릿 변수가 없는 경우 (기본 프롬프트)
            pass

    # TODO: 리포트 데이터 주입

    return base_prompt


class ChatRequest(BaseModel):
    message: str
    user_id: str = None
    user: Dict[str, Any] = None
    user_health: Dict[str, Any] = None
    user_profile: dict = None
    dietary_restrictions: List[str] = None
    today_report: Dict[str, Any] = None
    report_data: dict = None
    user_role: str = None  # "BASIC", "ADVANCED" 등


class ChatResponse(BaseModel):
    reply: str
    detected_hashtag: str = None


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    # 식단 코칭 챗봇 API


    ## 현재 지원 기능:
    - #식단: 식단 추천 (리포트 기반, ADVANCED 유저만 사용 가능)
    - #상담: 식단 관련 고민 상담 (리포트 불필요)
    - #일일리포트: 일일 식단 분석 (리포트 필요, 현재 미구현)
    - #주간리포트: 주간 식단 분석 (리포트 필요, 현재 미구현)


    ## user_profile 예시:
    ```json
    {
        "name": "테스트 유저",
        "height": 175,
        "weight": 70,
        "diabetes": 0,
        "high_blood_pressure": 0,
        "hyperlipidemia": 0,
        "kidney_disease": 0
    }
    ```

    ## 사용 예시: #식단 추천 (ADVANCED 유저)
    ```json
    {
        "message": "#식단 지금까지의 식단을 기반으로 저녁에 뭘 먹을지 추천해줘",
        "user_id": "2",
        "user_role": "ADVANCED",
        "user": {
            "name": "홍길동",
            "age": 30
        },
        "user_health": {
            "height": 175,
            "weight": 70,
            "activity_level": "MEDIUM"
        },
        "dietary_restrictions": ["해산물", "견과류"],
        "today_report": {
            "type": "DAILY",
            "totalCalories": 1800,
            "proteinG": 55,
            "carbG": 200,
            "fatG": 50,
            "mealCount": 3,
            "meals": [...]
        }
    }
    ```
    """
    try:
        # 해시태그 추출
        logger.debug(f"📥 /chat 요청 수신: message={request.message[:50]}...")
        hashtag, clean_message = extract_hashtag(request.message)
        logger.debug(f"   추출된 hashtag: {hashtag}")

        # ===== #식단 (RAG 기반 추천) =====
        if hashtag == "#식단":
            logger.info(
                f"🎯 #식단 요청 감지. 사용자: {request.user.get('name') if request.user else 'unknown'}")
            # Role 체크
            user_role = request.user_role or ""
            if user_role != "ADVANCED":
                raise HTTPException(
                    status_code=403,
                    detail=f"이 기능은 ADVANCED 유저만 사용 가능합니다. 현재 역할: {user_role or 'UNKNOWN'}"
                )

            # 필수 필드 체크
            if not request.today_report:
                raise HTTPException(
                    status_code=400,
                    detail="오늘의 리포트 데이터(today_report)가 필요합니다."
                )

            if not request.user or not request.user.get("name"):
                raise HTTPException(
                    status_code=400,
                    detail="사용자 정보(user)가 필요합니다."
                )

            if not request.user_health:
                raise HTTPException(
                    status_code=400,
                    detail="사용자 건강 정보(user_health)가 필요합니다."
                )

            # RAG 플로우 호출
            logger.info(f"🚀 diet_recommend_flow.recommend() 호출...")
            result = await diet_recommend_flow.recommend(
                message=clean_message,
                user_id=request.user_id or "unknown",
                user=request.user,
                user_health=request.user_health,
                dietary_restrictions=request.dietary_restrictions or [],
                today_report=request.today_report,
                meal_type="dinner"  # 기본값
            )
            logger.info(
                f"✅ recommend() 완료. result 타입: {type(result)}, keys: {result.keys() if isinstance(result, dict) else 'N/A'}")

            import json as _json
            # 프롬프트 요구 형식(JSON)만 reply에 담아 반환
            llm_reply_obj = {
                "summary": result.get("summary", ""),
                "meal_suggestion": result.get("meal_suggestion", ""),
                "tips": result.get("tips", []),
            }

            return ChatResponse(
                reply=_json.dumps(llm_reply_obj, ensure_ascii=False),
                detected_hashtag=hashtag
            )

        # ===== 기존 로직 (일반 챗봇) =====
        # 시스템 프롬프트 생성
        system_prompt = build_system_prompt(
            hashtag,
            request.user_profile or request.user,
            request.report_data
        )

        # 메시지 구성
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": clean_message}
        ]

        # API 호출
        stream = await client.chat.completions.create(
            model='gpt-5-nano',
            messages=messages,
            stream=False,
        )

        return ChatResponse(
            reply=stream.choices[0].message.content,
            detected_hashtag=hashtag
        )

    except HTTPException:
        raise
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

# 실행: uvicorn main:app --host 0.0.0.0 --port 8001
