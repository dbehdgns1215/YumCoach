from fastapi import APIRouter, HTTPException, UploadFile, File, Form
from typing import Optional, Tuple
import base64
import io

from PIL import Image  # pip install pillow

from app.schemas import (
    FoodImageAnalysisRequest,
    FoodAnalysisResult,
    MultiFoodAnalysisResult,
    SimpleFoodListResult,
)
from app.services.prompt_builder import (
    build_multi_food_messages,
    build_simple_food_list_messages,
)
from app.services.gms_openai_service import call_gms_openai_raw
from app.services.json_utils import extract_json_object

router = APIRouter(prefix="/analysis", tags=["Food Analysis"])


# =========================
# Helpers
# =========================

def _normalize_image_format(content_type: Optional[str]) -> str:
    """
    content_type 예: image/jpeg, image/png, image/webp ...
    """
    if not content_type:
        return "jpeg"
    if content_type.startswith("image/"):
        fmt = content_type.split("/", 1)[1].lower()
        # pillow save()에 쓰기 좋은 포맷으로 정규화
        if fmt in ("jpg", "jpeg"):
            return "jpeg"
        if fmt in ("png", "webp", "gif", "bmp", "tiff"):
            return fmt
    return "jpeg"


def _compress_image_bytes(
    image_bytes: bytes,
    *,
    max_side: int = 512,
    jpeg_quality: int = 70,
) -> Tuple[bytes, str]:
    """
    업로드된 이미지(bytes)를 열어서:
    - 가장 긴 변이 max_side를 넘으면 thumbnail 축소
    - JPEG로 저장(투명도 있으면 흰 배경 합성)
    반환: (compressed_bytes, out_mime_subtype)

    out_mime_subtype는 data URL에 들어갈 형식 (ex: "jpeg")
    """
    try:
        img = Image.open(io.BytesIO(image_bytes))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid image file: {e}")

    # EXIF 회전 보정(가능하면)
    try:
        from PIL import ImageOps
        img = ImageOps.exif_transpose(img)
    except Exception:
        pass

    # RGBA/LA/P 모드(투명도 가능) → JPEG 저장을 위해 RGB로 변환
    if img.mode in ("RGBA", "LA") or (img.mode == "P" and "transparency" in img.info):
        background = Image.new("RGB", img.size, (255, 255, 255))
        # 알파 채널이 있으면 합성
        alpha = img.convert("RGBA")
        background.paste(alpha, mask=alpha.split()[-1])
        img = background
    elif img.mode != "RGB":
        img = img.convert("RGB")

    # 축소 (비율 유지)
    img.thumbnail((max_side, max_side))

    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=jpeg_quality, optimize=True)
    return buf.getvalue(), "jpeg"


def _bytes_to_data_url(image_bytes: bytes, mime_subtype: str) -> str:
    b64 = base64.b64encode(image_bytes).decode("utf-8")
    return f"data:image/{mime_subtype};base64,{b64}"


async def _uploadfile_to_data_url(
    file: UploadFile,
    *,
    max_side: int = 512,
    jpeg_quality: int = 70,
) -> str:
    """
    UploadFile -> (리사이즈/압축) -> data URL
    """
    raw = await file.read()
    if not raw:
        raise HTTPException(status_code=400, detail="Empty file")

    # 원본 타입 참고(참고용)
    _ = _normalize_image_format(file.content_type)

    compressed_bytes, out_subtype = _compress_image_bytes(
        raw, max_side=max_side, jpeg_quality=jpeg_quality
    )
    return _bytes_to_data_url(compressed_bytes, out_subtype)


# =========================
# Routes (image_url 기반)
# =========================

@router.post("/food/image", response_model=FoodAnalysisResult)
async def analyze_food_image(request: FoodImageAnalysisRequest):
    """Single food analysis - delegates to multi and returns first item"""
    try:
        messages = build_multi_food_messages(
            image_url=request.image_url,
            extra_text=request.extra_text or "Analyze a list of foods in this image and return results in Korean"
        )

        raw = await call_gms_openai_raw(messages)
        obj = extract_json_object(raw)
        validated = MultiFoodAnalysisResult.model_validate(obj)

        if validated.items:
            first = validated.items[0]
            return FoodAnalysisResult(
                name=first.name,
                portion=first.portion,
                calories_kcal=first.calories_kcal,
                protein_g=first.protein_g,
                fat_g=first.fat_g,
                carbs_g=first.carbs_g
            )

        raise HTTPException(
            status_code=404, detail="No food detected in image")

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/food/image/multi", response_model=MultiFoodAnalysisResult)
async def analyze_food_image_multi(request: FoodImageAnalysisRequest):
    """Analyze all foods from an image URL"""
    try:
        messages = build_multi_food_messages(
            image_url=request.image_url,
            extra_text=request.extra_text,
        )

        raw = await call_gms_openai_raw(messages)
        obj = extract_json_object(raw)
        validated = MultiFoodAnalysisResult.model_validate(obj)
        return validated

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# =========================
# Routes (file upload 기반) - 2순위 적용: resize+compress -> base64
# =========================

@router.post("/food/upload", response_model=FoodAnalysisResult)
async def analyze_food_upload(
    file: UploadFile = File(...,
                            description="Image file (jpeg, png, gif, webp)"),
    extra_text: Optional[str] = Form(None),
):
    """파일 업로드로 음식 분석 - (축소/압축 후) 첫 번째 항목 반환"""
    try:
        # ✅ 2순위: 리사이즈+압축 후 base64 data URL 생성
        image_url = await _uploadfile_to_data_url(file, max_side=512, jpeg_quality=70)

        messages = build_multi_food_messages(
            image_url=image_url,
            extra_text=extra_text or "Analyze the main food in this image and estimate macros."
        )

        raw = await call_gms_openai_raw(messages)
        obj = extract_json_object(raw)
        validated = MultiFoodAnalysisResult.model_validate(obj)

        if validated.items:
            first = validated.items[0]
            return FoodAnalysisResult(
                name=first.name,
                portion=first.portion,
                calories_kcal=first.calories_kcal,
                protein_g=first.protein_g,
                fat_g=first.fat_g,
                carbs_g=first.carbs_g
            )

        raise HTTPException(
            status_code=404, detail="No food detected in image")

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/food/upload/multi", response_model=MultiFoodAnalysisResult)
async def analyze_food_upload_multi(
    file: UploadFile = File(...,
                            description="Image file (jpeg, png, gif, webp)"),
    extra_text: Optional[str] = Form(None),
):
    """파일 업로드로 음식 분석 - (축소/압축 후) 모든 음식 반환"""
    try:
        image_url = await _uploadfile_to_data_url(file, max_side=512, jpeg_quality=70)

        messages = build_multi_food_messages(
            image_url=image_url,
            extra_text=extra_text or "이미지에 있는 모든 음식의 이름을 알려주세요"
        )

        raw = await call_gms_openai_raw(messages)
        obj = extract_json_object(raw)
        validated = MultiFoodAnalysisResult.model_validate(obj)
        return validated

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/food/list", response_model=SimpleFoodListResult)
async def get_food_list(
    file: UploadFile = File(...,
                            description="Image file (jpeg, png, gif, webp)"),
    extra_text: Optional[str] = Form(None),
):
    """파일 업로드로 음식 리스트만 추출 (축소/압축 후, 영양소 제외)"""
    try:
        image_url = await _uploadfile_to_data_url(file, max_side=512, jpeg_quality=70)

        messages = build_simple_food_list_messages(
            image_url=image_url,
            extra_text=extra_text,
        )

        raw = await call_gms_openai_raw(messages)
        obj = extract_json_object(raw)
        validated = SimpleFoodListResult.model_validate(obj)
        return validated

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
