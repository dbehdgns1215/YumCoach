# chatbot/main.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import AsyncOpenAI
import os
from dotenv import load_dotenv

load_dotenv()

app = FastAPI()
client = AsyncOpenAI(
    base_url="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
    api_key=os.getenv("OPENAI_API_KEY")
)

class ChatRequest(BaseModel):
    message: str
    user_id: str = None

class ChatResponse(BaseModel):
    reply: str

@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    try:
        stream = await client.chat.completions.create(
            model='gpt-5-nano',
            messages=[
                {"role": "system", "content": "당신은 식단 코칭 AI입니다."},
                {"role": "user", "content": request.message}
            ],
            stream=False
        )
        return ChatResponse(reply=stream.choices[0].message.content)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# 실행: uvicorn main:app --host 0.0.0.0 --port 8001