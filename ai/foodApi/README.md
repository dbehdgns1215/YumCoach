# 프로젝트 구조

```
food-analysis-api/
├── app/
│   ├── main.py              # FastAPI 엔트리포인트
│   ├── config.py            # 환경변수 / 설정
│   ├── schemas.py           # Pydantic 요청/응답 모델
│   ├── services/
│   │   └── openai_service.py
│   └── routers/
│       └── analysis.py
├── requirements.txt
├── .env.example
└── README.md
```

# 실행 방법

```
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate

pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --port 8111

```

📌 Swagger
👉 http://127.0.0.1:8111/docs

# 요청 예시

```
{
  "prompt": "불고기 덮밥의 칼로리와 영양소를 분석해줘"
}

```
