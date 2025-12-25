from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import os
import re
from pathlib import Path
from typing import Optional, Dict, Any
from dotenv import load_dotenv
import httpx

load_dotenv()

app = FastAPI(
    title="YumCoach Chatbot API",
    version="1.0.0"
)

# ✅ Gemini(GMS) 설정
GMS_KEY = os.getenv("GMS_KEY") or os.getenv("GOOGLE_API_KEY")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash-lite")

# 해시태그 -> 프롬프트 파일 매핑
HASHTAG_TO_FILE = {
    "#주간리포트": "weekly_report.txt",
    "#일일리포트": "daily_report.txt",
    "#식단": "diet.txt",
    "#상담": "counsel.txt",
    "#기본": "default.txt"
}


def load_prompt(filename: str) -> str:
    """프롬프트 파일 로드"""
    prompt_path = Path(__file__).parent / "prompts" / filename
    try:
        with open(prompt_path, "r", encoding="utf-8") as f:
            return f.read()
    except FileNotFoundError:
        print(f"Warning: {filename} not found, using default")
        # default.txt도 없을 수 있으니 무한재귀 방지
        default_path = Path(__file__).parent / "prompts" / "default.txt"
        if default_path.exists():
            return default_path.read_text(encoding="utf-8")
        return "You are a helpful diet coaching assistant."


def extract_hashtag(message: str) -> tuple[Optional[str], str]:
    """메시지에서 해시태그 추출 (위치 무관)"""
    pattern = r"#(주간리포트|일일리포트|식단|상담)"
    match = re.search(pattern, message)

    if match:
        hashtag = match.group(0)
        clean_message = re.sub(pattern, "", message).strip()
        return hashtag, clean_message

    return "#기본", message


def format_health_status(user_profile: Dict[str, Any]) -> str:
    """건강 상태를 텍스트로 변환"""
    conditions = []

    # 0=없음, 1=있음
    if user_profile.get("diabetes") == 1:
        conditions.append("당뇨")
    if user_profile.get("high_blood_pressure") == 1:
        conditions.append("고혈압")
    if user_profile.get("hyperlipidemia") == 1:
        conditions.append("고지혈증")
    if user_profile.get("kidney_disease") == 1:
        conditions.append("신장질환")

    if not conditions:
        return "특별한 질환 없음"

    return ", ".join(conditions) + " 보유"


def build_system_prompt(
    hashtag: Optional[str],
    user_profile: Optional[Dict[str, Any]] = None,
    report_data: Optional[Dict[str, Any]] = None
) -> str:
    """시스템 프롬프트 생성"""
    # 해시태그에 맞는 프롬프트 파일 로드
    if hashtag and hashtag in HASHTAG_TO_FILE:
        base_prompt = load_prompt(HASHTAG_TO_FILE[hashtag])
    else:
        base_prompt = load_prompt("default.txt")

    # 사용자 프로필 정보 주입
    if user_profile:
        name = user_profile.get("name", "사용자")
        height = user_profile.get("height", "알 수 없음")
        weight = user_profile.get("weight", "알 수 없음")
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

    # TODO: 리포트 데이터 주입 (필요 시 report_data를 prompt에 반영)

    return base_prompt


class ChatRequest(BaseModel):
    message: str
    user_id: Optional[str] = None
    user_profile: Optional[Dict[str, Any]] = None
    report_data: Optional[Dict[str, Any]] = None


class ChatResponse(BaseModel):
    reply: str
    detected_hashtag: Optional[str] = None


def parse_gemini_text(resp_json: Dict[str, Any]) -> str:
    """
    Gemini 응답 파싱:
    - 정상 케이스: candidates[0].content.parts[*].text 합치기
    - 방어적으로 처리
    """
    candidates = resp_json.get("candidates") or []
    if not candidates:
        return ""

    content = candidates[0].get("content") or {}
    parts = content.get("parts") or []
    texts = []
    for p in parts:
        t = p.get("text")
        if isinstance(t, str):
            texts.append(t)

    raw = "\n".join(texts).strip()

    # 코드 펜스(````, ```json)로 감싸진 경우 제거
    if raw.startswith("```"):
        lines = raw.splitlines()
        # 첫 줄의 ``` 혹은 ```json 제거
        if lines:
            lines = lines[1:]
        # 마지막 줄이 ```이면 제거
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        raw = "\n".join(lines).strip()

    return raw


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    try:
        if not GMS_KEY:
            raise HTTPException(
                status_code=500, detail="GMS_KEY (or GOOGLE_API_KEY) is not set")

        # 해시태그 추출
        hashtag, clean_message = extract_hashtag(request.message)

        # 시스템 프롬프트 생성
        system_prompt = build_system_prompt(
            hashtag,
            request.user_profile,
            request.report_data
        )

        # ✅ Gemini REST payload (roles 명시 + systemInstruction 사용)
        payload = {
            "systemInstruction": {"parts": [{"text": system_prompt}]},
            "contents": [
                {"role": "user", "parts": [{"text": clean_message}]}
            ],
        }
        url = (
            f"https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta/"
            f"models/{GEMINI_MODEL}:generateContent?key={GMS_KEY}"
        )

        async with httpx.AsyncClient(timeout=60) as client:
            r = await client.post(
                url,
                headers={"Content-Type": "application/json"},
                json=payload
            )
            # GMS 에러 바디를 detail로 보여주기 위해 raise 전에 text 확보
            if r.status_code >= 400:
                raise HTTPException(
                    status_code=r.status_code,
                    detail=r.text
                )

            data = r.json()

        reply_text = parse_gemini_text(data) or "답변을 생성하지 못했어요. 다시 시도해 주세요."

        return ChatResponse(
            reply=reply_text,
            detected_hashtag=hashtag
        )

    except HTTPException:
        raise
    except Exception as e:
        s
        raise HTTPException(status_code=500, detail=str(e))


# 실행 예시:
#   export GMS_KEY="..."
#   uvicorn main:app --host 0.0.0.0 --port 8077 --reload
