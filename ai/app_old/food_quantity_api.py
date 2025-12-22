from flask import Flask, request, jsonify
import torch
import torch.nn.functional as F
from torch.autograd import Variable
from torchvision import transforms
from PIL import Image
import numpy as np
import os
import io
import traceback
import time

app = Flask(__name__)


class FoodQuantityPredictor:
    def __init__(self, model_path, device='cpu'):
        """
        음식 양 분류 모델 초기화

        Args:
            model_path: 학습된 모델 가중치 파일 경로
            device: 'cuda' 또는 'cpu'
        """
        self.device = torch.device(
            device if torch.cuda.is_available() else 'cpu')
        print(f"🚀 Using device: {self.device}")

        # 모델 로드
        self.model, self.class_to_idx = self.load_checkpoint(model_path)
        self.model = self.model.to(self.device)
        self.model.eval()

        # 클래스 매핑
        self.idx_to_class = {v: k for k, v in self.class_to_idx.items()}
        self.class_names = ['Q1', 'Q2', 'Q3', 'Q4', 'Q5']

        print("✅ 모델 로딩 완료!")
        print(f"📊 지원 클래스: {self.class_names}")

    def load_checkpoint(self, filepath):
        """체크포인트에서 모델 로드"""
        try:
            print(f"📂 모델 로딩 중: {filepath}")

            checkpoint = torch.load(
                filepath, map_location=self.device, weights_only=False)

            # 체크포인트 구조 확인 및 처리
            if 'model_ft' in checkpoint:
                model = checkpoint['model_ft']
                model.load_state_dict(checkpoint['state_dict'], strict=False)
                class_to_idx = checkpoint.get(
                    'class_to_idx', {f'Q{i}': i-1 for i in range(1, 6)})
            else:
                # 다른 체크포인트 구조 처리
                model = checkpoint.get('model', checkpoint)
                class_to_idx = checkpoint.get(
                    'class_to_idx', {f'Q{i}': i-1 for i in range(1, 6)})

            # 추론 모드 설정
            for param in model.parameters():
                param.requires_grad = False

            return model, class_to_idx

        except Exception as e:
            print(f"❌ 모델 로딩 오류: {e}")
            if 'checkpoint' in locals():
                print(f"🔍 체크포인트 키: {list(checkpoint.keys())}")
            raise

    def process_image(self, image):
        """
        이미지 전처리 (기존 스크립트와 동일한 전처리 파이프라인)
        """
        preprocess = transforms.Compose([
            transforms.Resize(256),
            transforms.CenterCrop(224),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406],
                                 std=[0.229, 0.224, 0.225])
        ])

        # RGB 변환 (RGBA나 다른 모드 처리)
        if image.mode != 'RGB':
            image = image.convert('RGB')

        return preprocess(image)

    def predict(self, image_input, topk=5):
        """
        단일 이미지에서 음식 양 예측

        Args:
            image_input: PIL Image 객체, 파일 경로, 또는 바이트 스트림
            topk: 상위 k개 예측 결과 반환

        Returns:
            dict: 예측 결과
        """
        try:
            # 입력 타입에 따른 이미지 로드
            if isinstance(image_input, str):
                # 파일 경로
                image = Image.open(image_input)
            elif hasattr(image_input, 'read'):
                # 파일 객체 (Flask FileStorage 등)
                image = Image.open(image_input)
            elif isinstance(image_input, bytes):
                # 바이트 스트림
                image = Image.open(io.BytesIO(image_input))
            else:
                # PIL Image 객체
                image = image_input

            # 이미지 전처리
            img_tensor = self.process_image(image)
            img_tensor = img_tensor.unsqueeze(0)  # 배치 차원 추가

            # 예측 수행 (메모리 효율성을 위해 torch.no_grad() 사용)
            with torch.no_grad():
                inputs = img_tensor.to(self.device)
                logits = self.model(inputs)
                probabilities = F.softmax(logits, dim=1)

                # Top-k 결과 추출
                topk_probs, topk_indices = probabilities.cpu().topk(topk)
                topk_probs = topk_probs.squeeze().tolist()
                topk_indices = topk_indices.squeeze().tolist()

                # 단일 예측인 경우 리스트로 변환
                if not isinstance(topk_probs, list):
                    topk_probs = [topk_probs]
                    topk_indices = [topk_indices]

            # 결과 구성
            predictions = []
            for prob, idx in zip(topk_probs, topk_indices):
                class_name = self.class_names[idx] if idx < len(
                    self.class_names) else f'Q{idx+1}'
                predictions.append({
                    'class': class_name,
                    'probability': float(prob),
                    'percentage': f"{float(prob) * 100:.2f}%"
                })

            # 최상위 예측
            top_prediction = predictions[0]

            return {
                'success': True,
                'predicted_quantity': top_prediction['class'],
                'confidence': top_prediction['probability'],
                'confidence_percentage': top_prediction['percentage'],
                'all_predictions': predictions
            }

        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'traceback': traceback.format_exc()
            }


# 전역 예측기 변수
predictor = None


def initialize_model(model_path='./weights/new_opencv_ckpt_b84_e200.pth'):
    """서버 시작 시 모델 초기화"""
    global predictor

    try:
        device_type = 'cuda' if torch.cuda.is_available() else 'cpu'
        print(f"🔧 모델 초기화 중... (디바이스: {device_type})")

        predictor = FoodQuantityPredictor(model_path, device=device_type)

        print("=" * 60)
        print("🎉 음식 양 예측 API 서버 준비 완료!")
        print("=" * 60)

        return True

    except Exception as e:
        print(f"💥 모델 초기화 실패: {e}")
        traceback.print_exc()
        return False

# Flask 라우트 정의


@app.route('/', methods=['GET'])
def home():
    """API 정보 페이지"""
    return jsonify({
        'service': 'Food Quantity Prediction API',
        'version': '1.0',
        'status': 'running' if predictor else 'model not loaded',
        'endpoints': {
            '/predict': 'POST - 이미지 파일 업로드로 음식 양 예측',
            '/health': 'GET - 서버 상태 확인'
        },
        'classes': {
            'Q1': '매우 적은 양',
            'Q2': '적은 양',
            'Q3': '보통 양',
            'Q4': '많은 양',
            'Q5': '매우 많은 양'
        }
    })


@app.route('/health', methods=['GET'])
def health_check():
    """서버 상태 확인"""
    if predictor:
        return jsonify({
            'status': 'healthy',
            'model_loaded': True,
            'device': str(predictor.device),
            'timestamp': time.time()
        })
    else:
        return jsonify({
            'status': 'unhealthy',
            'model_loaded': False,
            'error': 'Model not initialized'
        }), 500


@app.route('/predict', methods=['POST'])
def predict_quantity():
    """
    이미지를 받아 음식 양 예측

    Form Data:
        - image: 이미지 파일
        - topk: (선택) 상위 k개 결과 반환 (기본값: 5)

    Returns:
        JSON: 예측 결과
    """
    global predictor

    if predictor is None:
        return jsonify({
            'success': False,
            'error': 'Model not initialized'
        }), 500

    # 이미지 파일 확인
    if 'image' not in request.files:
        return jsonify({
            'success': False,
            'error': 'No image file provided. Please upload with key "image"'
        }), 400

    image_file = request.files['image']

    if image_file.filename == '':
        return jsonify({
            'success': False,
            'error': 'Empty filename'
        }), 400

    # topk 파라미터
    topk = int(request.form.get('topk', 5))

    try:
        start_time = time.time()

        # 예측 수행
        result = predictor.predict(image_file, topk=topk)

        # 처리 시간 추가
        result['processing_time'] = f"{(time.time() - start_time):.3f}s"

        return jsonify(result)

    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'traceback': traceback.format_exc()
        }), 500


if __name__ == '__main__':
    # 모델 경로 확인
    model_path = './weights/new_opencv_ckpt_b84_e200.pth'

    if not os.path.exists(model_path):
        print(f"❌ 모델 파일을 찾을 수 없습니다: {model_path}")
        print("💡 다음을 확인해주세요:")
        print("   1. weights 폴더가 존재하는지")
        print("   2. new_opencv_ckpt_b84_e200.pth 파일이 있는지")
        exit(1)

    # 모델 초기화
    if not initialize_model(model_path):
        print("💥 서버를 시작할 수 없습니다.")
        exit(1)

    # Flask 서버 실행
    print("🌐 Flask 서버 시작 중...")
    app.run(host='0.0.0.0', port=5001, debug=False)
