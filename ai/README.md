# 음식 이미지 분석 API (하이브리드)

YOLOv3 로컬 모델로 음식 종류를 1차 인식하고, 불확실한 경우에만 Gemini 2.0 Flash를 호출해 보강하는 하이브리드 API입니다. 여러 음식이 있는 이미지에서도 각 항목을 검출/식별합니다.

## 사전 준비

- Python 3.11 이상

## 프로젝트 설정

이 프로젝트는 `requirements.txt` 파일을 사용하여 필요한 파이썬 라이브러리를 관리합니다. 이 파일은 애플리케이션 실행에 필요한 모든 의존성을 명시하고 있습니다.

프로젝트를 설정하고 필요한 모든 라이브러리를 한 번에 설치하려면 아래 단계를 따르세요.

### 1. 가상 환경 생성 (권장)

프로젝트별로 독립된 환경을 구성하기 위해 가상 환경을 사용하는 것이 좋습니다. 프로젝트 폴더 내에서 터미널을 열고 다음 명령어를 실행하세요.

```bash
# 'venv'라는 이름의 가상 환경 생성
python -m venv venv
```

### 2. 가상 환경 활성화

생성한 가상 환경을 활성화합니다.

Windows:

```
.\venv\Scripts\activate
```

macOS / Linux:

```
source venv/bin/activate
```

(터미널 프롬프트 앞에 (venv)가 표시되면 가상 환경이 성공적으로 활성화된 것입니다.)

### 3. 의존성 라이브러리 설치

requirements.txt 파일을 사용하여 모든 라이브러리를 설치합니다. 다음 명령어를 실행하세요.

```
pip install -r requirements.txt
```

이 명령어는 pip(파이썬 패키지 설치 관리자)에게 requirements.txt 파일을 읽어 그 안에 명시된 모든 라이브러리를 자동으로 설치하라고 지시합니다.

### 가상환경 재실행

````
# 현재 가상환경 비활성화 및 삭제
deactivate
rm -rf venv

# 새로운 가상환경 생성 및 활성화
python -m venv venv
source venv/bin/activate

# pip 업그레이드 후 패키지 설치
pip install --upgrade pip
pip install -r requirements.txt

## 실행 방법 (Flask)

```bash
cd ai/app
# 방법 A) .env 파일 사용 (권장)
cp .env.example .env  # 없는 경우
# .env를 열어 아래 중 하나를 설정
# 1) GMS 프록시 사용(권장):
#    GMS_KEY=YOUR_GMS_KEY
#    # (선택) GMS_BASE_URL, GMS_MODEL 커스터마이즈 가능
# 2) 구글 SDK 사용:
#    GOOGLE_API_KEY=YOUR_GOOGLE_API_KEY

# 쉘에 export 하고 싶다면 (macOS/zsh/bash)
set -a; source .env; set +a

# 방법 B) 직접 export
# export GMS_KEY=YOUR_GMS_KEY
# or export GOOGLE_API_KEY=YOUR_GOOGLE_API_KEY

python main.py
```

- 서버: http://localhost:8100
- 엔드포인트:
  - `GET /health` 모델 상태 체크
  - `POST /cf` 단일 최상위 음식 + 양 추정 (기존)
  - `POST /analyze` 다중 음식 분석 (하이브리드)

### /analyze 사용 예시

```bash
curl -X POST \
	-F "image=@/path/to/meal.jpg" \
	-F "include_quantity=true" \
	http://localhost:8100/analyze | jq
```

응답 형태

```json
{
	"success": true,
	"items": [
		{"box":[x1,y1,x2,y2],"source":"local","local_conf":0.87,"code":"01011001","final_name":"쌀밥","quantity":"Q3"},
		{"box":[...],"source":"gemini","local_conf":0.41,"code":"00000000","final_name":"된장찌개"}
	]
}
```

## 로컬 테스트 스크립트

```bash
cd ai/app
python test_hybrid.py --image /path/to/meal.jpg --include-quantity
```

## 요약 (작업 내용)

로컬 YOLO 다중 검출을 확장하고, 불확실 박스만 크롭 후 Gemini 2.0 Flash로 일괄 분석해 토큰을 절약하는 하이브리드 파이프라인을 추가했습니다. 이름→코드 매핑과 `/analyze` 엔드포인트, 테스트 스크립트를 제공해 빠르게 검증할 수 있습니다.

```

```
````
