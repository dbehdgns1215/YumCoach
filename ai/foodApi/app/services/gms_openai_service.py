import httpx
from app.config import settings


async def call_gms_openai_raw(messages: list[dict]) -> str:
    """
    GMS OpenAI API 호출
    URL: https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions
    """
    url = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions"

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {settings.GMS_OPENAI_API_KEY}",
    }

    payload = {
        "model": settings.GMS_OPENAI_MODEL,
        "messages": messages,
        "temperature": 0.1,
    }

    async with httpx.AsyncClient(timeout=60) as client:
        r = await client.post(url, headers=headers, json=payload)

    r.raise_for_status()
    data = r.json()
    return data["choices"][0]["message"]["content"]
