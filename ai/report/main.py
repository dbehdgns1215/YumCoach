from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import AsyncOpenAI
import os
import json
from pathlib import Path
from typing import Optional, Any, Dict
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
    # report can be passed as a dict (preferred) or a JSON string
    report: Any

# ========== 엔드포인트 ==========
@app.post("/analyze-report")
async def analyze_report(req: AnalyzeReportRequest):
    try:
        report_json = req.report

        # normalize report_json to a dict if a JSON string was provided
        if isinstance(report_json, str):
            try:
                report_json = json.loads(report_json)
            except Exception:
                # leave as-is (will be included in user content as string)
                pass

        system_prompt = load_prompt("report_analysis_prompt.txt")

        # Attempt to extract user info from report payload for prompt substitution
        name = ""
        height = ""
        weight = ""
        activity_level = ""
        age = ""
        dietary_restrictions = ""
        health_status = ""

        try:
            if isinstance(report_json, dict):
                # common locations: top-level keys, or nested under 'user'/'userInfo'
                user_obj = None
                if "user" in report_json and isinstance(report_json["user"], dict):
                    user_obj = report_json["user"]
                elif "userInfo" in report_json and isinstance(report_json["userInfo"], dict):
                    user_obj = report_json["userInfo"]
                else:
                    # also accept keys directly on the report
                    user_obj = report_json

                name = user_obj.get("name", "") if isinstance(user_obj, dict) else ""
                height = user_obj.get("height", "") if isinstance(user_obj, dict) else ""
                weight = user_obj.get("weight", "") if isinstance(user_obj, dict) else ""
                activity_level = user_obj.get("activity_level", "") if isinstance(user_obj, dict) else user_obj.get("activityLevel", "") if isinstance(user_obj, dict) else ""
                age = user_obj.get("age", "") if isinstance(user_obj, dict) else ""

                # dietary_restrictions may be a list or string
                dr = user_obj.get("dietary_restrictions", None) if isinstance(user_obj, dict) else None
                if dr is None:
                    dr = user_obj.get("dietaryRestrictions", None) if isinstance(user_obj, dict) else None
                if isinstance(dr, list):
                    dietary_restrictions = ", ".join([str(x) for x in dr])
                elif dr is not None:
                    dietary_restrictions = str(dr)

                # health_status may be a string or object
                hs = user_obj.get("health_status", None) if isinstance(user_obj, dict) else None
                if hs is None:
                    hs = user_obj.get("healthStatus", None) if isinstance(user_obj, dict) else None
                if isinstance(hs, dict):
                    vals = []
                    for k, v in hs.items():
                        if v:
                            vals.append(k)
                    health_status = ", ".join(vals)
                elif hs is not None:
                    health_status = str(hs)
        except Exception:
            # ignore extraction errors and continue with blanks
            pass

        # substitute placeholders in system prompt if present
        try:
            if system_prompt:
                system_prompt = system_prompt.replace("{name}", str(name))
                system_prompt = system_prompt.replace("{height}", str(height))
                system_prompt = system_prompt.replace("{weight}", str(weight))
                system_prompt = system_prompt.replace("{activity_level}", str(activity_level))
                system_prompt = system_prompt.replace("{age}", str(age))
                system_prompt = system_prompt.replace("{dietary_restrictions}", str(dietary_restrictions))
                system_prompt = system_prompt.replace("{health_status}", str(health_status))
        except Exception:
            pass

        user_content = f"다음 식단 리포트를 분석하세요:\n\n{json.dumps(report_json, ensure_ascii=False)}\n\n**필수:**\n- 한국어 JSON만 출력\n- insights 정확히 3개 (good 1개, warn 1개, keep 1개)\n- coachMessage와 nextAction 필수"

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
    

    # 실행: uvicorn main:app --host 0.0.0.0 --port 8000