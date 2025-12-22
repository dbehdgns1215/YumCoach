from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import AsyncOpenAI
import os
import json
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

def load_prompt(filename: str) -> str:
    """프롬프트 파일 로드"""
    prompt_path = Path(__file__).parent / "prompts" / filename
    try:
        with open(prompt_path, 'r', encoding='utf-8') as f:
            return f.read()
    except FileNotFoundError:
        print(f"Warning: {filename} not found")
        return ""

# ========== Request/Response 모델 ==========
class ChatRequest(BaseModel):
    message: str
    user_id: str = None

class ChatResponse(BaseModel):
    reply: str
    detected_hashtag: str = None

class AnalyzeReportRequest(BaseModel):
    report: str

# ========== 엔드포인트 ==========
@app.post("/analyze-report")
async def analyze_report(req: AnalyzeReportRequest):
    try:
        report_json = req.report
        system_prompt = load_prompt("report_analysis_prompt.txt")
        
        if not system_prompt:
            system_prompt = """당신은 전문 영양 코치입니다.
반드시 한국어로 JSON만 반환하세요.

{
  "heroTitle": "요약",
  "heroLine": "부제",
  "score": 85,
  "coachMessage": "메시지",
  "nextAction": "행동",
  "insights": [{"kind": "good", "title": "제목", "body": "내용"}]
}"""

        user_content = f"다음 리포트를 분석하세요:\n\n{report_json}\n\n반드시 한국어로 JSON만 출력."

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_content}
        ]

        stream = await client.chat.completions.create(
            model=os.getenv("OPENAI_MODEL", "gpt-5-nano"),
            messages=messages,
            stream=False,
        )

        content = stream.choices[0].message.content
        
        # 마크다운 제거
        if "```json" in content:
            content = content.split("```json")[1].split("```")[0].strip()
        elif "```" in content:
            content = content.split("```")[1].split("```")[0].strip()

        parsed = json.loads(content)
        return parsed

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))