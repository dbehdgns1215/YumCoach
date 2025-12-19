from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import AsyncOpenAI
import os
import re
from pathlib import Path
from typing import Optional
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(
    title="YumCoach Chatbot API",
    version="1.0.0"
)

client = AsyncOpenAI(
    base_url="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
    api_key=os.getenv("OPENAI_API_KEY")
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
    user_profile: dict = None
    report_data: dict = None

class ChatResponse(BaseModel):
    reply: str
    detected_hashtag: str = None

@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    # 식단 코칭 챗봇 API
    
    ## 현재 지원 기능:
    - #식단: 일반 식단 상담 (리포트 불필요)
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
    
    ## 사용 예시 1: 식단 상담
    ```json
    {
        "message": "#식단 다이어트 식단 추천해줘",
        "user_id": "2",
        "user_profile": {
            "name": "홍길동",
            "height": 175,
            "weight": 80,
            "diabetes": 1,
            "high_blood_pressure": 1,
            "hyperlipidemia": 1,
            "kidney_disease": 1
        }
    }
    ```
    
    ## 사용 예시 2: 상담
    ```json
    {
        "message": "#상담 요즘 다이어트가 너무 힘들어",
        "user_id": "2",
        "user_profile": {
            "name": "홍길동",
            "height": 175,
            "weight": 80,
            "diabetes": 1,
            "high_blood_pressure": 1,
            "hyperlipidemia": 1,
            "kidney_disease": 1
        }
    }
    ```
    """
    try:
        # 해시태그 추출
        hashtag, clean_message = extract_hashtag(request.message)
        
        # 시스템 프롬프트 생성
        system_prompt = build_system_prompt(
            hashtag, 
            request.user_profile,
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
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# 실행: uvicorn main:app --host 0.0.0.0 --port 8001