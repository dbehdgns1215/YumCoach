import io
import json
from typing import List, Dict, Tuple, Optional
from PIL import Image
import numpy as np

from food_classification_api import FoodClassificationAPI
from food_quantity_api import FoodQuantityPredictor
from gemini_fallback import analyze_crops

try:
    from food_dict import food_names
except Exception:
    food_names = {}


def _area(box):
    x1, y1, x2, y2 = box
    return max(0, x2 - x1) * max(0, y2 - y1)


def _iou(a, b) -> float:
    ax1, ay1, ax2, ay2 = a
    bx1, by1, bx2, by2 = b
    ix1, iy1 = max(ax1, bx1), max(ay1, by1)
    ix2, iy2 = min(ax2, bx2), min(ay2, by2)
    iw, ih = max(0, ix2 - ix1), max(0, iy2 - iy1)
    inter = iw * ih
    if inter == 0:
        return 0.0
    ua = _area(a) + _area(b) - inter
    return inter / ua if ua > 0 else 0.0


def _is_contained(inner, outer, ratio: float = 0.9) -> bool:
    ix1, iy1, ix2, iy2 = inner
    ox1, oy1, ox2, oy2 = outer
    inside = (ix1 >= ox1 and iy1 >= oy1 and ix2 <= ox2 and iy2 <= oy2)
    if not inside:
        return False
    if _area(inner) == 0:
        return True
    return (_area(inner) / _area(outer)) <= ratio


def _normalize_name(name: str) -> str:
    import re
    s = (name or "").strip().lower()
    s = re.sub(r"[\s\-_/()+.,'\"·]", "", s)
    return s


# Build inverse map (name -> code)
_name_to_code = None


def _build_inverse_map():
    global _name_to_code
    if _name_to_code is not None:
        return _name_to_code
    inv = {}
    for code, name in food_names.items():
        inv[_normalize_name(name)] = code
    _name_to_code = inv
    return inv


class HybridFoodAnalyzer:
    def __init__(self, classifier: FoodClassificationAPI,
                 quantity_predictor: Optional[FoodQuantityPredictor] = None,
                 high_conf: float = 0.55,
                 low_conf: float = 0.25,
                 max_gemini_crops: int = 5,
                 iou_dedupe: float = 0.7,
                 min_area_ratio: float = 0.01,
                 ensure_min: int = 0,
                 low_conf_floor: float = 0.15,
                 exclude_null_code: bool = True,
                 exclude_generic_classes: List[str] = None,
                 max_single_box_ratio: float = 0.5):
        self.classifier = classifier
        self.quantity_predictor = quantity_predictor
        self.high_conf = high_conf
        self.low_conf = low_conf
        self.max_gemini_crops = max_gemini_crops
        self.iou_dedupe = iou_dedupe
        self.min_area_ratio = min_area_ratio
        self.ensure_min = ensure_min
        self.low_conf_floor = low_conf_floor
        self.exclude_null_code = exclude_null_code
        self.exclude_generic_classes = exclude_generic_classes or [
            '급식', '식판', '트레이']
        self.max_single_box_ratio = max_single_box_ratio

        # Keep analysis image-driven; no prompt steering

    def _filter_and_dedupe(self, dets: List[Dict], image_size: Tuple[int, int]) -> List[Dict]:
        W, H = image_size
        image_area = W * H
        min_area = self.min_area_ratio * image_area

        # conf + area + null-code + generic class filter
        filtered = []
        for d in dets:
            box_area = _area(d['box'])
            if box_area < min_area:
                continue
            if d.get('class_name') == '00000000' and self.exclude_null_code:
                continue
            # 통합 클래스 제외 (급식, 식판 등)
            if d.get('class_name') in self.exclude_generic_classes:
                continue
            # 너무 큰 박스(전체 이미지의 50% 이상) 제외
            if box_area / image_area > self.max_single_box_ratio:
                continue
            if d['confidence'] >= self.low_conf:
                filtered.append(d)
        dets = filtered
        # sort by conf desc
        dets.sort(key=lambda d: d['confidence'], reverse=True)
        # dedupe by class with IoU and containment
        kept = []
        for d in dets:
            discard = False
            for k in kept:
                if d['class_name'] == k['class_name']:
                    if _iou(d['box'], k['box']) > self.iou_dedupe or _is_contained(d['box'], k['box']):
                        discard = True
                        break
            if not discard:
                kept.append(d)
        # If too few, relax thresholds to include more candidates down to low_conf_floor
        if self.ensure_min and len(kept) < self.ensure_min:
            min_area2 = max(min_area * 0.5, 1.0)
            for d in dets:
                if d in kept:
                    continue
                if d['confidence'] < self.low_conf_floor:
                    continue
                if _area(d['box']) < min_area2:
                    continue
                far = True
                for k in kept:
                    if _iou(d['box'], k['box']) > (self.iou_dedupe - 0.2):
                        far = False
                        break
                if far:
                    kept.append(d)
                if len(kept) >= self.ensure_min:
                    break
        return kept

    def _crop_from_pil(self, pil: Image.Image, box: List[int]) -> np.ndarray:
        x1, y1, x2, y2 = box
        crop = pil.crop((x1, y1, x2, y2))
        return np.array(crop)

    def _pad_box(self, box: List[int], W: int, H: int, pad_ratio: float = 0.12) -> List[int]:
        x1, y1, x2, y2 = box
        w = max(1, x2 - x1)
        h = max(1, y2 - y1)
        px = int(w * pad_ratio)
        py = int(h * pad_ratio)
        nx1 = max(0, x1 - px)
        ny1 = max(0, y1 - py)
        nx2 = min(W, x2 + px)
        ny2 = min(H, y2 + py)
        return [nx1, ny1, nx2, ny2]

    def _map_name_to_code(self, name: str) -> str:
        inv = _build_inverse_map()
        key = _normalize_name(name)
        if key in inv:
            return inv[key]
        # simple partial match
        for k, code in inv.items():
            if key and k.startswith(key):
                return code
        for k, code in inv.items():
            if key and key in k:
                return code
        return "00000000"

    def _code_to_korean(self, code: str) -> str:
        return food_names.get(code, "이름 정보 없음")

    def _analyze_grid(self, pil: Image.Image) -> List[Dict]:
        """이미지를 격자로 나눠서 각 영역을 Gemini로 분석"""
        W, H = pil.size
        # 2x3 또는 3x2 격자로 분할 (식판 형태에 따라)
        grid_cols = 3 if W >= H else 2
        grid_rows = 2 if W >= H else 3

        cell_w = W // grid_cols
        cell_h = H // grid_rows

        crops = []
        boxes = []
        for row in range(grid_rows):
            for col in range(grid_cols):
                x1 = col * cell_w
                y1 = row * cell_h
                x2 = x1 + cell_w
                y2 = y1 + cell_h
                # 마지막 열/행은 나머지 전부 포함
                if col == grid_cols - 1:
                    x2 = W
                if row == grid_rows - 1:
                    y2 = H

                crop = pil.crop((x1, y1, x2, y2))
                crops.append(np.array(crop))
                boxes.append([x1, y1, x2, y2])

        # Gemini로 분석 (중립 프롬프트)
        results = analyze_crops(crops)

        items = []
        for idx, (box, result) in enumerate(zip(boxes, results)):
            name = result.get('name', '')
            if not name or name.lower() in ['none', 'empty', '없음', '빈칸']:
                continue
            code = self._map_name_to_code(name)
            items.append({
                'box': box,
                'source': 'gemini-grid',
                'local_conf': 0.0,
                'code': code,
                'final_name': name or self._code_to_korean(code),
            })

        return items

    def analyze(self, image_bytes: bytes, include_quantity: bool = False) -> Dict:
        pil = Image.open(io.BytesIO(image_bytes)).convert('RGB')
        W, H = pil.size

        # Local multi-detect
        cls_res = self.classifier.predict_multi(image_bytes)
        if not cls_res.get('success'):
            return {"success": False, "items": [], "message": cls_res.get('error', 'classification failed')}

        dets = self._filter_and_dedupe(cls_res['detections'], (W, H))

        # 만약 검출이 너무 적으면 이미지를 격자로 나눠서 Gemini로 분석
        if len(dets) < 2:
            print(f"⚠️ 검출된 항목이 {len(dets)}개뿐. 이미지를 격자로 분할해 Gemini 분석 시도...")
            grid_items = self._analyze_grid(pil)
            if len(grid_items) > len(dets):
                return {"success": True, "items": grid_items}

        # Split into confident and uncertain
        confident, uncertain = [], []
        for d in dets:
            if d['confidence'] >= self.high_conf:
                confident.append(d)
            else:
                uncertain.append(d)

        items = []
        # Confident: accept local (class_name is likely code)
        for d in confident:
            code = d['class_name']
            name = self._code_to_korean(code)
            item = {
                'box': d['box'],
                'source': 'local',
                'local_conf': d['confidence'],
                'code': code,
                'final_name': name,
            }
            items.append(item)

        # Uncertain: Gemini fallback (limit N)
        uncertain = sorted(uncertain, key=lambda x: x['confidence'])[
            : self.max_gemini_crops]
        if len(uncertain) > 0:
            # Dual-crop consensus: original + padded for added context
            aug_crops = []
            for d in uncertain:
                aug_crops.append(self._crop_from_pil(pil, d['box']))
                padded_box = self._pad_box(d['box'], W, H)
                aug_crops.append(self._crop_from_pil(pil, padded_box))
            results = analyze_crops(aug_crops)
            # Map back by pair index
            for idx, d in enumerate(uncertain):
                name_a = results[2 * idx].get('name',
                                              '') if 2 * idx < len(results) else ''
                name_b = results[2 * idx +
                                 1].get('name', '') if 2 * idx + 1 < len(results) else ''
                # Prefer padded prediction if available; else original; else empty
                name = name_b or name_a or ''
                code = self._map_name_to_code(name)
                item = {
                    'box': d['box'],
                    'source': 'gemini',
                    'local_conf': d['confidence'],
                    'code': code,
                    'final_name': name or self._code_to_korean(code),
                }
                items.append(item)

        # Optional: quantity per item using same crops
        if include_quantity and self.quantity_predictor is not None:
            for i, it in enumerate(items):
                try:
                    crop = self._crop_from_pil(pil, it['box'])
                    # predictor.predict accepts file-like or PIL; pass PIL
                    qres = self.quantity_predictor.predict(
                        Image.fromarray(crop), topk=1)
                    if qres.get('success'):
                        it['quantity'] = qres['predicted_quantity']
                        it['quantity_confidence'] = qres['confidence']
                except Exception:
                    continue

        return {"success": True, "items": items}
