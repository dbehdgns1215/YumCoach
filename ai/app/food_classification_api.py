# food_classification_api.py
import traceback
from typing import Union, Dict
import time
import io
from PIL import Image
import numpy as np
import cv2
import torch
import sys
import os

# =============================================================================
# 🔥 중요: 다른 import보다 먼저 경로 설정
# =============================================================================


def setup_yolo_paths():
    """YOLOv3 모듈 경로를 자동으로 찾아서 sys.path에 추가"""
    current_dir = os.path.dirname(os.path.abspath(__file__))

    # 가능한 YOLOv3 경로들 (우선순위 순)
    possible_paths = [
        '/app/yolov3',  # Docker 환경
        os.path.join(current_dir, 'yolov3'),  # app/yolov3/
        os.path.join(os.path.dirname(current_dir), 'yolov3'),  # ../yolov3/
        os.path.join(current_dir, '..', 'yolov3'),  # 상위 폴더의 yolov3
    ]

    for path in possible_paths:
        if os.path.exists(path) and os.path.exists(os.path.join(path, 'models.py')):
            if path not in sys.path:
                sys.path.insert(0, path)
            print(f"✅ YOLOv3 경로 설정: {path}")
            return path

    # 모든 경로에서 찾지 못한 경우
    raise RuntimeError(f"❌ YOLOv3 폴더를 찾을 수 없습니다. 확인한 경로: {possible_paths}")


# 경로 설정 실행
yolo_path = setup_yolo_paths()

# =============================================================================
# 이제 YOLOv3 모듈들을 import할 수 있습니다
# =============================================================================

# YOLOv3 모듈 import
try:
    from models import Darknet
    from utils.datasets import letterbox
    from utils.utils import non_max_suppression, scale_coords, load_classes
    from utils import torch_utils
    print("✅ YOLOv3 모듈 import 성공!")
except ImportError as e:
    print(f"❌ YOLOv3 모듈 import 실패: {e}")
    print(f"현재 YOLOv3 경로: {yolo_path}")
    if os.path.exists(yolo_path):
        print(f"YOLOv3 폴더 내용: {os.listdir(yolo_path)}")
    raise


class FoodClassificationAPI:
    """YOLOv3 기반 음식 분류 API"""

    def __init__(
        self,
        cfg_path: str = None,
        weights_path: str = None,
        names_path: str = None,
        img_size: int = 320,
        conf_thres: float = 0.3,
        iou_thres: float = 0.5,
        device: str = ''
    ):
        # 기본 경로 설정 (발견된 yolo_path 기준)
        if cfg_path is None:
            cfg_path = os.path.join(yolo_path, 'cfg/yolov3-spp-403cls.cfg')
        if weights_path is None:
            weights_path = os.path.join(
                yolo_path, 'weights/best_403food_e200b150v2.pt')
        if names_path is None:
            names_path = os.path.join(yolo_path, 'data/403food.names')

        # 파일 존재 확인
        for path, name in [(cfg_path, 'Config'), (weights_path, 'Weights'), (names_path, 'Names')]:
            if not os.path.exists(path):
                raise FileNotFoundError(f"❌ {name} 파일을 찾을 수 없습니다: {path}")

        print(f"📂 Config: {cfg_path}")
        print(f"📂 Weights: {weights_path}")
        print(f"📂 Names: {names_path}")

        self.img_size = img_size
        self.conf_thres = conf_thres
        self.iou_thres = iou_thres

        # 디바이스 설정
        self.device = torch_utils.select_device(device)
        print(f"🚀 디바이스: {self.device}")

        # 모델 로딩
        print("📂 모델 로딩 중...")
        self.model = Darknet(cfg_path, img_size)

        checkpoint = torch.load(
            weights_path, map_location=self.device, weights_only=False)

        self.model.load_state_dict(checkpoint['model'], strict=False)
        self.model.to(self.device).eval()

        # Half precision (GPU 사용 시)
        self.half = self.device.type != 'cpu'
        if self.half:
            self.model.half()

        # 클래스 이름 로드
        self.names = load_classes(names_path)
        print(f"✅ {len(self.names)}개 음식 클래스 로딩 완료!")

        # 워밍업
        print("🔥 모델 워밍업 중...")
        dummy_img = torch.zeros((1, 3, img_size, img_size), device=self.device)
        with torch.no_grad():
            _ = self.model(dummy_img.half()
                           if self.half else dummy_img.float())
        print("✅ 준비 완료!")

    def preprocess_image(self, image_input) -> tuple:
        """이미지 전처리 - RGB/BGR 호환성 강화"""
        if isinstance(image_input, str):
            # 파일 경로 - OpenCV 사용 (BGR)
            img0 = cv2.imread(image_input)
            if img0 is None:
                raise ValueError(f"이미지를 읽을 수 없습니다: {image_input}")
        elif isinstance(image_input, Image.Image):
            # PIL Image - RGB를 BGR로 변환
            img0 = np.array(image_input.convert('RGB'))
            img0 = cv2.cvtColor(img0, cv2.COLOR_RGB2BGR)  # 🔥 핵심 수정
        elif isinstance(image_input, bytes):
            pil_img = Image.open(io.BytesIO(image_input))
            img0 = np.array(pil_img.convert('RGB'))
            img0 = cv2.cvtColor(img0, cv2.COLOR_RGB2BGR)  # 🔥 핵심 수정
        elif hasattr(image_input, 'read'):
            # Flask FileStorage
            image_input.seek(0)
            pil_img = Image.open(image_input)
            img0 = np.array(pil_img.convert('RGB'))
            img0 = cv2.cvtColor(img0, cv2.COLOR_RGB2BGR)  # 🔥 핵심 수정
        elif isinstance(image_input, np.ndarray):
            img0 = image_input
        else:
            raise ValueError(f"지원하지 않는 이미지 타입: {type(image_input)}")

        # Letterbox 리사이징 (YOLO 표준)
        img = letterbox(img0, new_shape=self.img_size)[0]

        # HWC -> CHW (BGR 상태 유지)
        img = img.transpose(2, 0, 1)
        img = np.ascontiguousarray(img)

        return img, img0

    def predict(self, image_input, return_details: bool = False) -> Dict:
        """음식 분류 예측 - 강화된 디버깅"""
        try:
            start_time = time.time()

            # 전처리
            img, img0 = self.preprocess_image(image_input)
            print(f"📐 원본 이미지 크기: {img0.shape}")
            print(f"📐 전처리 후 크기: {img.shape}")

            # 텐서 변환 및 추론
            img = torch.from_numpy(img).to(self.device)
            img = img.half() if self.half else img.float()
            img /= 255.0

            if img.ndimension() == 3:
                img = img.unsqueeze(0)

            with torch.no_grad():
                pred = self.model(img, augment=False)[0]

            if self.half:
                pred = pred.float()

            # 🔍 Raw prediction 분석
            if pred.shape[0] > 0:
                max_conf = torch.max(pred[..., 4]).item()
                print(f"🔍 모델 최대 신뢰도: {max_conf:.4f}")
                print(f"🔍 설정된 임계값: {self.conf_thres}")

                # 클래스별 예측 분석
                if pred.shape[-1] > 5:
                    class_scores = pred[..., 5:]
                    max_class_conf, max_class_id = torch.max(
                        class_scores, dim=-1)

                    # 상위 5개 클래스 출력
                    unique_classes, counts = torch.unique(
                        max_class_id, return_counts=True)
                    print(f"\n🔍 예측된 상위 클래스:")
                    for i in range(min(5, len(unique_classes))):
                        cls_id = int(unique_classes[i])
                        class_name = self.names[cls_id] if cls_id < len(
                            self.names) else "Unknown"
                        print(
                            f"  클래스 {cls_id:3d} ({class_name}): {counts[i]}개")

            # NMS 적용
            pred = non_max_suppression(
                pred, self.conf_thres, self.iou_thres,
                multi_label=False, classes=None, agnostic=False
            )

            # 결과 처리
            detections = []
            print(f"\n🔍 NMS 후 검출 개수: {len(pred)}")

            for i, det in enumerate(pred):
                if det is not None and len(det):
                    print(f"🔍 배치 {i}: {len(det)}개 객체 검출됨")

                    det[:, :4] = scale_coords(
                        img.shape[2:], det[:, :4], img0.shape).round()

                    for j, (*xyxy, conf, cls) in enumerate(det):
                        class_id = int(cls)
                        confidence = float(conf)
                        class_name = self.names[class_id] if class_id < len(
                            self.names) else f"Unknown({class_id})"

                        print(
                            f"  [{j}] ID: {class_id:3d} | 이름: {class_name:15s} | 신뢰도: {confidence:.4f}")

                        # 🔥 핵심: 00000000 제외하고 유효한 음식만 추가
                        if class_name != '00000000':
                            detections.append({
                                'class_name': class_name,
                                'confidence': confidence,
                                'confidence_percentage': f"{confidence * 100:.2f}%"
                            })

            # 결과 반환
            detections.sort(key=lambda x: x['confidence'], reverse=True)

            if len(detections) == 0:
                print(f"\n❌ 유효한 음식을 찾지 못했습니다.")
                print(f"💡 임계값을 {self.conf_thres}에서 더 낮춰보세요.")

                return {
                    'success': False,
                    'food_code': '00000000',
                    'message': '음식을 인식할 수 없습니다. 임계값을 조정하거나 다른 이미지를 시도하세요.'
                }

            top_detection = detections[0]
            print(
                f"\n✅ 최종 선택: {top_detection['class_name']} ({top_detection['confidence_percentage']})")

            return {
                'success': True,
                'food_code': top_detection['class_name'],
                'confidence': top_detection['confidence'],
                'confidence_percentage': top_detection['confidence_percentage']
            }

        except Exception as e:
            print(f"❌ 예측 중 오류: {e}")
            traceback.print_exc()
            return {
                'success': False,
                'food_code': '00000000',
                'error': str(e)
            }


# =============================================================================
# 기존 코드 호환성을 위한 함수들
# =============================================================================
_food_classifier = None


def initModel():
    """기존 코드 호환성을 위한 초기화 함수"""
    global _food_classifier

    if _food_classifier is None:
        _food_classifier = FoodClassificationAPI()

    return _food_classifier


def detect(user_img, user_seq, model=None):
    """기존 코드 호환성을 위한 detect 함수"""
    if model is None:
        model = _food_classifier

    if model is None:
        raise RuntimeError("모델이 초기화되지 않았습니다. initModel()을 먼저 호출하세요.")

    result = model.predict(user_img)
    return result['food_code']
