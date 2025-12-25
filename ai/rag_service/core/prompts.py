from pathlib import Path
from typing import Optional

try:
    from rag_service.config import config
except ImportError:
    from config import config


def load_prompt(filename: str) -> str:
    """
    프롬프트 파일 로드

    Args:
        filename: 프롬프트 파일명 (예: "diet_recommend.txt")

    Returns:
        파일 내용 문자열
    """
    prompt_path = config.PROMPTS_DIR / filename
    try:
        with open(prompt_path, 'r', encoding='utf-8') as f:
            content = f.read()
            print(f"✅ 프롬프트 파일 로드 성공: {filename} ({len(content)} chars)")
            return content
    except FileNotFoundError:
        print(f"❌ Warning: {filename} not found at {prompt_path}")
        return ""


def render_prompt(
    template: str,
    **kwargs
) -> str:
    """
    프롬프트 템플릿에 값 주입

    Args:
        template: 프롬프트 템플릿 문자열
        **kwargs: 치환할 변수들 (예: name="홍길동", age=30)

    Returns:
        렌더링된 프롬프트
    """
    result = template
    for key, value in kwargs.items():
        placeholder = "{" + key + "}"
        result = result.replace(placeholder, str(value))
    return result
