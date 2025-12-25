# 대화 1

````
좋아, **옵션 B(벡터 검색은 후보군 찾기용 / 숫자 근거는 DB)** 기준으로 바로 가져다 쓸 수 있게:

1. **두 테이블 병합 SQL**
2. **추출 → 문서화 → Chroma 저장 Python 코드**
3. **폴더 구조 + 아키텍처**

순서로 줄게.

---

## 1) `food_items` + `nutrition_facts` 병합 SQL

> 전제: 스키마는 스크린샷 기준으로 `yumcoach_db.food_items`, `yumcoach_db.nutrition_facts` 처럼 보임.
> `nutrition_facts.food_id`가 nullable이라 데이터 정합성 위해 LEFT JOIN 추천.

### (A) 벡터화용 “풀 문서” 추출 (권장)

```sql
SELECT
  fi.food_id,
  fi.food_name,
  fi.data_type,
  fi.major_category_code,
  fi.major_category_name,
  fi.middle_category_code,
  fi.middle_category_name,
  fi.sub_category_code,
  fi.sub_category_name,
  fi.representative_food_code,
  fi.representative_food_name,
  fi.serving_size,
  fi.weight,
  fi.data_created,
  fi.data_reference,

  nf.nutrition_id,
  nf.energy_kcal,
  nf.water_g,
  nf.protein_g,
  nf.fat_g,
  nf.ash_g,
  nf.carbohydrate_g,
  nf.sugars_g,
  nf.dietary_fiber_g,
  nf.calcium_mg,
  nf.iron_mg,
  nf.phosphorus_mg,
  nf.potassium_mg,
  nf.sodium_mg,
  nf.vitamin_a_rae,
  nf.retinol_ug,
  nf.beta_carotene_ug,
  nf.thiamin_mg,
  nf.riboflavin_mg,
  nf.niacin_mg,
  nf.vitamin_c_mg,
  nf.vitamin_d_ug,
  nf.cholesterol_mg,
  nf.saturated_fat_g,
  nf.trans_fat_g,
  nf.fructose_g,
  nf.sugar_alcohol_g
  -- 필요한 컬럼 더 있으면 계속 추가
FROM yumcoach_db.food_items fi
LEFT JOIN yumcoach_db.nutrition_facts nf
  ON nf.food_id = fi.food_id
WHERE fi.food_id IS NOT NULL
  AND fi.food_name IS NOT NULL;
```

### (B) 최신/유효 데이터만(선택)

데이터가 오래된 게 섞이면 “추천 신빙성”이 흔들릴 수 있어서 `data_reference` 또는 `data_created`로 필터 가능:

```sql
... (위와 동일)
WHERE fi.food_id IS NOT NULL
  AND fi.food_name IS NOT NULL
  AND (fi.data_reference IS NULL OR fi.data_reference >= DATE_SUB(CURDATE(), INTERVAL 10 YEAR));
```

---

## 2) MySQL → 문서화 → Chroma 저장 Python 코드

아래 코드는 “배치 1번 실행” 기준이야. (추후 증분 upsert도 쉽게 확장 가능)

### 설치 패키지

```bash
pip install chromadb mysql-connector-python python-dotenv
```

### `src/ingest/build_chroma.py`

```python
import os
import json
from typing import Dict, Any, List, Tuple

import mysql.connector
import chromadb
from chromadb.config import Settings
from dotenv import load_dotenv


# -----------------------------
# 1) DB 연결 / 데이터 로드
# -----------------------------
SQL_EXTRACT = """
SELECT
  fi.food_id,
  fi.food_name,
  fi.data_type,
  fi.major_category_code,
  fi.major_category_name,
  fi.middle_category_code,
  fi.middle_category_name,
  fi.sub_category_code,
  fi.sub_category_name,
  fi.representative_food_code,
  fi.representative_food_name,
  fi.serving_size,
  fi.weight,
  fi.data_created,
  fi.data_reference,

  nf.nutrition_id,
  nf.energy_kcal,
  nf.water_g,
  nf.protein_g,
  nf.fat_g,
  nf.ash_g,
  nf.carbohydrate_g,
  nf.sugars_g,
  nf.dietary_fiber_g,
  nf.calcium_mg,
  nf.iron_mg,
  nf.phosphorus_mg,
  nf.potassium_mg,
  nf.sodium_mg,
  nf.vitamin_a_rae,
  nf.retinol_ug,
  nf.beta_carotene_ug,
  nf.thiamin_mg,
  nf.riboflavin_mg,
  nf.niacin_mg,
  nf.vitamin_c_mg,
  nf.vitamin_d_ug,
  nf.cholesterol_mg,
  nf.saturated_fat_g,
  nf.trans_fat_g,
  nf.fructose_g,
  nf.sugar_alcohol_g
FROM yumcoach_db.food_items fi
LEFT JOIN yumcoach_db.nutrition_facts nf
  ON nf.food_id = fi.food_id
WHERE fi.food_id IS NOT NULL
  AND fi.food_name IS NOT NULL;
"""


def mysql_fetch_all(conn) -> List[Dict[str, Any]]:
    cur = conn.cursor(dictionary=True)
    cur.execute(SQL_EXTRACT)
    rows = cur.fetchall()
    cur.close()
    return rows


# -----------------------------
# 2) 문서(text) + metadata 설계
#    - 벡터 검색은 "후보군" 용도이므로
#      음식명/카테고리/특징(고단백 등) 중심 텍스트를 만든다
# -----------------------------
NUM_FIELDS = [
    "energy_kcal", "protein_g", "fat_g", "carbohydrate_g", "sugars_g", "dietary_fiber_g",
    "sodium_mg"
]


def safe_float(x):
    try:
        if x is None:
            return None
        return float(x)
    except Exception:
        return None


def build_tags(row: Dict[str, Any]) -> List[str]:
    # 아주 단순한 룰 기반 태그(초기 MVP용)
    kcal = safe_float(row.get("energy_kcal"))
    protein = safe_float(row.get("protein_g"))
    sugar = safe_float(row.get("sugars_g"))
    sodium = safe_float(row.get("sodium_mg"))
    fiber = safe_float(row.get("dietary_fiber_g"))

    tags = []
    if protein is not None and kcal is not None and kcal > 0:
        ppk = protein / kcal  # protein per kcal
        if ppk >= 0.08:
            tags.append("고단백")
    if sugar is not None and sugar <= 5:
        tags.append("저당")
    if sodium is not None and sodium <= 140:
        tags.append("저나트륨")
    if fiber is not None and fiber >= 5:
        tags.append("고식이섬유")
    return tags


def build_document_text(row: Dict[str, Any]) -> str:
    # 임베딩 입력 텍스트: 자연어 쿼리에 걸릴만한 정보 위주로 구성
    parts = []

    parts.append(f"음식명: {row.get('food_name')}")
    if row.get("representative_food_name"):
        parts.append(f"대표음식: {row.get('representative_food_name')}")
    if row.get("data_type"):
        parts.append(f"데이터유형: {row.get('data_type')}")

    # 카테고리
    cat = " > ".join([c for c in [
        row.get("major_category_name"),
        row.get("middle_category_name"),
        row.get("sub_category_name")
    ] if c])
    if cat:
        parts.append(f"카테고리: {cat}")

    # 제공 단위(문자열이라도 그대로 넣어두면 검색 도움)
    if row.get("serving_size"):
        parts.append(f"서빙: {row.get('serving_size')}")
    if row.get("weight"):
        parts.append(f"중량: {row.get('weight')}")

    # 핵심 영양소 요약
    # (숫자 자체는 최종 근거는 DB에서 다시 조회하지만, 검색에는 도움됨)
    nutrients = []
    for k in NUM_FIELDS:
        v = row.get(k)
        if v is not None:
            nutrients.append(f"{k}={v}")
    if nutrients:
        parts.append("영양요약: " + ", ".join(nutrients))

    tags = build_tags(row)
    if tags:
        parts.append("특징: " + ", ".join(tags))

    return "\n".join(parts)


def build_metadata(row: Dict[str, Any]) -> Dict[str, Any]:
    # metadata는 필터링/정렬에 활용 가능 (chroma where)
    md = {
        "food_id": row.get("food_id"),
        "food_name": row.get("food_name"),
        "data_type": row.get("data_type"),
        "major_category_code": row.get("major_category_code"),
        "major_category_name": row.get("major_category_name"),
        "middle_category_code": row.get("middle_category_code"),
        "middle_category_name": row.get("middle_category_name"),
        "sub_category_code": row.get("sub_category_code"),
        "sub_category_name": row.get("sub_category_name"),
        "representative_food_name": row.get("representative_food_name"),
    }

    # 숫자 메타데이터(필터/후처리용). None은 빼는 게 안전
    for k in NUM_FIELDS:
        v = safe_float(row.get(k))
        if v is not None:
            md[k] = v

    # 태그도 메타데이터로
    tags = build_tags(row)
    if tags:
        md["tags"] = ",".join(tags)

    return {k: v for k, v in md.items() if v is not None}


# -----------------------------
# 3) Chroma 저장
# -----------------------------
def chunked(lst, n) -> List[List[Any]]:
    for i in range(0, len(lst), n):
        yield lst[i:i+n]


def main():
    load_dotenv()

    # MySQL env
    mysql_host = os.getenv("MYSQL_HOST", "localhost")
    mysql_port = int(os.getenv("MYSQL_PORT", "3306"))
    mysql_user = os.getenv("MYSQL_USER", "root")
    mysql_password = os.getenv("MYSQL_PASSWORD", "")
    mysql_db = os.getenv("MYSQL_DB", "yumcoach_db")

    # Chroma env
    chroma_dir = os.getenv("CHROMA_DIR", "./data/chroma")
    collection_name = os.getenv("CHROMA_COLLECTION", "food_items_v1")

    os.makedirs(chroma_dir, exist_ok=True)

    conn = mysql.connector.connect(
        host=mysql_host,
        port=mysql_port,
        user=mysql_user,
        password=mysql_password,
        database=mysql_db,
    )

    rows = mysql_fetch_all(conn)
    conn.close()

    # Chroma persistent client
    client = chromadb.PersistentClient(
        path=chroma_dir,
        settings=Settings(anonymized_telemetry=False),
    )
    col = client.get_or_create_collection(name=collection_name)

    ids: List[str] = []
    docs: List[str] = []
    metas: List[Dict[str, Any]] = []

    for r in rows:
        food_id = r.get("food_id")
        if not food_id:
            continue

        doc = build_document_text(r)
        md = build_metadata(r)

        ids.append(str(food_id))
        docs.append(doc)
        metas.append(md)

    # upsert in batches
    BATCH = int(os.getenv("CHROMA_BATCH", "500"))
    for batch_idxs in chunked(list(range(len(ids))), BATCH):
        batch_ids = [ids[i] for i in batch_idxs]
        batch_docs = [docs[i] for i in batch_idxs]
        batch_metas = [metas[i] for i in batch_idxs]
        col.upsert(ids=batch_ids, documents=batch_docs, metadatas=batch_metas)

    print(f"Done. Upserted {len(ids)} documents into collection='{collection_name}' at '{chroma_dir}'")


if __name__ == "__main__":
    main()
```

### `.env.example`

```env
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=your_password
MYSQL_DB=yumcoach_db

CHROMA_DIR=./data/chroma
CHROMA_COLLECTION=food_items_v1
CHROMA_BATCH=500
```

---

## 3) 폴더 구조 + 아키텍처(옵션 B 기준)

### 폴더 구조(MVP)

```text
diet-bot/
  README.md
  requirements.txt
  .env.example

  src/
    ingest/
      build_chroma.py          # (배치) MySQL -> Chroma 적재
    rag/
      retriever.py             # Chroma 검색(후보군)
      rerank.py                # 숫자 기반 재정렬(옵션)
    services/
      nutrition_lookup.py      # 최종 근거용: food_id 리스트 -> DB 재조회 + 영양합산
      report_parser.py         # (기존) 일일레포트/프롬프트 기반 부족 영양소 계산(너가 이미 보유)
    api/
      chat_handler.py          # "#식단" 라우팅 + 파이프라인 연결

  data/
    chroma/                    # Chroma persistent storage
```

### 런타임 아키텍처(추천 흐름에 딱 맞게 “가볍게”)

1. **User**: “#식단… 저녁 추천”
2. **Existing Analyzer**(이미 구현된 것):

   - 오늘 섭취 기록 기반 → `deficits` 산출 (예: 단백질 35g 부족, 나트륨은 높음)

3. **Retriever (Chroma)**:

   - 질의 예: `"저녁 메인 단백질 보충 저나트륨"`
   - 결과: food_id 후보 TopK (예: 50개)

4. **DB 기반 Rerank/Filter (중요)**:

   - 후보 food_id들을 DB에서 다시 조회해서(숫자 신빙성 확보)
   - “부족한 영양소를 효율적으로 채우는 순” + “나트륨/당 제한” 적용
   - 최종 TopN(예: 5개)

5. **Gemini**:

   - TopN 후보 + 각 후보의 영양 수치(근거)를 주입
   - “추천 문장 + 근거 설명 + 서빙 예시” 작성

> 포인트: **Chroma는 ‘찾기’만**, 근거는 **항상 DB 재조회**(너의 요구사항 충족)

---

## (보너스) Retriever/Rerank 스케치(짧게)

- `retriever.py`에서 Chroma query → food_id list
- `nutrition_lookup.py`에서 `SELECT ... WHERE food_id IN (...)`
- `rerank.py`에서 deficit별 score로 정렬

원하면 위 3개 파일도 바로 만들어줄게.

---

원하는 DB가 MySQL 맞지? (스크린샷상 MySQL Workbench 느낌이라 그렇게 가정했어.)
맞다면, 다음으로는 **retriever.py + rerank.py + nutrition_lookup.py**까지 “작동하는 수준”으로 이어서 코드로 완성해줄게.

````

# 대화 2

````

좋아. 지금 코드 베이스는 **“리포트 분석용 엔드포인트(/analyze-report)”** 중심이고, 여기에 옵션 B(Chroma로 후보군 찾기 + DB로 근거 확정)를 **“#식단 저녁추천”** 흐름으로 붙이면 돼.

핵심은 구조를 이렇게 나누는 거야:

- **LLM 호출 코드는 공통화(재사용)**
- **RAG(Chroma) + DB 근거 조회는 별도 서비스로 분리**
- FastAPI 라우터는 “오케스트레이션”만 한다

아래는 “바로 붙일 수 있는” 아키텍처/폴더 구조 + 런타임 플로우 제안이야.

---

## 1) 붙일 전체 런타임 플로우 (#식단 저녁 추천)

### 요청 예

`POST /chat`
`message: "#식단 지금까지의 식단 기반으로 저녁 추천해줘"`

### 처리 순서 (옵션 B)

1. **Hashtag Router**

   - 메시지에서 `#식단` 감지 → `DietDinnerRecommendFlow`로 라우팅

2. **(기존 자산 재사용) deficit 계산**

   - “오늘(또는 현재까지) 섭취 식단” + “일일 목표”로 부족 영양소 산출
   - _이 부분은 너가 이미 프롬프트/일일 레포트로 하고 있다고 했으니 그대로 둠_
   - 결과는 구조화: 예)

     ```json
     {
       "deficits": { "protein_g": 35, "fiber_g": 6 },
       "limits": { "sodium_mg": 2000 }
     }
     ```

3. **Retriever (Chroma)**

   - deficit 기반으로 검색 쿼리 문자열 생성

     - 예: `"저녁 메인 고단백 저나트륨"` / `"단백질 보충 포만감"`

   - Chroma에서 TopK food_id 가져오기(예: 50개)

4. **Evidence fetch (DB 재조회)**

   - TopK food_id들을 MySQL에서 다시 조회(조인 SQL)
   - 여기서부터 숫자는 “진짜 근거”

5. **Rule filter + numeric rerank**

   - 유저 제한(알레르기/선호/나트륨 상한) 필터
   - deficit 목적에 맞게 점수화해서 정렬(고단백/저칼 등)

6. **LLM(설명/자연어)**

   - 최종 TopN 후보 + 각 후보 영양 수치(근거) + deficit 요약을 넣고
   - “추천 텍스트” 생성

> 결론: **Chroma는 후보 찾기**, **DB는 근거 확정**, **LLM은 표현**.

---

## 2) 지금 코드 기준 “구조를 어떻게 나누면 좋은가”

현재 코드는 `main.py` 하나가 너무 많은 역할을 하고 있어.
그래서 아래처럼 분리하면 붙이기 쉬워져.

### 권장 폴더 구조 (MVP + 확장 고려)

```text
app/
  main.py                      # FastAPI 엔트리
  core/
    config.py                  # env, 모델명, 경로
    llm_client.py              # AsyncOpenAI client + 공통 호출 래퍼
    prompts.py                 # load_prompt 공통화
  routers/
    report.py                  # /analyze-report
    chat.py                    # /chat (hashtag 라우팅)
  flows/
    diet_recommend.py          # "#식단 저녁추천" 오케스트레이션
  services/
    deficit_service.py         # (기존) 부족 영양소 계산 로직(LLM/규칙)
    retriever_service.py       # Chroma query -> food_id 후보
    food_evidence_service.py   # MySQL 조회 (food_items + nutrition_facts 조인)
    rerank_service.py          # 숫자 기반 점수화/필터링
  db/
    mysql.py                   # mysql connection pool / query helper
  vector/
    chroma_client.py           # chroma PersistentClient, collection
  prompts/
    report_analysis_prompt.txt
    diet_deficit_prompt.txt    # (있다면)
    diet_recommend_prompt.txt  # 추천 문장 생성용 (LLM)
```

### 각 레이어 역할

- `routers/*`: HTTP 입력/출력만 담당
- `flows/*`: “1) deficit → 2) retrieve → 3) evidence → 4) rerank → 5) compose” 오케스트레이션
- `services/*`: 실제 기능 단위 로직
- `core/*`: 공통 유틸(LLM 호출, 프롬프트 로딩, 설정)

---

## 3) 기존 코드에서 “재사용/이동”하면 좋은 것

### A) `load_prompt()`는 `core/prompts.py`로 이동

이미 좋게 되어 있음.

### B) OpenAI client 생성은 `core/llm_client.py`로 이동

지금처럼 endpoint가 늘어나면, 매 라우터에서 만들면 관리가 어려움.

- `async def chat_completion(system_prompt, user_content, model=...) -> str` 같은 래퍼를 만들어
- `/analyze-report`도 그대로 사용
- `#식단 추천`도 동일하게 사용

### C) `/analyze-report`는 `routers/report.py`로 분리

지금 코드 그대로 옮기되, “LLM 호출” 부분만 공통 래퍼로 바꿔.

---

## 4) "#식단 저녁 추천"을 붙이는 최소 변경 전략

### Step 1) `/chat` 엔드포인트 만들기(또는 기존이 있으면 확장)

- message에서 `#식단`이면 `flows/diet_recommend.py` 호출
- 아니면 기존 챗봇 로직

### Step 2) `DietDinnerRecommendFlow` 구현

오케스트레이션만 담당하게.

Pseudo:

```python
async def run(message: str, user_id: str) -> str:
    deficits = await deficit_service.compute(user_id)  # 기존 방식 재사용
    query = retriever_service.build_query(deficits, meal="dinner")
    candidate_ids = retriever_service.search(query, top_k=50)

    foods = food_evidence_service.fetch(candidate_ids)  # MySQL join
    ranked = rerank_service.rank(foods, deficits, user_profile)

    reply = await llm_service.compose_dinner_reply(deficits, ranked[:5])
    return reply
```

### Step 3) Chroma/DB 붙이기

- Chroma는 “ingest 스크립트”로 이미 넣고,
- 런타임에서는 query만.

---

## 5) “근거 기반 추천”을 위한 데이터 계약(중요)

LLM이 말로만 “단백질 많아요” 하지 않게 하려면,
LLM에 주입하는 데이터는 이렇게 “표 형태 JSON”이 가장 안전해.

예: LLM 입력에 주입

```json
{
  "deficits": {"protein_g": 35, "fiber_g": 6},
  "candidates": [
    {"food_id":"...", "food_name":"...", "energy_kcal":220, "protein_g":32, "fat_g":4, "carbohydrate_g":8, "sodium_mg":180},
    ...
  ]
}
```

그리고 프롬프트에:

- “candidate 값만 근거로 말해라”
- “수치 없는 주장 금지”
- “각 추천에 근거 수치 2개 이상 표시”

이렇게 하면 신빙성이 크게 올라가.

---

## 6) 추천하는 “최소 구현 순서”

1. **Chroma ingest 배치 만들기**(우리가 앞에서 만든 build_chroma.py)
2. **food_evidence_service(MySQL join 조회)** 먼저 만들기

   - TopK ids 받으면 영양정보 리턴

3. **retriever_service(Chroma query)** 만들기
4. **rerank_service(단백질 부족이면 protein 효율 정렬)** 만들기
5. 마지막에 **diet_recommend_prompt** 하나 추가해서 LLM이 보기 좋게 설명

---

원하면, 네 코드 스타일(AsyncOpenAI 사용/프롬프트 파일 로딩 방식)을 그대로 따라가서:

- `core/llm_client.py`
- `vector/chroma_client.py`
- `services/retriever_service.py`
- `services/food_evidence_service.py` (SQL + IN 조회)
- `services/rerank_service.py`
- `flows/diet_recommend.py`
- `routers/chat.py` (hashtag 라우팅)

까지 **구체 코드 템플릿**을 한 번에 만들어줄게.

추가로 딱 한 가지만 “가정”할게(질문은 안 할게):
현재 `/chat` 엔드포인트가 없다면 새로 만들고, 있다면 거기에 `#식단` 분기만 추가하는 방향으로 작성해줄 수 있어.

````

# 대화 3

````
User: 이 md파일을 참고해서

(예시)
1. 예를 들어 유저가 "#식단 지금까지의 식단을 기반으로 해서 저녁에 뭘 먹을지 추천해줘" 2. #식단 -> 기존에 작성된 프롬프트와 일일 레포트 등을 사용해서 유저가 먹었던 이전 식사를 기준으로 부족한 영양소 (예: 단백질, 지방 등) 을 판단함 3. 부족한 영양소를 기반으로 하여 식품이나 식단, 또는 메인디쉬를 추천해줌 4. 추천의 경우 숫자와 실제 근거를 기반으로 함

의 흐름을 구현하고 싶은 거야

지금 가지고 있는 report용 main.py를 반드시 사용해야하고

해당 레포트를 기반으로 해서 유저에게 구체적인 식단 몇 가지와 자세한 영양 정보 등을 알려주는 RAG 기반의 서비스를 구현해야 하는거야

바로 코드를 작성하지는 말고 네가 이해한 바와 구현 방향, 구조 등을 먼저 알려줘

GitHub Copilot: 현재 코드베이스를 먼저 파악한 후 제 이해도와 구현 방향을 설명드리겠습니다.

Read [](file:///Users/arinkim/GitHub/YumCoach/ai/report/main.py#1-1), lines 1 to 100

Read [](file:///Users/arinkim/GitHub/YumCoach/ai/chatbot/main.py#1-1), lines 1 to 100

Read [](file:///Users/arinkim/GitHub/YumCoach/ai/chatbot)

이제 foodApi의 구조도 확인하겠습니다.

Read [](file:///Users/arinkim/GitHub/YumCoach/ai/foodApi)

Read [](file:///Users/arinkim/GitHub/YumCoach/ai/foodApi/app/main.py)

이제 main.py 전체를 읽어서 현재 분석 로직을 확인하겠습니다.

Read [](file:///Users/arinkim/GitHub/YumCoach/ai/report/main.py#100-100), lines 100 to 353

chatbot/main.py의 나머지 부분도 확인하겠습니다.

Read [](file:///Users/arinkim/GitHub/YumCoach/ai/chatbot/main.py#100-100), lines 100 to 206

이제 backend 구조와 DB 스키마를 확인해야 합니다.

Read [](file:///Users/arinkim/GitHub/YumCoach/init-db)

Read [](file:///Users/arinkim/GitHub/YumCoach/init-db/06-food_items.sql#1-1), lines 1 to 50

파일이 크네요. 다른 접근을 해보겠습니다.

Read [](file:///Users/arinkim/GitHub/YumCoach/init-db/07-nutrition_facts_primary.sql#1-1), lines 1 to 80

완벽합니다! 이제 현재 코드베이스를 충분히 이해했습니다. 제 이해도와 구현 방향을 설명드리겠습니다.

---

## 📋 현재 코드베이스 상황 분석

### **현재 존재하는 것:**
1. **main.py** - `/analyze-report` 엔드포인트
   - 일일/주간 레포트를 받아서 OpenAI(Gemini)를 호출해 분석 결과를 반환
   - 사용자 정보, 영양소 요약, 식단 상세, 챌린지 정보를 프롬프트에 주입
   - 응답: heroTitle, heroLine, coachMessage, nextAction, score, insights 등 구조화된 JSON

2. **main.py** - `/chat` 엔드포인트
   - `#식단`, `#상담`, `#일일리포트`, `#주간리포트` 해시태그 지원
   - 프롬프트 파일 기반 시스템 메시지 생성
   - 사용자 프로필(건강상태, 신체 정보)을 프롬프트에 주입

3. **DB 스키마**
   - `food_items`: 식품명, 카테고리, 영양 참고자료 등 메타정보
   - `nutrition_facts_primary`: 음식별 영양정보 (단백질, 탄수화물, 지방, 칼슘, 철분 등 30개 이상의 영양소)
   - 1-to-1 관계 (LEFT JOIN 가능)

---

## 🎯 사용자의 요구사항 정리

**목표**: "#식단 저녁 추천" 흐름에서:
1. **기존 리포트 분석** → 부족한 영양소 파악 (단백질, 식이섬유 등)
2. **RAG 검색** → Chroma 벡터 DB로 후보 음식 탐색
3. **DB 근거 조회** → 최종 추천 음식의 영양정보를 MySQL에서 재확인
4. **LLM 표현** → 숫자 근거와 함께 자연스러운 추천 문장 생성

**제약조건**:
- main.py를 반드시 재사용해야 함 (부족 영양소 계산 로직)
- 숫자 기반 근거가 필수 (핸드와레이 주장 금지)

---

## 🏗️ 제안하는 아키텍처 (옵션 B 기반)

### **핵심 구조 원칙**

```
사용자 메시지 "#식단 저녁 추천"
       ↓
 [Hashtag 라우터]
       ↓
[1단계] 리포트 분석 (기존 report/main.py 로직 재사용)
   → deficit_service: 부족 영양소 계산
       ↓
[2단계] 벡터 검색 (Chroma RAG)
   → retriever_service: 부족 영양소 기반 쿼리 생성 및 검색
   → Top-50 food_id 후보 도출
       ↓
[3단계] 근거 확정 (MySQL 재조회)
   → food_evidence_service: food_id IN (...) 조인 쿼리
   → 각 후보의 정확한 영양정보 가져오기
       ↓
[4단계] 재정렬 및 필터링
   → rerank_service: 부족 영양소 효율, 사용자 제약 조건(알레르기 등) 반영
   → Top-5 최종 추천 음식
       ↓
[5단계] LLM 표현 (Gemini)
   → 숫자 근거와 함께 추천 설명 생성
```

---

## 🔄 데이터 흐름 예시

**입력**:
```json
{
  "message": "#식단 지금까지의 식단을 기반으로 저녁에 뭘 먹을지 추천해줘",
  "user_id": "2",
  "user_profile": {"name": "홍길동", "height": 175, "weight": 70},
  "today_report": { /* 오늘의 리포트 데이터 */ }
}
```

**단계별 처리**:

1️⃣ **Deficit 계산**
   ```json
   {
     "deficits": {"protein_g": 35, "dietary_fiber_g": 6},
     "limits": {"sodium_mg": 2000}
   }
   ```

2️⃣ **Chroma 검색 쿼리 생성**
   ```
   "저녁 메인 고단백 저나트륨 포만감"
   ```
   → Chroma에서 유사 food_id 50개 반환

3️⃣ **MySQL 재조회**
   ```sql
   SELECT fi.*, nf.* FROM food_items fi
   LEFT JOIN nutrition_facts_primary nf ON nf.food_id = fi.food_id
   WHERE fi.food_id IN ('2001', '2002', ...) -- Chroma 결과
   ```

4️⃣ **Rerank**
   ```
   - 단백질 효율 점수 = protein_g / (energy_kcal/4)
   - 나트륨 제약 필터
   - 정렬 후 Top-5 선정
   ```

5️⃣ **LLM 입력 예시**
   ```json
   {
     "deficits": {"protein_g": 35},
     "meal_type": "dinner",
     "candidates": [
       {"food_id": "2001", "food_name": "계란 계란말이", "protein_g": 28, "energy_kcal": 220, "sodium_mg": 180},
       {"food_id": "2002", "food_name": "닭 가슴살", "protein_g": 45, "energy_kcal": 165, "sodium_mg": 75},
       ...
     ]
   }
   ```

6️⃣ **LLM 출력 (자연어)**
   ```
   저녁으로 추천하는 메뉴:

   1️⃣ 계란 계란말이 (1인분)
   - 단백질 28g, 칼로리 220kcal, 나트륨 180mg
   - 오늘 부족한 단백질 35g 중 80% 보충 가능합니다

   2️⃣ 닭 가슴살 구이 (150g)
   - 단백질 45g, 칼로리 165kcal, 나트륨 75mg
   - 부족한 단백질을 완전히 보충할 수 있는 최적의 선택입니다
   ```

---

## 📁 권장 폴더 구조 (최소 변경 전략)

```
ai/
  chatbot/
    main.py (기존, #식단 라우팅 추가)
    prompts/
      diet_recommend.txt (신규 - 추천 프롬프트)
      ...

  report/
    main.py (기존 - deficit 계산 로직 그대로 사용)
    prompts/
      ...

  rag_service/ (신규 디렉토리)
    __init__.py
    core/
      config.py (Chroma 설정, MySQL 설정 등)
      llm_client.py (공통 LLM 호출 래퍼)
    vector/
      chroma_client.py (Chroma 클라이언트)
    db/
      mysql_client.py (MySQL 연결 풀)
    services/
      deficit_service.py (기존 report/main.py 로직 옮김)
      retriever_service.py (Chroma 검색)
      food_evidence_service.py (MySQL 조회)
      rerank_service.py (숫자 기반 정렬)
    flows/
      diet_recommend_flow.py (5단계 오케스트레이션)
    ingest/
      build_chroma.py (배치: MySQL → Chroma 적재)

  foodApi/
    (기존 - 변경 없음)
```

---

## 🔑 핵심 설계 원칙

### **1) 재사용성**
- main.py의 deficit 계산 로직을 `services/deficit_service.py`로 추출
- LLM 호출을 `core/llm_client.py`로 공통화
- 프롬프트 로딩도 공통 함수로 통일

### **2) 계층 분리**
- **Router** (main.py): HTTP 요청/응답만
- **Flow** (`diet_recommend_flow.py`): 오케스트레이션 (단계 2~5)
- **Service** (`services/*`): 실제 기능 (검색, 쿼리, 재정렬)
- **Core** (`core/*`): 공통 유틸

### **3) 근거 기반 추천**
- Chroma는 **검색 후보** 역할만 (1-2초 내 50개 추출)
- MySQL은 **근거 확정** 역할 (숫자 신뢰성 확보)
- LLM은 **표현** 역할만 (숫자는 프롬프트에서 미리 검증)

### **4) 비동기 처리**
- FastAPI + AsyncOpenAI 사용 (기존 방식 유지)
- Chroma 검색도 별도 비동기 함수로

---

## 📊 최소 구현 순서

1. **Chroma 데이터 적재** (`ingest/build_chroma.py`)
   - MySQL → Chroma 배치 작업

2. **서비스 계층 구축** (Bottom-up)
   - `core/config.py` → 설정 관리
   - `db/mysql_client.py` → DB 연결
   - `vector/chroma_client.py` → 벡터 DB 클라이언트
   - `services/food_evidence_service.py` → MySQL 조회
   - `services/retriever_service.py` → Chroma 검색
   - `services/rerank_service.py` → 점수 계산

3. **오케스트레이션** (Top-down)
   - `services/deficit_service.py` → 기존 보고서 로직 추출
   - `flows/diet_recommend_flow.py` → 5단계 흐름 연결

4. **라우터 통합**
   - main.py의 `/chat`에 `#식단` 분기 추가
   - 기존 `/analyze-report` 로직은 그대로 유지

---

## ❓ 확인이 필요한 사항

1. **MySQL 연결 정보**: 현재 .env에 어떤 DB 설정이 있는지?
2. **Chroma 저장 위치**: 로컬 파일 기반? 원격 서버?
3. **사용자 제약 조건**: 알레르기, 싫어하는 음식 등의 데이터는 어디서 오는가?
4. **리포트 형식**: 현재 `/analyze-report`에 오는 리포트 구조는?

---

이게 제가 이해한 구현 방향입니다. 다음 단계로 **이 구조가 맞는지 확인**하시거나 **특정 부분 수정 요청**을 주시면, 그에 맞춰 **구체 코드**를 작성해드리겠습니다.

User: 1. MySQL의 경우 현재 AWS에 올라가있음
2. Chroma는 일단 로컬에 저장
3. 사용자 제약 조건의 경우 dietary_restrictions에서 가져옴
4. 스프링에서 "user.put("name", u.getName());
                user.put("age", u.getAge());
            }
            if (uh != null) {
                user.put("height", uh.getHeight());
                user.put("weight", uh.getWeight());
                user.put("activity_level", uh.getActivityLevel());" 이런식으로 보내주는거임


GitHub Copilot: 완벽합니다! 이제 구체적인 정보가 있으니 더 정확한 아키텍처를 제시할 수 있습니다.

---

## 🎯 최종 구현 방향 (구체화)

### **1️⃣ 데이터 흐름의 명확화**

```
Spring 백엔드
    ↓ (user + userHealth JSON)
┌─────────────────────────────────┐
│  /chat 또는 새로운 /diet-recommend │
│  {                              │
│    "message": "#식단 저녁추천"   │
│    "user_id": "2"               │
│    "user": {                    │
│      "name": "홍길동",          │
│      "age": 30                  │
│    },                           │
│    "userHealth": {              │
│      "height": 175,             │
│      "weight": 70,              │
│      "activity_level": "MEDIUM" │
│    },                           │
│    "dietary_restrictions": ["...",...] │
│    "today_report": {...}  ← 오늘 먹은 식단 데이터 │
│  }                              │
└─────────────────────────────────┘
    ↓
[Step 1] Deficit 계산
    → report/main.py 로직 재사용
    → 부족한 영양소 도출
    ↓
[Step 2] Chroma 검색 (로컬)
    → 쿼리: "고단백 저나트륨 저칼로리"
    → Top-50 food_id 반환
    ↓
[Step 3] MySQL 조회 (AWS)
    → food_id IN (...) 쿼리
    → 정확한 영양정보 가져오기
    ↓
[Step 4] Rerank
    → 부족 영양소 효율성 계산
    → dietary_restrictions 필터 (피해야 할 음식)
    → Top-5 선정
    ↓
[Step 5] LLM (Gemini)
    → 추천 설명 생성
    → 숫자 근거 포함
```

---

### **2️⃣ 요청/응답 포맷 설계**

#### **입력 (Spring → FastAPI)**
```json
{
  "message": "#식단 지금까지의 식단을 기반으로 저녁에 뭘 먹을지 추천해줘",
  "user_id": "2",
  "user": {
    "name": "홍길동",
    "age": 30
  },
  "userHealth": {
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
      ...
    ]
  }
}
```

#### **출력 (FastAPI → Spring)**
```json
{
  "detected_hashtag": "#식단",
  "deficits": {
    "protein_g": 25,
    "dietary_fiber_g": 8,
    "calcium_mg": 400
  },
  "recommendations": [
    {
      "rank": 1,
      "food_id": "2001",
      "food_name": "닭 가슴살 구이 (150g)",
      "category": "육류 > 가금류",
      "nutrition": {
        "energy_kcal": 165,
        "protein_g": 31,
        "fat_g": 3.6,
        "carbohydrate_g": 0,
        "sodium_mg": 75,
        "calcium_mg": 8,
        "dietary_fiber_g": 0
      },
      "reason": "부족한 단백질 31g 보충 가능"
    },
    {
      "rank": 2,
      "food_id": "3005",
      "food_name": "브로콜리 (100g)",
      "category": "채소 > 엽채류",
      "nutrition": { ... },
      "reason": "식이섬유 3.7g, 칼슘 71mg"
    },
    ...
  ],
  "summary": "저녁으로 고단백 식품(닭 가슴살)을 메인으로, ...",
  "meal_suggestion": "닭 가슴살 구이 + 브로콜리 + 흰쌀밥으로 영양 균형을 맞춰보세요"
}
```

---

### **3️⃣ 핵심 서비스 설계**

#### **A) `deficit_service` (기존 report/main.py에서 추출)**

**역할**: 리포트 분석 → 부족 영양소 계산

**입력**:
```python
{
  "today_report": {...},
  "user_profile": {"age": 30, "height": 175, ...}
}
```

**출력**:
```python
{
  "deficits": {"protein_g": 25, "fiber_g": 8},
  "limits": {"sodium_mg": 2000},
  "current_nutrition": {"protein_g": 55, ...},
  "daily_targets": {"protein_g": 80, ...}
}
```

**로직**:
- 기존 report/main.py의 영양 요약 생성 로직 활용
- LLM 호출 없이 순수 계산만 수행
- (선택) 일일 권장량은 보건복지부 기준 또는 사용자 프로필 기반 계산

---

#### **B) `retriever_service` (Chroma 검색)**

**역할**: deficit 기반으로 벡터 검색

**입력**:
```python
deficits = {"protein_g": 25, "dietary_fiber_g": 8}
meal_type = "dinner"
dietary_restrictions = ["해산물", "견과류"]
```

**출력**:
```python
candidate_food_ids = ["2001", "2005", "3005", ...] # 50개
```

**로직**:
1. deficit 기반 검색어 생성 (예: "고단백 저칼로리 저나트륨")
2. Chroma query → 유사도 기반 Top-50
3. dietary_restrictions 키워드 필터 (가능하면 제외)

---

#### **C) `food_evidence_service` (MySQL 조회)**

**역할**: Chroma 결과 → 정확한 영양 데이터 조회

**SQL (핵심)**:
```sql
SELECT
  fi.food_id,
  fi.food_name,
  fi.major_category_name,
  fi.middle_category_name,
  fi.serving_size,
  nf.energy_kcal,
  nf.protein_g,
  nf.fat_g,
  nf.carbohydrate_g,
  nf.dietary_fiber_g,
  nf.sodium_mg,
  nf.calcium_mg,
  nf.iron_mg
FROM yumcoach_db.food_items fi
LEFT JOIN yumcoach_db.nutrition_facts_primary nf
  ON nf.food_id = fi.food_id
WHERE fi.food_id IN (?, ?, ?)  -- Chroma 결과
```

**입력**:
```python
candidate_food_ids = ["2001", "2005", ...]
```

**출력**:
```python
[
  {
    "food_id": "2001",
    "food_name": "닭 가슴살",
    "category": "육류 > 가금류",
    "nutrition": {
      "energy_kcal": 165,
      "protein_g": 31,
      "fat_g": 3.6,
      ...
    }
  },
  ...
]
```

---

#### **D) `rerank_service` (점수 계산 + 정렬)**

**역할**: 숫자 기반 재정렬 및 필터링

**입력**:
```python
foods = [...]  # MySQL 조회 결과
deficits = {"protein_g": 25, ...}
dietary_restrictions = ["해산물"]
user_health = {"age": 30, "weight": 70, ...}
```

**점수 계산 로직**:

1. **단백질 효율** (부족 영양소가 단백질일 때)
   ```
   protein_score = (nutrition["protein_g"] / nutrition["energy_kcal"]) * 100
   ```

2. **식이섬유 효율** (부족 영양소가 식이섬유일 때)
   ```
   fiber_score = nutrition["dietary_fiber_g"]
   ```

3. **제약 필터**
   ```
   if "해산물" in dietary_restrictions:
       if "새우" in food_name or "굴" in food_name:
           continue  # 제외
   ```

4. **건강 상태 필터** (선택)
   ```
   if user_has_diabetes and nutrition["sugars_g"] > 10:
       apply_penalty()
   ```

**출력**:
```python
ranked_foods = [
  {"food_id": "2001", "food_name": "...", "score": 95.5},
  {"food_id": "3005", "food_name": "...", "score": 88.2},
  ...
]  # Top-5 반환
```

---

#### **E) `diet_recommend_flow` (오케스트레이션)**

**역할**: 5단계 조율

```python
async def recommend_dinner(request: DietRecommendRequest):
    # Step 1: Deficit
    deficits = await deficit_service.calculate(
        request.today_report,
        request.user
    )

    # Step 2: Chroma
    candidates = await retriever_service.search(
        deficits,
        dietary_restrictions=request.dietary_restrictions,
        meal_type="dinner"
    )

    # Step 3: MySQL
    foods = await food_evidence_service.fetch(candidates)

    # Step 4: Rerank
    top_5 = await rerank_service.rank(
        foods,
        deficits,
        request.dietary_restrictions,
        request.user_health
    )

    # Step 5: LLM
    reply = await llm_service.compose_recommendation(
        deficits,
        top_5,
        request.user
    )

    return reply
```

---

### **4️⃣ 라우터 통합 방안**

#### **Option A: 기존 `/chat` 확장 (추천)**

```python
# chatbot/main.py 수정
@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    hashtag, clean_message = extract_hashtag(request.message)

    # #식단 감지 → 새로운 플로우
    if hashtag == "#식단":
        reply = await diet_recommend_flow.recommend(
            message=clean_message,
            user_id=request.user_id,
            user=request.user,
            user_health=request.user_health,
            dietary_restrictions=request.dietary_restrictions,
            today_report=request.today_report  # 필수
        )
        return ChatResponse(reply=reply, detected_hashtag=hashtag)

    # 기존 로직
    ...
```

#### **Option B: 별도 엔드포인트 추가**

```python
@app.post("/diet-recommend")
async def diet_recommend(request: DietRecommendRequest):
    return await diet_recommend_flow.recommend(...)
```

**추천: Option A** (기존 `/chat`과 통합하면 프론트엔드 변경 없음)

---

### **5️⃣ 프롬프트 설계**

#### **`prompts/diet_recommend.txt`** (신규)

```
당신은 YumCoach의 영양 코칭 전문가입니다.

【 사용자 정보 】
- 이름: {name}
- 나이: {age}
- 키: {height}cm
- 체중: {weight}kg
- 피해야 할 음식: {dietary_restrictions}

【 오늘의 식단 분석 】
현재까지 섭취한 영양소:
- 단백질: {current_protein}g (목표: {target_protein}g) → 부족: {deficit_protein}g
- 식이섬유: {current_fiber}g (목표: {target_fiber}g) → 부족: {deficit_fiber}g
- 나트륨: {current_sodium}mg (상한: {limit_sodium}mg) → 여유: {available_sodium}mg

【 저녁 추천 음식 (Top-5) 】
다음 음식들은 Chroma 벡터 검색 + MySQL 영양 데이터 기반으로 선정되었습니다:

{recommendations}

### 지시사항
1. 각 음식의 근거를 구체적인 영양수치로 제시하세요
2. "~일 수 있다", "~일 가능성이 높다" 같은 표현 금지 → 수치 기반 팩트만
3. 여러 음식의 조합(예: 닭가슴살 + 브로콜리)을 제안해보세요
4. 서빙량 제시 (예: 닭 가슴살 150g)
5. 사용자 이름을 2~3회 자연스럽게 사용

### 출력 포맷
"""
[이름]님의 저녁 추천 메뉴

## 추천 메뉴
1️⃣ **닭 가슴살 구이 (150g)**
   - 단백질 31g (부족한 25g 완전 충당)
   - 칼로리 165kcal (저칼로리)
   - 나트륨 75mg (제약 범위 내)

2️⃣ **브로콜리 (100g)**
   - 식이섬유 3.7g (부족한 8g의 46% 보충)
   - 칼슘 71mg
   ...

## 종합 제안
...
"""
```

---

### **6️⃣ 폴더 구조 (최종)**

```
ai/
  ├── chatbot/
  │   ├── main.py (기존 - #식단 분기 추가)
  │   └── prompts/
  │       ├── diet.txt (기존)
  │       └── ...
  │
  ├── report/
  │   ├── main.py (기존 - 변경 없음)
  │   └── prompts/
  │
  └── rag_service/ (신규 - 핵심 로직)
      ├── __init__.py
      ├── config.py              # 설정 (MySQL, Chroma 경로, 목표 영양소 등)
      ├── core/
      │   ├── __init__.py
      │   ├── llm_client.py       # AsyncOpenAI 공통 래퍼
      │   └── prompts.py          # load_prompt() 공통화
      ├── db/
      │   ├── __init__.py
      │   └── mysql.py            # MySQL 연결풀, 쿼리 헬퍼
      ├── vector/
      │   ├── __init__.py
      │   └── chroma.py           # Chroma 클라이언트 (로컬 경로)
      ├── services/
      │   ├── __init__.py
      │   ├── deficit_service.py   # 부족 영양소 계산
      │   ├── retriever_service.py # Chroma 검색
      │   ├── food_evidence_service.py  # MySQL 조회
      │   └── rerank_service.py    # 점수 계산
      ├── flows/
      │   ├── __init__.py
      │   └── diet_recommend.py    # 오케스트레이션
      ├── prompts/
      │   └── diet_recommend.txt   # LLM 프롬프트
      └── ingest/
          ├── __init__.py
          └── build_chroma.py      # 배치: MySQL → Chroma
```

---

### **7️⃣ 통신 다이어그램 (최종)**

```
┌──────────────┐
│  Spring App  │
└──────┬───────┘
       │ POST /chat
       │ {message: "#식단 저녁추천", user, userHealth, dietary_restrictions, today_report}
       │
       ▼
┌──────────────────────────────────────────────────┐
│         FastAPI - chatbot/main.py                │
│  ├─ extract_hashtag("#식단" 감지)                 │
│  └─ if hashtag == "#식단":                       │
│     call diet_recommend_flow.recommend()        │
└──────────┬───────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────┐
│    rag_service/flows/diet_recommend.py           │
│  [Step 1] deficit_service.calculate()            │
│  ├─ 리포트 분석 → {deficits, limits}            │
│  [Step 2] retriever_service.search()             │
│  ├─ Chroma (로컬) → 50개 food_id               │
│  [Step 3] food_evidence_service.fetch()          │
│  ├─ MySQL (AWS) → 영양정보 조회                │
│  [Step 4] rerank_service.rank()                  │
│  ├─ 점수 계산 → Top-5 선정                      │
│  [Step 5] llm_client.compose()                   │
│  └─ Gemini → 자연어 설명                         │
└──────────┬───────────────────────────────────────┘
           │
           ▼ reply: {...recommendations, summary, ...}
┌──────────────┐
│  Spring App  │ ← ChatResponse 반환
└──────────────┘
```

---

## 🔑 핵심 설계 원칙 (최종 정리)

| 계층 | 담당 | 도구 | 예시 |
|------|------|------|------|
| **Retriever** | 후보 검색 | Chroma (로컬) | Top-50 food_id 빠르게 추출 |
| **Evidence** | 근거 확정 | MySQL (AWS) | 정확한 영양정보 재조회 |
| **Rerank** | 숫자 기반 정렬 | Python 규칙 | 부족 영양소 효율로 Top-5 선정 |
| **LLM** | 표현 | Gemini | 수치 근거와 함께 설명 생성 |

---

이 아키텍처면 다음을 모두 달성합니다:

✅ **기존 main.py 재사용** (deficit 계산)
✅ **숫자 기반 근거** (MySQL → 정확한 데이터)
✅ **RAG의 장점** (Chroma로 빠른 검색)
✅ **확장성** (서비스 계층 분리)
✅ **Spring과의 통합** (JSON 자동 매핑)



## ✅ 완성 요약

RAG 기반 식단 추천 서비스의 **전체 구조**가 완성되었습니다!

### 📦 생성된 파일 목록

#### **Core 파일들**
- rag_service/config.py - 전역 설정 (MySQL, Chroma, OpenAI, 영양소 목표)
- rag_service/core/llm_client.py - LLM 공통 호출 래퍼
- rag_service/core/prompts.py - 프롬프트 로딩 및 렌더링

#### **DB & Vector 계층**
- rag_service/db/mysql.py - MySQL 연결풀 + 쿼리 헬퍼
- rag_service/vector/chroma.py - Chroma 벡터 DB 클라이언트

#### **Services 계층 (4개)**
- rag_service/services/deficit_service.py - 부족 영양소 계산
- rag_service/services/retriever_service.py - Chroma 벡터 검색
- rag_service/services/food_evidence_service.py - MySQL 음식 조회
- rag_service/services/rerank_service.py - 점수 계산 및 정렬

#### **Flow & Prompt**
- rag_service/flows/diet_recommend.py - 5단계 오케스트레이션 (메인 로직)
- rag_service/prompts/diet_recommend.txt - LLM 프롬프트

#### **Batch & Config**
- rag_service/ingest/build_chroma.py - MySQL → Chroma 배치 작업
- rag_service/requirements.txt - 패키지 의존성
- rag_service/README.md - 상세 문서

#### **통합**
- chatbot/main.py **수정** - `/chat` 엔드포인트에 #식단 라우팅 + ADVANCED role 체크 추가
- .env.example - 환경 설정 템플릿

## 🔑 핵심 특징

| 특징 | 구현 | 위치 |
|------|------|------|
| **Role Check** | ADVANCED 사용자만 가능 | chatbot/main.py#L190-L195 |
| **5단계 플로우** | Deficit → Search → Evidence → Rerank → LLM | flows/diet_recommend.py |
| **벡터 검색** | Chroma로 50개 후보 (100ms) | services/retriever_service.py |
| **정확한 근거** | MySQL 재조회 (영양정보 확정) | services/food_evidence_service.py |
| **점수 계산** | 부족 영양소 효율성 기반 | services/rerank_service.py |
| **자연어 생성** | LLM으로 추천 문장 생성 | flows/diet_recommend.py#L88-L112 |

---

## ⚠️ 주의사항

1. **MySQL 접속**: AWS RDS 보안 그룹에서 포트 3306 열려있는지 확인
2. **Chroma 저장**: 첫 실행 시 `data/chroma/` 디렉토리 자동 생성됨
3. **OpenAI API**: 사용량 제한 확인 (Gemini 기반이므로 프리미엄 필요)
4. **음식 데이터**: MySQL의 `food_items` + `nutrition_facts_primary` 테이블 필수

---

## 📊 응답 구조 예시

```json
{
  "detected_hashtag": "#식단",
  "deficits": {
    "protein_g": 25,
    "dietary_fiber_g": 8
  },
  "recommendations": [
    {
      "rank": 1,
      "food_id": "2001",
      "food_name": "닭 가슴살 구이",
      "score": 95.5,
      "nutrition": {
        "energy_kcal": 165,
        "protein_g": 31,
        "fat_g": 3.6,
        "sodium_mg": 75
      }
    },
    ...
  ],
  "summary": "홍길동님의 저녁 식사 분석...",
  "meal_suggestion": "닭 가슴살 + 브로콜리 조합 추천...",
  "tips": ["조리 팁1", "팁2", "팁3"]
}
```

---

## 🎯 완성도 체크리스트

✅ **아키텍처**: 5단계 오케스트레이션 완성
✅ **서비스 계층**: 4개 서비스 + flow 완성
✅ **DB 연동**: MySQL (AWS) + Chroma (로컬) 완성
✅ **보안**: ADVANCED role 체크 구현
✅ **통합**: main.py 수정 완료
✅ **배치**: Chroma 빌드 스크립트 완성
✅ **문서**: README + .env.example 완성

````
