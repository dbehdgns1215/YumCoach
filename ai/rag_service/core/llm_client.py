import os
from openai import AsyncOpenAI
from typing import List, Dict, Any, Optional

# Get config
try:
    from rag_service.config import config
except ImportError:
    from config import config


class LLMClient:
    """공통 LLM 클라이언트 (OpenAI/Gemini)"""

    def __init__(self):
        self.client = AsyncOpenAI(
            base_url=config.OPENAI_BASE_URL,
            api_key=config.OPENAI_API_KEY
        )
        self.model = config.OPENAI_MODEL

    async def chat_completion(
        self,
        system_prompt: str,
        user_content: str,
        model: str = None,
        max_tokens: int = 8000,
    ) -> str:
        """
        OpenAI API 호출 (공통 래퍼)

        Args:
            system_prompt: 시스템 메시지
            user_content: 사용자 내용
            model: 사용할 모델 (기본값: config.OPENAI_MODEL)
            temperature: 창의도 (0~1)
            max_tokens: 최대 토큰

        Returns:
            LLM 응답 문자열
        """
        if model is None:
            model = self.model

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_content}
        ]

        response = await self.client.chat.completions.create(
            model=model,
            messages=messages,
            stream=False,
            # 간결한 프롬프트와 충분한 토큰으로 content 생성 보장
            extra_body={
                "max_completion_tokens": int(max_tokens)
            }
        )

        return response.choices[0].message.content


# 싱글톤 인스턴스
_llm_client = None


def get_llm_client() -> LLMClient:
    """LLMClient 싱글톤 반환"""
    global _llm_client
    if _llm_client is None:
        _llm_client = LLMClient()
    return _llm_client
