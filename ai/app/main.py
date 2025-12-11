# main.py
from food_quantity_api import FoodQuantityPredictor
from food_classification_api import FoodClassificationAPI, initModel, detect
import traceback
from flask import Flask, request, jsonify, render_template
import shutil
import os
import sys

# =============================================================================
# 경로 설정 (import보다 먼저)
# =============================================================================
current_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.dirname(current_dir)

# 필요한 경로들을 sys.path에 추가
paths_to_add = [
    os.path.join(project_root, 'yolov3'),
    os.path.join(project_root, 'quantity_est'),
    os.path.join(current_dir, 'yolov3'),  # app/yolov3
    os.path.join(current_dir, 'quantity_est'),  # app/quantity_est
]

for path in paths_to_add:
    if os.path.exists(path) and path not in sys.path:
        sys.path.insert(0, path)
        print(f"경로 추가: {path}")


# 모듈 import

# Flask 앱 설정
template_dir = os.path.join(current_dir, "templates")
app = Flask(__name__, template_folder=template_dir)

print("=" * 60)
print("서버 초기화 중...")
print("=" * 60)

# 모델 초기화
print("음식 종류 인식 모델 로딩 중...")
food_classifier = FoodClassificationAPI(
    conf_thres=0.05,  # 0.3 → 0.05로 대폭 낮춤
    iou_thres=0.5
)

print("음식 양 예측 모델 로딩 중...")
# 양 예측 모델 경로 찾기
quantity_model_paths = [
    "/app/quantity_est/weights/new_opencv_ckpt_b84_e200.pth",
    os.path.join(
        project_root, "quantity_est/weights/new_opencv_ckpt_b84_e200.pth"),
    os.path.join(
        current_dir, "quantity_est/weights/new_opencv_ckpt_b84_e200.pth"),
    "./weights/new_opencv_ckpt_b84_e200.pth"
]

quantity_model_path = None
for path in quantity_model_paths:
    if os.path.exists(path):
        quantity_model_path = path
        break

if quantity_model_path is None:
    raise FileNotFoundError(
        f"양 예측 모델을 찾을 수 없습니다. 확인한 경로: {quantity_model_paths}")

quantity_predictor = FoodQuantityPredictor(model_path=quantity_model_path)

# 음식 이름 딕셔너리 import
try:
    from food_dict import get_food_name, get_quantity_description
    print("음식 이름 딕셔너리 로딩 완료!")
except ImportError:
    print("food_dict.py를 찾을 수 없습니다. 기본 함수를 사용합니다.")

    def get_food_name(code):
        return '이름 정보 없음'

    def get_quantity_description(code):
        return code

print("모든 모델 준비 완료!")


@app.route('/')
def index():
    return render_template('upload.html')


@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'healthy',
        'models': {
            'classification': 'loaded' if food_classifier else 'not loaded',
            'quantity': 'loaded' if quantity_predictor else 'not loaded'
        }
    })


@app.route('/cf', methods=['GET', 'POST'])
def post():
    if request.method == 'POST':
        if 'user_img' not in request.files:
            return jsonify({
                'status': 'fail',
                'message': '파일이 전송되지 않았습니다.',
                'code': '00000000',
                'food_name': '인식 실패',
                'quantity': '0'
            }), 400

        user_img = request.files['user_img']
        user_seq = request.form.get('user_seq', 'default_user')

        if user_img.filename == '':
            return jsonify({
                'status': 'fail',
                'message': '선택된 파일이 없습니다.',
                'code': '00000000',
                'food_name': '인식 실패',
                'quantity': '0'
            }), 400

        try:
            print(f"이미지 처리 시작: {user_img.filename}")

            # 음식 종류 인식
            print("음식 종류 인식 중...")
            food_result = food_classifier.predict(
                user_img, return_details=True)
            food_code = food_result['food_code']

            food_name = get_food_name(food_code)
            print(f"음식 인식 완료: {food_code} → {food_name}")

            # 파일 포인터 리셋
            user_img.seek(0)

            # 음식 양 예측
            print("음식 양 예측 중...")
            quantity_result = quantity_predictor.predict(user_img, topk=3)

            # 결과 반환
            if quantity_result.get('success'):
                quantity_code = quantity_result['predicted_quantity']
                quantity_desc = get_quantity_description(quantity_code)
                return jsonify({
                    'status': 'success',
                    'code': food_code,
                    'food_name': food_name,
                    'quantity': quantity_result['predicted_quantity'],
                    'quantity_name': quantity_desc,
                    'quantity_confidence': quantity_result['confidence_percentage'],
                    'food_confidence': food_result.get('confidence_percentage', 'N/A')
                })
            else:
                quantity_code = 'Q3'  # 기본값
                quantity_desc = get_quantity_description(quantity_code)
                return jsonify({
                    'status': 'partial_success',
                    'code': food_code,
                    'food_name': food_name,
                    'quantity': quantity_code,
                    'quantity_name': quantity_desc,
                    'quantity_confidence': '0%',
                    'message': '음식 종류는 인식했으나 양 추정에 실패했습니다.'
                })

        except Exception as e:
            print(f"처리 중 오류: {str(e)}")
            traceback.print_exc()
            return jsonify({
                'status': 'fail',
                'message': f'처리 중 오류가 발생했습니다: {str(e)}',
                'code': '00000000',
                'food_name': '인식 실패',
                'quantity': '0'
            }), 500


if __name__ == '__main__':
    print("\n🌐 Flask 서버 시작 중...")
    print("📍 http://localhost:8100")
    app.run(debug=True, host='0.0.0.0', port=8100)
