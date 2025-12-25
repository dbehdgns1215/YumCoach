from typing import List, Dict, Any, Optional
import logging

try:
    from rag_service.config import config
except ImportError:
    from config import config

logger = logging.getLogger(__name__)


class RerankService:
    """숫자 기반 점수 계산 및 재정렬 서비스"""

    @staticmethod
    def calculate_nutrition_score(
        food: Dict[str, Any],
        deficits: Dict[str, float]
    ) -> float:
        """
        음식의 영양소 점수 계산

        Args:
            food: 음식 정보
            deficits: 부족 영양소

        Returns:
            점수 (0~100)
        """
        score = 0.0

        # 부족 영양소별 가중치
        nutrition = food.get("nutrition", {})

        if "protein_g" in deficits and nutrition.get("protein_g", 0) > 0:
            # 단백질 효율성: 칼로리 대비 단백질 함량
            kcal = nutrition.get("energy_kcal", 1)
            protein_ratio = nutrition.get("protein_g", 0) / max(kcal, 1)
            score += protein_ratio * 100  # 높을수록 좋음

        if "dietary_fiber_g" in deficits and nutrition.get("dietary_fiber_g", 0) > 0:
            # 식이섬유: 많을수록 좋음 (상한선 설정)
            fiber = nutrition.get("dietary_fiber_g", 0)
            score += min(fiber * 10, 30)  # 최대 30점

        if "calcium_mg" in deficits and nutrition.get("calcium_mg", 0) > 0:
            # 칼슘: 많을수록 좋음
            calcium = nutrition.get("calcium_mg", 0)
            score += min(calcium / 10, 20)  # 최대 20점

        if "iron_mg" in deficits and nutrition.get("iron_mg", 0) > 0:
            # 철분: 많을수록 좋음
            iron = nutrition.get("iron_mg", 0)
            score += min(iron * 5, 20)  # 최대 20점

        return min(score, 100)  # 최대 100점

    @staticmethod
    def check_dietary_restrictions(
        food_name: str,
        dietary_restrictions: List[str]
    ) -> bool:
        """
        음식이 dietary_restrictions와 충돌하는지 확인

        Args:
            food_name: 음식명
            dietary_restrictions: 피해야 할 음식 리스트

        Returns:
            True: 제약 조건 OK, False: 제약 위반
        """
        if not dietary_restrictions:
            return True

        food_name_lower = food_name.lower()
        for restriction in dietary_restrictions:
            restriction_lower = restriction.lower()
            # 포함 여부 체크 (정확한 필터링은 DB에서 수행)
            if restriction_lower in food_name_lower:
                return False

        return True

    @staticmethod
    def rank(
        foods: List[Dict[str, Any]],
        deficits: Dict[str, float],
        dietary_restrictions: List[str] = None,
        top_k: int = None
    ) -> List[Dict[str, Any]]:
        """
        음식 리스트를 점수순으로 정렬 및 필터링

        Args:
            foods: 음식 정보 리스트
            deficits: 부족 영양소
            dietary_restrictions: 피해야 할 음식
            top_k: 반환할 상위 개수 (기본값: config.RECOMMENDED_TOP_K)

        Returns:
            정렬된 음식 리스트 (상위 top_k개)
        """
        if top_k is None:
            top_k = config.RECOMMENDED_TOP_K

        if dietary_restrictions is None:
            dietary_restrictions = []

        # 점수 계산
        scored_foods = []
        for food in foods:
            # dietary_restrictions 필터
            if not RerankService.check_dietary_restrictions(
                food.get("food_name", ""),
                dietary_restrictions
            ):
                continue  # 제약 위반 → 제외

            # 영양소 점수 계산
            score = RerankService.calculate_nutrition_score(food, deficits)

            scored_foods.append({
                **food,
                "score": score
            })

        # 점수순 정렬
        scored_foods.sort(key=lambda x: x["score"], reverse=True)

        # 상위 top_k개 반환
        result = scored_foods[:top_k]

        logger.info(
            f"✅ Rerank 완료: {len(result)}개 음식 선정 (총 {len(scored_foods)}개 중)")

        return result


# 싱글톤 인스턴스
rerank_service = RerankService()
