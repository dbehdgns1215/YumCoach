# Chatbot (ai/chatbot)

## 빠른 실행

먼저 폴더로 이동:

```bash
cd C:\SSAFY\GWANTONG\YumCoach\ai\chatbot
```

1. 가상환경 생성 (없으면)

```bash
python -m venv chatbot_env
```

2. 활성화

- Windows (PowerShell)

```powershell
.\chatbot_env\Scripts\Activate.ps1
```

- mac / Linux (bash)

```bash
source chatbot_env/bin/activate
```

3. 패키지 설치

```bash
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
```

4. chatbot 디렉토리 안에 `.env`파일 생성 후 키 추가

```
OPENAI_API_KEY=sk-xxxx...
```

5. 실행 방법 (둘 중 하나)

- 단순 스크립트 실행:

```bash
python main_gemini.py
```

- FastAPI(Uvicorn) 서버로 실행

```bash
uvicorn main_gemini:app --host 0.0.0.0 --port 8001
```
