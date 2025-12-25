#!/usr/bin/env python3
"""
Chroma 벡터 DB 생성 배치 작업
MySQL의 food_items + nutrition_facts_primary를 Chroma에 저장
"""

from rag_service.config import config
from rag_service.db.mysql import get_mysql_client
from rag_service.vector.chroma import get_chroma_client
from rag_service.core.prompts import load_prompt
from dotenv import load_dotenv
import os
import sys
import json
from typing import Dict, Any, List
from pathlib import Path
import logging
import warnings

# Chroma telemetry 비활성화
os.environ["CHROMA_TELEMETRY_DISABLED"] = "true"
os.environ["CHROMADB_TELEMETRY_DISABLED"] = "true"
warnings.filterwarnings('ignore', category=DeprecationWarning)

# 부모 디렉토리를 path에 추가해서 import 가능하게 함
sys.path.insert(0, str(Path(__file__).parent.parent.parent))


load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Telemetry 에러 로그 무시
logging.getLogger('chromadb.telemetry').setLevel(logging.CRITICAL)
logging.getLogger('chromadb.telemetry.posthog').setLevel(logging.CRITICAL)


# ===== SQL 쿼리 =====
SQL_EXTRACT_FOODS = """
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
    nf.trans_fat_g
FROM yumcoach_db.food_items fi
LEFT JOIN yumcoach_db.nutrition_facts_primary nf
    ON nf.food_id = fi.food_id
WHERE fi.food_id IS NOT NULL
    AND fi.food_name IS NOT NULL;
"""


def safe_float(x):
    """안전한 float 변환"""
    try:
        if x is None:
            return None
        return float(x)
    except Exception:
        return None


def build_tags(row: Dict[str, Any]) -> List[str]:
    """영양 기반 태그 생성"""
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
    """벡터화용 문서 텍스트 생성"""
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

    # 제공 단위
    if row.get("serving_size"):
        parts.append(f"서빙: {row.get('serving_size')}")
    if row.get("weight"):
        parts.append(f"중량: {row.get('weight')}")

    # 핵심 영양소 요약
    nutrients = []
    num_fields = [
        "energy_kcal", "protein_g", "fat_g", "carbohydrate_g",
        "sugars_g", "dietary_fiber_g", "sodium_mg", "calcium_mg"
    ]
    for k in num_fields:
        v = row.get(k)
        if v is not None:
            nutrients.append(f"{k}={v}")

    if nutrients:
        parts.append("영양: " + ", ".join(nutrients))

    # 태그
    tags = build_tags(row)
    if tags:
        parts.append("특징: " + ", ".join(tags))

    return "\n".join(parts)


def build_metadata(row: Dict[str, Any]) -> Dict[str, Any]:
    """메타데이터 생성"""
    md = {
        "food_id": row.get("food_id"),
        "food_name": row.get("food_name"),
        "data_type": row.get("data_type"),
        "major_category_name": row.get("major_category_name"),
        "middle_category_name": row.get("middle_category_name"),
        "sub_category_name": row.get("sub_category_name"),
        "representative_food_name": row.get("representative_food_name"),
    }

    # 숫자 메타데이터
    num_fields = [
        "energy_kcal", "protein_g", "fat_g", "carbohydrate_g",
        "dietary_fiber_g", "sodium_mg", "calcium_mg", "iron_mg"
    ]
    for k in num_fields:
        v = safe_float(row.get(k))
        if v is not None:
            md[k] = v

    # 태그
    tags = build_tags(row)
    if tags:
        md["tags"] = ",".join(tags)

    return {k: v for k, v in md.items() if v is not None}


def chunked(lst, n) -> List[List[Any]]:
    """리스트를 n개씩 묶기"""
    for i in range(0, len(lst), n):
        yield lst[i:i+n]


async def main():
    """메인 배치 작업"""
    logger.info("=" * 60)
    logger.info("🚀 Chroma 벡터 DB 생성 배치 시작")
    logger.info("=" * 60)

    # 테스트 시 CHROMA_LIMIT 환경변수로 조회 건수를 제한할 수 있음 (예: 100)
    limit_env = os.getenv("CHROMA_LIMIT")
    limit_clause = ""
    if limit_env:
        try:
            limit_val = int(limit_env)
            if limit_val > 0:
                limit_clause = f" LIMIT {limit_val}"
                logger.info(f"⏳ 테스트 모드: 상위 {limit_val}건만 조회")
        except ValueError:
            logger.warning("CHROMA_LIMIT 값이 정수가 아닙니다. 전체 데이터 조회를 진행합니다.")

    # MySQL 조회 (동기 처리)
    logger.info("📊 MySQL에서 데이터 조회 중...")
    mysql = get_mysql_client()

    # SQL 직접 실행
    conn = mysql.get_connection()
    cursor = conn.cursor(dictionary=True)
    sql = SQL_EXTRACT_FOODS.rstrip(";\n ") + limit_clause + ";"
    cursor.execute(sql)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()

    logger.info(f"✅ {len(rows)}개 음식 조회 완료")

    if len(rows) == 0:
        logger.warning("⚠️  조회된 음식이 없습니다. MySQL 연결 및 데이터를 확인하세요.")
        return

    # 문서 생성
    logger.info("📝 문서 생성 중...")
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

    logger.info(f"✅ {len(ids)}개 문서 생성 완료")

    if len(ids) == 0:
        logger.warning("⚠️  생성된 문서가 없습니다.")
        return

    # Chroma 저장
    logger.info("💾 Chroma에 저장 중...")
    chroma = get_chroma_client()

    batch_size = config.CHROMA_BATCH_SIZE
    total_batches = (len(ids) + batch_size - 1) // batch_size

    for batch_idx, batch_idxs in enumerate(chunked(list(range(len(ids))), batch_size), 1):
        batch_ids = [ids[i] for i in batch_idxs]
        batch_docs = [docs[i] for i in batch_idxs]
        batch_metas = [metas[i] for i in batch_idxs]

        await chroma.upsert(batch_ids, batch_docs, batch_metas)
        logger.info(f"   [{batch_idx}/{total_batches}] {len(batch_ids)}개 저장")

    logger.info("=" * 60)
    logger.info(f"✅ Chroma 벡터 DB 생성 완료!")
    logger.info(f"   - 총 {len(ids)}개 음식")
    logger.info(f"   - 저장 위치: {config.CHROMA_DIR}")
    logger.info(f"   - 컬렉션: {config.CHROMA_COLLECTION}")
    logger.info("=" * 60)


if __name__ == "__main__":
    import asyncio
    asyncio.run(main())
