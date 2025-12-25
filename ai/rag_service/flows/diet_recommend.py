from typing import Dict, Any, Optional, List
import logging
import json

try:
    from rag_service.services.deficit_service import deficit_service
    from rag_service.services.retriever_service import retriever_service
    from rag_service.services.food_evidence_service import food_evidence_service
    from rag_service.services.rerank_service import rerank_service
    from rag_service.core.llm_client import get_llm_client
    from rag_service.core.prompts import load_prompt, render_prompt
    from rag_service.config import config
except ImportError:
    from services.deficit_service import deficit_service
    from services.retriever_service import retriever_service
    from services.food_evidence_service import food_evidence_service
    from services.rerank_service import rerank_service
    from core.llm_client import get_llm_client
    from core.prompts import load_prompt, render_prompt
    from config import config

logger = logging.getLogger(__name__)


class DietRecommendFlow:
    """
    식단 추천 플로우 (5단계 오케스트레이션)

    1. Deficit 계산 (부족 영양소)
    2. Chroma 검색 (후보군 추출)
    3. MySQL 조회 (근거 확정)
    4. Rerank (점수 계산 및 정렬)
    5. LLM (자연어 설명 생성)
    """

    @staticmethod
    async def recommend(
        message: str,
        user_id: str,
        user: Dict[str, Any],
        user_health: Dict[str, Any],
        dietary_restrictions: List[str],
        today_report: Dict[str, Any],
        meal_type: str = "dinner"
    ) -> Dict[str, Any]:
        """
        식단 추천 메인 플로우

        Args:
            message: 사용자 메시지 (예: "저녁에 뭘 먹을지 추천해줘")
            user_id: 사용자 ID
            user: 사용자 정보 {"name": "홍길동", "age": 30}
            user_health: 건강 정보 {"height": 175, "weight": 70, "activity_level": "MEDIUM"}
            dietary_restrictions: 제약 조건 ["해산물", "견과류"]
            today_report: 오늘 리포트 {"totalCalories": 1800, ...}
            meal_type: 식사 유형 ("breakfast", "lunch", "dinner")

        Returns:
            추천 결과 dict
        """
        try:
            logger.info(
                f"🚀 식단 추천 플로우 시작 (사용자: {user.get('name')}, 식사: {meal_type})")

            # ===== Step 1: Deficit 계산 =====
            logger.info("📊 Step 1: 부족 영양소 계산 중...")
            deficits_result = deficit_service.calculate_deficits(
                today_report,
                user.get("age", 30),
                "female" if user.get("gender") == "F" else "male"
            )
            deficits = deficits_result["deficits"]
            current_nutrition = deficits_result["current_nutrition"]
            daily_targets = deficits_result["daily_targets"]

            logger.info(f"   부족 영양소: {deficits}")

            # ===== Step 2: Chroma 검색 =====
            logger.info("🔍 Step 2: Chroma 벡터 검색 중...")
            candidate_food_ids = await retriever_service.search(
                deficits,
                meal_type=meal_type,
                dietary_restrictions=dietary_restrictions
            )
            logger.info(f"   후보 음식: {len(candidate_food_ids)}개")

            # ===== Step 3: MySQL 조회 =====
            logger.info("🗄️  Step 3: MySQL 조회 중...")
            candidate_foods = await food_evidence_service.fetch(candidate_food_ids)
            logger.info(f"   조회 완료: {len(candidate_foods)}개")

            # ===== Step 4: Rerank =====
            logger.info("⭐ Step 4: 점수 계산 및 재정렬 중...")
            recommended_foods = rerank_service.rank(
                candidate_foods,
                deficits,
                dietary_restrictions=dietary_restrictions
            )
            logger.info(f"   최종 추천: {len(recommended_foods)}개")

            # ===== Step 5: LLM 호출 =====
            logger.info("🤖 Step 5: LLM 자연어 생성 중...")
            llm_reply = await DietRecommendFlow._compose_reply(
                user=user,
                user_health=user_health,
                deficits=deficits_result,
                recommended_foods=recommended_foods,
                meal_type=meal_type
            )

            # 응답 구성
            response = {
                "detected_hashtag": config.DIET_HASHTAG,
                "deficits": deficits,
                "current_nutrition": current_nutrition,
                "daily_targets": daily_targets,
                "recommendations": DietRecommendFlow._format_recommendations(recommended_foods),
                "summary": llm_reply.get("summary", ""),
                "meal_suggestion": llm_reply.get("meal_suggestion", ""),
                "tips": llm_reply.get("tips", [])
            }

            logger.info("✅ 식단 추천 플로우 완료")
            return response

        except Exception as e:
            logger.error(f"❌ 식단 추천 플로우 실패: {e}", exc_info=True)
            raise

    @staticmethod
    async def _compose_reply(
        user: Dict[str, Any],
        user_health: Dict[str, Any],
        deficits: Dict[str, Any],
        recommended_foods: List[Dict[str, Any]],
        meal_type: str
    ) -> Dict[str, Any]:
        """
        LLM을 사용해서 자연어 설명 생성
        """
        # 프롬프트 로드
        prompt_template = load_prompt("diet_recommend.txt")

        # 영양소 정보 포맷팅
        deficits_text = "\n".join([
            f"- {k}: {v}g 부족" if "g" in k else f"- {k}: {v}mg 부족"
            for k, v in deficits.get("deficits", {}).items()
        ])

        recommendations_text = "\n\n".join([
            DietRecommendFlow._format_food_for_prompt(food, idx + 1)
            for idx, food in enumerate(recommended_foods)
        ])

        # 프롬프트 렌더링
        logger.info(f"📝 프롬프트 렌더링 시작...")
        logger.debug(f"   deficits_text: {deficits_text[:200]}...")
        logger.debug(
            f"   recommendations_text: {recommendations_text[:300]}...")

        system_prompt = render_prompt(
            prompt_template,
            name=user.get("name", "사용자"),
            age=user.get("age", "알 수 없음"),
            height=user_health.get("height", "알 수 없음"),
            weight=user_health.get("weight", "알 수 없음"),
            meal_type=meal_type,
            deficits=deficits_text,
            recommendations=recommendations_text
        )

        logger.debug(f"   렌더링된 system_prompt 길이: {len(system_prompt)}")

        user_content = (
            "위 추천 Top-5 음식들을 근거로 간단 코칭을 생성하세요. "
            "다음 3개 필드만 포함한 JSON으로만 답하세요: summary, meal_suggestion, tips. "
            "영양소 테이블이나 원문 복붙 금지, 수치 근거만 간결하게 요약하세요."
        )
        logger.debug(f"   user_content: {user_content}")

        # LLM 호출
        logger.info(f"🤖 LLM 호출 중...")
        llm = get_llm_client()
        reply = await llm.chat_completion(system_prompt, user_content)
        logger.info(f"✅ LLM 응답 수신. 길이: {len(reply)}")
        logger.info(f"   LLM raw reply: {reply}")

        # 응답 파싱
        logger.info(f"📦 응답 파싱 중...")
        try:
            parsed = json.loads(reply)
            logger.info(
                f"   파싱 성공: summary={len(parsed.get('summary', ''))}자, tips={len(parsed.get('tips', []))}개")
            return {
                "summary": parsed.get("summary", ""),
                "meal_suggestion": parsed.get("meal_suggestion", ""),
                "tips": parsed.get("tips", [])
            }
        except:
            # JSON 파싱 실패 시 전체 응답을 summary로 반환
            return {
                "summary": reply,
                "meal_suggestion": "",
                "tips": []
            }

    @staticmethod
    def _format_food_for_prompt(food: Dict[str, Any], rank: int) -> str:
        """음식 정보를 프롬프트용으로 포맷팅"""
        nutrition = food.get("nutrition", {})

        return f"""## {rank}위. {food.get('food_name', 'N/A')} (점수: {food.get('score', 0):.1f}/100)
- 카테고리: {food.get('category', 'N/A')}
- 서빙량: {food.get('serving_size', 'N/A')} / 중량: {food.get('weight', 'N/A')}
- 칼로리: {nutrition.get('energy_kcal', 0):.0f} kcal
- 단백질: {nutrition.get('protein_g', 0):.1f}g
- 탄수화물: {nutrition.get('carbohydrate_g', 0):.1f}g
- 지방: {nutrition.get('fat_g', 0):.1f}g
- 식이섬유: {nutrition.get('dietary_fiber_g', 0):.1f}g
- 나트륨: {nutrition.get('sodium_mg', 0):.0f}mg
- 칼슘: {nutrition.get('calcium_mg', 0):.0f}mg
- 철분: {nutrition.get('iron_mg', 0):.1f}mg"""

    @staticmethod
    def _format_recommendations(recommended_foods: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """추천 음식을 응답 포맷으로 변환"""
        return [
            {
                "rank": idx + 1,
                "food_id": food.get("food_id"),
                "food_name": food.get("food_name"),
                "category": food.get("category"),
                "nutrition": food.get("nutrition"),
                "score": round(food.get("score", 0), 1),
                "serving_size": food.get("serving_size"),
                "weight": food.get("weight")
            }
            for idx, food in enumerate(recommended_foods)
        ]


# 싱글톤 인스턴스
diet_recommend_flow = DietRecommendFlow()
