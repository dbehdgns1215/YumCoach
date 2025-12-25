from typing import List, Dict, Any, Optional
import logging

try:
    from rag_service.vector.chroma import get_chroma_client
except ImportError:
    from vector.chroma import get_chroma_client

logger = logging.getLogger(__name__)


class RetrieverService:
    """Chroma 벡터 검색 서비스"""

    @staticmethod
    def build_search_query(
        deficits: Dict[str, float],
        meal_type: str = "dinner"
    ) -> str:
        """
        부족 영양소 기반으로 검색 쿼리 생성

        Args:
            deficits: 부족 영양소 딕셔너리
            meal_type: 식사 유형 ("breakfast", "lunch", "dinner")

        Returns:
            검색 쿼리 문자열
        """
        keywords = []

        # 부족 영양소별 키워드
        if "protein_g" in deficits:
            keywords.append("고단백")
        if "dietary_fiber_g" in deficits:
            keywords.append("고식이섬유")
        if "calcium_mg" in deficits:
            keywords.append("칼슘")
        if "iron_mg" in deficits:
            keywords.append("철분")

        # 나트륨 제약
        if "sodium_mg" in deficits and deficits["sodium_mg"] < 1000:
            keywords.append("저나트륨")

        # 기본 키워드
        meal_keywords = {
            "breakfast": "아침",
            "lunch": "점심",
            "dinner": "저녁"
        }
        keywords.insert(0, meal_keywords.get(meal_type, ""))

        # 쿼리 생성
        query = " ".join(k for k in keywords if k).strip()
        if not query:
            query = f"{meal_type} 음식"

        logger.info(f"✅ 검색 쿼리 생성: '{query}'")
        return query

    @staticmethod
    async def search(
        deficits: Dict[str, float],
        meal_type: str = "dinner",
        dietary_restrictions: List[str] = None,
        top_k: int = None
    ) -> List[str]:
        """
        부족 영양소 기반으로 음식 검색

        Args:
            deficits: 부족 영양소
            meal_type: 식사 유형
            dietary_restrictions: 피해야 할 음식 (선택, Chroma 검색 후 필터링)
            top_k: 반환할 개수 (기본값: config.RETRIEVER_TOP_K)

        Returns:
            Food ID 리스트
        """
        from rag_service.config import config

        if top_k is None:
            top_k = config.RETRIEVER_TOP_K

        if dietary_restrictions is None:
            dietary_restrictions = []

        # 검색 쿼리 생성
        query = RetrieverService.build_search_query(deficits, meal_type)

        # Chroma 검색
        chroma = get_chroma_client()
        food_ids = await chroma.search(query, top_k=top_k)

        # dietary_restrictions 필터링 (선택사항)
        # 주의: Chroma에는 음식명이 없으므로 여기서는 ID만 반환
        # 실제 필터링은 rerank_service에서 수행

        return food_ids


# 싱글톤 인스턴스
retriever_service = RetrieverService()
