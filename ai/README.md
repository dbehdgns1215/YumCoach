# 음식 정보 OCR을 위한 API

FastAPI와 YOLO 모델을 사용하여 구축된 음식 종류 및 중량 OCR을 위한 API 백엔드입니다.

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

```
# 현재 가상환경 비활성화 및 삭제
deactivate
rm -rf venv

# 새로운 가상환경 생성 및 활성화
python -m venv venv
source venv/bin/activate

# pip 업그레이드 후 패키지 설치
pip install --upgrade pip
pip install -r requirements.txt
```
