from typing import List, Dict, Any
import logging

try:
    from rag_service.db.mysql import get_mysql_client
except ImportError:
    from db.mysql import get_mysql_client

logger = logging.getLogger(__name__)


class FoodEvidenceService:
    """음식 영양정보 조회 서비스 (MySQL)"""

    @staticmethod
    def normalize_nutrition(row: Dict[str, Any]) -> Dict[str, Any]:
        """
        MySQL 조회 결과를 정규화

        Args:
            row: MySQL 쿼리 결과 행

        Returns:
            정규화된 음식 정보
        """
        # 카테고리 조합
        categories = [
            row.get("major_category_name"),
            row.get("middle_category_name"),
            row.get("sub_category_name")
        ]
        category = " > ".join([c for c in categories if c])

        # 영양정보 추출
        nutrition = {
            "energy_kcal": float(row.get("energy_kcal", 0)) if row.get("energy_kcal") else 0,
            "water_g": float(row.get("water_g", 0)) if row.get("water_g") else 0,
            "protein_g": float(row.get("protein_g", 0)) if row.get("protein_g") else 0,
            "fat_g": float(row.get("fat_g", 0)) if row.get("fat_g") else 0,
            "ash_g": float(row.get("ash_g", 0)) if row.get("ash_g") else 0,
            "carbohydrate_g": float(row.get("carbohydrate_g", 0)) if row.get("carbohydrate_g") else 0,
            "sugars_g": float(row.get("sugars_g", 0)) if row.get("sugars_g") else 0,
            "dietary_fiber_g": float(row.get("dietary_fiber_g", 0)) if row.get("dietary_fiber_g") else 0,
            "calcium_mg": float(row.get("calcium_mg", 0)) if row.get("calcium_mg") else 0,
            "iron_mg": float(row.get("iron_mg", 0)) if row.get("iron_mg") else 0,
            "phosphorus_mg": float(row.get("phosphorus_mg", 0)) if row.get("phosphorus_mg") else 0,
            "potassium_mg": float(row.get("potassium_mg", 0)) if row.get("potassium_mg") else 0,
            "sodium_mg": float(row.get("sodium_mg", 0)) if row.get("sodium_mg") else 0,
            "vitamin_a_rae": float(row.get("vitamin_a_rae", 0)) if row.get("vitamin_a_rae") else 0,
            "retinol_ug": float(row.get("retinol_ug", 0)) if row.get("retinol_ug") else 0,
            "beta_carotene_ug": float(row.get("beta_carotene_ug", 0)) if row.get("beta_carotene_ug") else 0,
            "thiamin_mg": float(row.get("thiamin_mg", 0)) if row.get("thiamin_mg") else 0,
            "riboflavin_mg": float(row.get("riboflavin_mg", 0)) if row.get("riboflavin_mg") else 0,
            "niacin_mg": float(row.get("niacin_mg", 0)) if row.get("niacin_mg") else 0,
            "vitamin_c_mg": float(row.get("vitamin_c_mg", 0)) if row.get("vitamin_c_mg") else 0,
            "vitamin_d_ug": float(row.get("vitamin_d_ug", 0)) if row.get("vitamin_d_ug") else 0,
            "cholesterol_mg": float(row.get("cholesterol_mg", 0)) if row.get("cholesterol_mg") else 0,
            "saturated_fat_g": float(row.get("saturated_fat_g", 0)) if row.get("saturated_fat_g") else 0,
            "trans_fat_g": float(row.get("trans_fat_g", 0)) if row.get("trans_fat_g") else 0,
            "unsaturated_fat_g": float(row.get("unsaturated_fat_g", 0)) if row.get("unsaturated_fat_g") else 0,
            "caffeine_mg": float(row.get("caffeine_mg", 0)) if row.get("caffeine_mg") else 0,
        }

        return {
            "food_id": row.get("food_id"),
            "food_name": row.get("food_name"),
            "data_type": row.get("data_type"),
            "category": category,
            "serving_size": row.get("serving_size"),
            "weight": row.get("weight"),
            "nutrition": nutrition
        }

    @staticmethod
    async def fetch(
        food_ids: List[str]
    ) -> List[Dict[str, Any]]:
        """
        Food ID 리스트로 음식 정보 조회

        Args:
            food_ids: 음식 ID 리스트

        Returns:
            정규화된 음식 정보 리스트
        """
        if not food_ids:
            return []

        # MySQL 조회
        mysql = get_mysql_client()
        rows = await mysql.fetch_foods_by_ids(food_ids)

        # 정규화
        foods = [FoodEvidenceService.normalize_nutrition(row) for row in rows]

        logger.info(f"✅ {len(foods)}개 음식 정보 조회 완료")

        return foods


# 싱글톤 인스턴스
food_evidence_service = FoodEvidenceService()
