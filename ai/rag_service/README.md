# YumCoach RAG-based Diet Recommendation Service

RAG(Retrieval-Augmented Generation) 기반의 식단 추천 서비스입니다.

## 🎯 기능

- **#식단 추천**: 사용자의 오늘 식단 분석 → 부족한 영양소 파악 → 최적의 음식 추천
- **벡터 검색**: Chroma를 사용한 빠른 후보 음식 추출
- **정확한 근거**: MySQL에서 재조회한 영양정보로 신뢰성 확보
- **점수 기반 정렬**: 부족 영양소 효율성으로 Top-5 추천
- **자연어 설명**: LLM(Gemini)으로 자연스러운 추천 메시지 생성

## 📋 아키텍처

```
[사용자 요청] #식단 저녁추천
    ↓
[Step 1] Deficit 계산 → 부족 영양소 파악
    ↓
[Step 2] Chroma 검색 → 50개 후보 추출
    ↓
[Step 3] MySQL 조회 → 정확한 영양정보 확인
    ↓
[Step 4] Rerank → 점수 계산 및 Top-5 선정
    ↓
[Step 5] LLM 호출 → 자연어 설명 생성
    ↓
[응답] 추천 음식 + 이유 + 팁
```

## 🚀 설치 및 실행

### 1. 환경 설정

```bash
cd /Users/arinkim/GitHub/YumCoach/ai

# .env 파일 생성
cp .env.example .env

# .env 파일 수정 (AWS MySQL 접속정보, OpenAI API 키 등)
```

### 2. 패키지 설치

```bash
pip install -r rag_service/requirements.txt
```

### 3. Chroma 벡터 DB 생성 (배치 작업)

```bash
cd /Users/arinkim/GitHub/YumCoach/ai

# 배치 실행 (권장: 경로 문제 없음)
python -m rag_service.ingest.build_chroma

# 또는 직접 실행
python rag_service/ingest/build_chroma.py
```

**출력 예시:**

```
============================================================
🚀 Chroma 벡터 DB 생성 배치 시작
============================================================
📊 MySQL에서 데이터 조회 중...
✅ 50000개 음식 조회 완료
📝 문서 생성 중...
✅ 50000개 문서 생성 완료
💾 Chroma에 저장 중...
   [1/100] 500개 저장
   ...
✅ Chroma 벡터 DB 생성 완료!
   - 총 50000개 음식
   - 저장 위치: ./data/chroma
   - 컬렉션: food_items_v1
============================================================
```

### 4. 챗봇 서버 실행

```bash
# 기본 포트 8001에서 실행
uvicorn ai.chatbot.main:app --host 0.0.0.0 --port 8001 --reload

# 또는 다른 포트
uvicorn ai.chatbot.main:app --host 0.0.0.0 --port 8077
```

## 📡 API 사용 예시

### POST `/chat` - 식단 추천 (#식단)

```bash
curl -X POST "http://localhost:8001/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "#식단 지금까지의 식단을 기반으로 저녁에 뭘 먹을지 추천해줘",
    "user_id": "2",
    "user_role": "ADVANCED",
    "user": {
      "name": "홍길동",
      "age": 30
    },
    "user_health": {
      "height": 175,
      "weight": 70,
      "activity_level": "MEDIUM"
    },
    "dietary_restrictions": ["해산물", "견과류"],
    "today_report": {
      "type": "DAILY",
      "totalCalories": 1800,
      "proteinG": 55,
      "carbG": 200,
      "fatG": 50,
      "mealCount": 3,
      "meals": [
        {
          "mealName": "아침: 계란밥",
          "calories": 550,
          "proteinG": 15,
          "carbG": 65,
          "fatG": 18
        },
        {
          "mealName": "점심: 돈까스",
          "calories": 750,
          "proteinG": 25,
          "carbG": 80,
          "fatG": 32
        },
        {
          "mealName": "간식: 바나나",
          "calories": 100,
          "proteinG": 1,
          "carbG": 27,
          "fatG": 0
        }
      ]
    }
  }'
```

### 응답 예시

```json
{
  "reply": "{\"recommendations\": [{\"rank\": 1, \"food_id\": \"2001\", \"food_name\": \"닭 가슴살 구이\", ...}, ...], \"summary\": \"홍길동님의 저녁 식사 분석...\", ...}",
  "detected_hashtag": "#식단"
}
```

## 🔑 핵심 구조

### 폴더 구조

```
rag_service/
├── config.py                          # 전역 설정
├── core/
│   ├── llm_client.py                 # LLM 공통 호출
│   └── prompts.py                    # 프롬프트 관리
├── db/
│   └── mysql.py                      # MySQL 연결 및 쿼리
├── vector/
│   └── chroma.py                     # Chroma 클라이언트
├── services/
│   ├── deficit_service.py            # 부족 영양소 계산
│   ├── retriever_service.py          # Chroma 검색
│   ├── food_evidence_service.py      # MySQL 음식 조회
│   └── rerank_service.py             # 점수 계산 및 정렬
├── flows/
│   └── diet_recommend.py             # 5단계 오케스트레이션
├── prompts/
│   └── diet_recommend.txt            # LLM 프롬프트
└── ingest/
    └── build_chroma.py               # 배치: MySQL → Chroma
```

### 핵심 서비스

| 서비스                  | 역할             | 입력                 | 출력             |
| ----------------------- | ---------------- | -------------------- | ---------------- |
| **DeficitService**      | 부족 영양소 계산 | 일일 리포트          | 부족 영양소 dict |
| **RetrieverService**    | 벡터 검색        | 부족 영양소          | 50개 food_id     |
| **FoodEvidenceService** | MySQL 조회       | food_id 리스트       | 영양정보 dict    |
| **RerankService**       | 점수 계산        | 음식들 + 부족 영양소 | Top-5 추천       |
| **DietRecommendFlow**   | 오케스트레이션   | 전체 입력            | 최종 추천 결과   |

## ⚙️ 설정 (config.py)

```python
# MySQL (AWS)
MYSQL_HOST = "your-aws-rds-endpoint.rds.amazonaws.com"
MYSQL_PORT = 3306
MYSQL_USER = "admin"
MYSQL_PASSWORD = "***"
MYSQL_DB = "yumcoach_db"

# Chroma (로컬)
CHROMA_DIR = "./data/chroma"
CHROMA_COLLECTION = "food_items_v1"
CHROMA_BATCH_SIZE = 500

# OpenAI
OPENAI_API_KEY = "sk-..."
OPENAI_BASE_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"
OPENAI_MODEL = "gpt-5-nano"

# 추천 설정
RECOMMENDED_TOP_K = 5  # 최종 추천 음식 개수
RETRIEVER_TOP_K = 50   # Chroma 후보 개수
```

## 🔒 보안 (Role Check)

`#식단` 기능은 **ADVANCED 역할의 사용자만** 사용 가능합니다.

```python
if user_role != "ADVANCED":
    raise HTTPException(
        status_code=403,
        detail="이 기능은 ADVANCED 유저만 사용 가능합니다."
    )
```

## 🧪 테스트

### 단위 테스트

```bash
# deficit_service 테스트
python -c "
from rag_service.services.deficit_service import deficit_service
result = deficit_service.calculate_deficits(
    {'totalCalories': 1800, 'proteinG': 55, 'carbG': 200},
    age=30
)
print(result)
"
```

### 통합 테스트

```bash
# Chroma 검색 테스트
python -c "
import asyncio
from rag_service.vector.chroma import get_chroma_client

async def test():
    chroma = get_chroma_client()
    results = await chroma.search('고단백 저나트륨', top_k=5)
    print(results)

asyncio.run(test())
"
```

## 📊 성능

- **Chroma 검색**: ~100ms (50개 후보 추출)
- **MySQL 조회**: ~200ms (50개 음식 영양정보 조회)
- **Rerank**: ~50ms (점수 계산)
- **LLM 호출**: ~2~3초 (자연어 생성)

**총 응답 시간**: ~3~4초

## 🐛 트러블슈팅

### "MySQL 연결 실패"

```
ERROR: 2003 (HY000): Can't connect to MySQL server on 'aws-endpoint'
```

**해결**:

1. AWS RDS 보안 그룹 확인 (포트 3306 열려있는지)
2. `.env`의 MYSQL\_\* 설정 재확인
3. AWS RDS 엔드포인트 복사 (읽기 엔드포인트 아님)

### "Chroma 폴더 생성 권한 오류"

```
PermissionError: [Errno 13] Permission denied: './data/chroma'
```

**해결**:

```bash
mkdir -p ./data/chroma
chmod 755 ./data
```

### "OpenAI API 인증 실패"

```
AuthenticationError: Invalid API key provided
```

**해결**:

1. `.env`의 OPENAI_API_KEY 확인
2. API 키 재발급 확인

## 📝 로깅

```bash
# 로그 레벨 설정
LOG_LEVEL=INFO  # DEBUG, INFO, WARNING, ERROR

# 실시간 로그 확인
tail -f logs/rag_service.log
```

## 🔄 업데이트

### Chroma 데이터 재생성

```bash
# 기존 데이터 삭제
rm -rf ./data/chroma

# 새로운 데이터 생성
python rag_service/ingest/build_chroma.py
```

### 프롬프트 수정

`rag_service/prompts/diet_recommend.txt`를 수정 후 서버 재시작

## 📞 지원

문제가 발생하면:

1. 로그 파일 확인
2. `.env` 설정 재확인
3. MySQL 연결 테스트
4. Chroma 벡터 DB 상태 확인

---

**최종 업데이트**: 2025-12-26
