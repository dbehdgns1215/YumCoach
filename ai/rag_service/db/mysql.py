import mysql.connector
from mysql.connector import pooling, Error
from typing import List, Dict, Any, Optional
import logging

try:
    from rag_service.config import config
except ImportError:
    from config import config

logger = logging.getLogger(__name__)


class MySQLClient:
    """MySQL 연결 풀 + 쿼리 헬퍼"""

    def __init__(self):
        """MySQL 연결 풀 초기화"""
        try:
            self.pool = pooling.MySQLConnectionPool(
                pool_name="yumcoach_pool",
                pool_size=5,
                pool_reset_session=True,
                host=config.MYSQL_HOST,
                port=config.MYSQL_PORT,
                user=config.MYSQL_USER,
                password=config.MYSQL_PASSWORD,
                database=config.MYSQL_DB,
                autocommit=True,
                charset='utf8mb4',
                ssl_disabled=True,  # EC2 MySQL에서 SSL 비활성화 (useSSL=false)
                use_pure=True  # Pure Python implementation (호환성)
            )
            logger.info(
                f"✅ MySQL 연결 풀 생성 성공 (호스트: {config.MYSQL_HOST}:{config.MYSQL_PORT})")
        except Error as e:
            logger.error(f"❌ MySQL 연결 풀 생성 실패: {e}")
            raise

    def get_connection(self):
        """연결 풀에서 연결 가져오기"""
        return self.pool.get_connection()

    async def fetch_foods_by_ids(
        self,
        food_ids: List[str]
    ) -> List[Dict[str, Any]]:
        """
        Food ID 리스트로 음식 정보 조회

        Args:
            food_ids: 음식 ID 리스트

        Returns:
            음식 정보 리스트
        """
        if not food_ids:
            return []

        # SQL IN 쿼리 생성
        placeholders = ",".join(["%s"] * len(food_ids))
        sql = f"""
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
                nf.unsaturated_fat_g,
                nf.caffeine_mg
            FROM yumcoach_db.food_items fi
            LEFT JOIN yumcoach_db.nutrition_facts_primary nf
                ON nf.food_id = fi.food_id
            WHERE fi.food_id IN ({placeholders})
        """

        try:
            conn = self.get_connection()
            cursor = conn.cursor(dictionary=True)
            cursor.execute(sql, food_ids)
            rows = cursor.fetchall()
            cursor.close()
            conn.close()

            logger.info(f"✅ {len(rows)}개 음식 조회 성공")
            return rows
        except Error as e:
            logger.error(f"❌ MySQL 쿼리 실패: {e}")
            return []

    async def search_foods_by_name(
        self,
        keyword: str,
        limit: int = 10
    ) -> List[Dict[str, Any]]:
        """
        음식명으로 검색

        Args:
            keyword: 검색 키워드
            limit: 반환 개수

        Returns:
            음식 정보 리스트
        """
        sql = """
            SELECT
                fi.food_id,
                fi.food_name,
                fi.major_category_name,
                fi.middle_category_name,
                nf.energy_kcal,
                nf.protein_g,
                nf.carbohydrate_g,
                nf.fat_g,
                nf.sodium_mg,
                nf.dietary_fiber_g
            FROM yumcoach_db.food_items fi
            LEFT JOIN yumcoach_db.nutrition_facts_primary nf
                ON nf.food_id = fi.food_id
            WHERE fi.food_name LIKE %s
            LIMIT %s
        """

        try:
            conn = self.get_connection()
            cursor = conn.cursor(dictionary=True)
            cursor.execute(sql, (f"%{keyword}%", limit))
            rows = cursor.fetchall()
            cursor.close()
            conn.close()

            logger.info(f"✅ '{keyword}' 검색 결과: {len(rows)}개")
            return rows
        except Error as e:
            logger.error(f"❌ MySQL 검색 실패: {e}")
            return []


# 싱글톤 인스턴스
_mysql_client = None


def get_mysql_client() -> MySQLClient:
    """MySQLClient 싱글톤 반환"""
    global _mysql_client
    if _mysql_client is None:
        _mysql_client = MySQLClient()
    return _mysql_client
