from typing import List, Dict, Any
import base64
import httpx
from io import BytesIO
from PIL import Image


def _resize_and_compress_image(image_bytes: bytes, max_side: int = 256, quality: int = 20) -> bytes:
    """이미지를 극도로 리사이징하고 압축 (페이로드 최소화 for 413 에러 해결)"""
    try:
        img = Image.open(BytesIO(image_bytes))

        # RGBA 이미지는 RGB로 변환
        if img.mode in ('RGBA', 'LA', 'P'):
            rgb_img = Image.new('RGB', img.size, (255, 255, 255))
            rgb_img.paste(img, mask=img.split()
                          [-1] if img.mode == 'RGBA' else None)
            img = rgb_img

        # 리사이징 (256px 최대)
        w, h = img.size
        scale = min(max_side / max(w, h), 1.0)
        if scale < 1.0:
            img = img.resize((int(w * scale), int(h * scale)), Image.LANCZOS)

        # JPEG 극도 압축 (quality=20, 거의 모든 데이터 제거)
        output = BytesIO()
        img.save(output, format='JPEG', quality=quality, optimize=True)
        return output.getvalue()
    except Exception as e:
        print(f"Warning: Image compression failed: {e}")
        return image_bytes  # 실패 시 원본 반환
        return image_bytes  # 실패 시 원본 반환


SIMPLE_FOOD_LIST_PROMPT = """Find all foods. Return JSON only: {"items":[{"name":"food in Korean","portion":null}]}"""

MULTI_FOOD_ANALYSIS_SYSTEM_PROMPT = """Find all foods in the image. Return JSON only:
{
  "items": [
    {"name": "food name in Korean"},
    {"name": "food name in Korean"}
  ]
}

Rules:
- JSON only. No markdown. No extra keys.
- Include every visible dish; if uncertain, include best guess.
"""


def build_simple_food_list_messages(image_url: str, extra_text: str | None = None) -> List[Dict[str, Any]]:
    """음식 리스트만 추출(영양소 제외)"""
    user_text = extra_text or "이미지에 있는 모든 음식의 이름과 대략적인 분량을 알려주세요."

    # HTTP/HTTPS URL인 경우 이미지 다운로드 후 base64 변환
    if image_url.startswith(("http://", "https://")):
        try:
            with httpx.Client(timeout=30) as client:
                response = client.get(image_url)
                response.raise_for_status()

                # Content-Type에서 이미지 형식 추출
                content_type = response.headers.get(
                    "content-type", "image/jpeg")
                if "image/" in content_type:
                    image_format = content_type.split("/")[-1].split(";")[0]
                else:
                    image_format = "jpeg"

                # 이미지 리사이징 및 압축 (max_side=384, quality=40으로 페이로드 최소화)
                compressed = _resize_and_compress_image(response.content)

                # base64 인코딩
                image_data = base64.b64encode(compressed).decode("utf-8")
                image_url = f"data:image/{image_format};base64,{image_data}"
        except Exception as e:
            print(f"Warning: Failed to download image from {image_url}: {e}")

    # raw base64 데이터인 경우 data URI 형식으로 변환
    elif not image_url.startswith("data:"):
        image_url = f"data:image/jpeg;base64,{image_url}"

    return [
        {"role": "developer", "content": SIMPLE_FOOD_LIST_PROMPT},
        {
            "role": "user",
            "content": [
                {"type": "text", "text": user_text},
                {"type": "image_url", "image_url": {"url": image_url}},
            ],
        },
    ]


def build_multi_food_messages(image_url: str, extra_text: str | None = None) -> List[Dict[str, Any]]:
    user_text = extra_text or "Analyze all foods in this image. Detect multiple dishes."

    # HTTP/HTTPS URL인 경우 이미지 다운로드 후 base64 변환
    if image_url.startswith(("http://", "https://")):
        try:
            with httpx.Client(timeout=30) as client:
                response = client.get(image_url)
                response.raise_for_status()

                # Content-Type에서 이미지 형식 추출
                content_type = response.headers.get(
                    "content-type", "image/jpeg")
                if "image/" in content_type:
                    image_format = content_type.split("/")[-1].split(";")[0]
                else:
                    image_format = "jpeg"

                # 이미지 리사이징 및 압축 (max_side=384, quality=40으로 페이로드 최소화)
                compressed = _resize_and_compress_image(response.content)

                # base64 인코딩
                image_data = base64.b64encode(compressed).decode("utf-8")
                image_url = f"data:image/{image_format};base64,{image_data}"
        except Exception as e:
            # 다운로드 실패 시 원본 URL 유지 (OpenAI가 접근 가능한 URL일 수도 있음)
            print(f"Warning: Failed to download image from {image_url}: {e}")

    # raw base64 데이터인 경우 data URI 형식으로 변환
    elif not image_url.startswith("data:"):
        image_url = f"data:image/jpeg;base64,{image_url}"

    return [
        {"role": "developer", "content": MULTI_FOOD_ANALYSIS_SYSTEM_PROMPT},
        {
            "role": "user",
            "content": [
                {"type": "text", "text": user_text},
                {"type": "image_url", "image_url": {"url": image_url}},
            ],
        },
    ]
