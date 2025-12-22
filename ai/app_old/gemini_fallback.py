import os
import io
import base64
import json
import requests
from typing import List, Dict
from PIL import Image
import numpy as np

# Load .env if present (supports multi-folder search)
try:
    from dotenv import load_dotenv, find_dotenv  # type: ignore
    load_dotenv(find_dotenv(), override=False)
except Exception:
    pass

# Prefer the newer google-genai client if available; fallback to google.generativeai
try:
    from google import genai  # type: ignore
    _CLIENT_STYLE = "genai"
except Exception:
    try:
        import google.generativeai as genai  # type: ignore
        _CLIENT_STYLE = "generativeai"
    except Exception:
        genai = None
        _CLIENT_STYLE = None


def _ensure_client():
    """Resolve which transport to use: GMS proxy REST preferred, else SDKs."""
    # Prefer GMS proxy if available
    gms_key = os.environ.get("GMS_KEY")
    if gms_key:
        return {"mode": "gms"}

    api_key = os.environ.get("GOOGLE_API_KEY")
    if not api_key:
        raise RuntimeError(
            "Missing API key. Set GMS_KEY (preferred) or GOOGLE_API_KEY in environment.")

    if _CLIENT_STYLE == "genai":
        client = genai.Client(api_key=api_key)
        return {"mode": "sdk-genai", "client": client}
    elif _CLIENT_STYLE == "generativeai":
        genai.configure(api_key=api_key)
        return {"mode": "sdk-generativeai"}
    else:
        raise RuntimeError(
            "google-genai/google-generativeai not installed. Or set GMS_KEY to use REST proxy.")


def _resize_and_encode_jpeg(img: np.ndarray, max_side: int = 768, quality: int = 80) -> bytes:
    if img.ndim == 2:
        pil = Image.fromarray(img)
    else:
        # Assume BGR or RGB; ensure RGB
        if img.shape[2] == 3:
            # If likely BGR (from OpenCV), convert to RGB by reversing channels
            img_rgb = img[:, :, ::-1]
        else:
            img_rgb = img
        pil = Image.fromarray(img_rgb)

    w, h = pil.size
    scale = min(max_side / max(w, h), 1.0)
    if scale < 1.0:
        pil = pil.resize((int(w * scale), int(h * scale)), Image.LANCZOS)

    buf = io.BytesIO()
    pil.save(buf, format="JPEG", quality=quality, optimize=True)
    return buf.getvalue()


def analyze_crops(crops: List[np.ndarray]) -> List[Dict]:
    """Batch analyze multiple food crops via Gemini and return list of {id, name}.

    Crops are np.ndarray images (BGR/RGB). Keeps token usage low with JSON-only output.
    """
    client_info = _ensure_client()

    # Build prompt
    system_prompt = (
        "여러 음식 크롭 이미지가 주어집니다. 각 이미지 id에 대해 한국어 음식명 1개만 정답으로 주세요. "
        "설명 없이 JSON 배열만 반환하세요. 형식: [{\"id\": <int>, \"name\": \"<음식명>\"}]"
    )

    # Prepare encoded crops once
    encoded = []
    for idx, crop in enumerate(crops):
        img_bytes = _resize_and_encode_jpeg(crop, max_side=512, quality=70)
        encoded.append((idx, img_bytes))

    try:
        mode = client_info.get("mode")
        if mode == "gms":
            # Use GMS proxy REST
            base_url = os.environ.get(
                "GMS_BASE_URL", "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com")
            model = os.environ.get("GMS_MODEL", "gemini-2.0-flash")
            key = os.environ["GMS_KEY"]
            url = f"{base_url}/v1beta/models/{model}:generateContent?key={key}"

            parts = [{"text": system_prompt}]
            for idx, img_bytes in encoded:
                parts.append({"text": f"image id={idx}"})
                parts.append({
                    "inlineData": {
                        "mimeType": "image/jpeg",
                        "data": base64.b64encode(img_bytes).decode("utf-8")
                    }
                })

            payload = {
                "contents": [{"parts": parts}],
                "generationConfig": {
                    "temperature": 0.2,
                    "maxOutputTokens": 128,
                    "responseMimeType": "application/json"
                }
            }
            headers = {"Content-Type": "application/json"}
            r = requests.post(url, headers=headers,
                              data=json.dumps(payload), timeout=30)
            r.raise_for_status()
            resp_json = r.json()
            # Extract text
            text = ""
            try:
                text = resp_json["candidates"][0]["content"]["parts"][0]["text"]
            except Exception:
                text = "[]"

        elif mode == "sdk-genai":
            client = client_info["client"]
            parts = [{"text": system_prompt}]
            for idx, img_bytes in encoded:
                parts.append({"text": f"image id={idx}"})
                parts.append({
                    "inline_data": {
                        "mime_type": "image/jpeg",
                        "data": base64.b64encode(img_bytes).decode("utf-8")
                    }
                })
            resp = client.models.generate_content(
                model=os.environ.get("GMS_MODEL", "gemini-2.0-flash"),
                contents=[{"role": "user", "parts": parts}],
                config={
                    "temperature": 0.2,
                    "max_output_tokens": 128,
                    "response_mime_type": "application/json",
                },
            )
            text = resp.output_text

        else:  # sdk-generativeai
            parts = [system_prompt]
            for idx, img_bytes in encoded:
                parts.append(f"image id={idx}")
                parts.append({"mime_type": "image/jpeg", "data": img_bytes})
            model = genai.GenerativeModel(
                os.environ.get("GMS_MODEL", "gemini-2.0-flash"))
            resp = model.generate_content(parts, generation_config={
                "temperature": 0.2,
                "max_output_tokens": 128,
                "response_mime_type": "application/json",
            })
            text = resp.text

        data = json.loads(text)
        out = []
        for item in data:
            out.append({"id": int(item.get("id", 0)),
                       "name": str(item.get("name", ""))})
        return out

    except Exception:
        return [{"id": i, "name": ""} for i in range(len(crops))]
