from typing import Dict, Any, Optional
import logging

try:
    from rag_service.config import config
except ImportError:
    from config import config

logger = logging.getLogger(__name__)


class DeficitService:
    """
    부족 영양소 계산 서비스
    (기존 report/main.py의 로직을 추출)
    """

    @staticmethod
    def get_daily_targets(age: int, gender: Optional[str] = "male") -> Dict[str, float]:
        """
        사용자의 일일 영양소 목표 계산

        Args:
            age: 나이
            gender: 성별 ("male" 또는 "female")

        Returns:
            영양소 목표 딕셔너리
        """
        # 기본값: 성인 남성 / 여성 기준
        if gender == "female" or gender == "여성":
            return config.NUTRITION_TARGETS["adult_female"]
        else:
            return config.NUTRITION_TARGETS["adult_male"]

    @staticmethod
    def calculate_deficits(
        report_data: Dict[str, Any],
        user_age: int,
        user_gender: Optional[str] = "male"
    ) -> Dict[str, Any]:
        """
        리포트 데이터 기반으로 부족 영양소 계산

        Args:
            report_data: 일일 리포트 데이터
                {
                    "totalCalories": 1800,
                    "proteinG": 55,
                    "carbG": 200,
                    "fatG": 50,
                    ...
                }
            user_age: 사용자 나이
            user_gender: 사용자 성별

        Returns:
            {
                "deficits": {"protein_g": 25, "dietary_fiber_g": 8, ...},
                "limits": {"sodium_mg": 2000, ...},
                "current_nutrition": {...},
                "daily_targets": {...},
                "ratio": {"protein_ratio": 0.69, ...}
            }
        """
        daily_targets = DeficitService.get_daily_targets(user_age, user_gender)

        # 현재 섭취 영양소
        current = {
            "energy_kcal": float(report_data.get("totalCalories", 0)),
            "protein_g": float(report_data.get("proteinG", 0)),
            "carbohydrate_g": float(report_data.get("carbG", 0)),
            "fat_g": float(report_data.get("fatG", 0)),
            "dietary_fiber_g": float(report_data.get("dietaryFiberG", 0)),
            "sodium_mg": float(report_data.get("sodiumMg", 0)),
            "calcium_mg": float(report_data.get("calciumMg", 0)),
            "iron_mg": float(report_data.get("ironMg", 0)),
        }

        # 부족 영양소 (음수 = 초과)
        deficits = {}
        for key, target in daily_targets.items():
            current_val = current.get(key, 0)
            deficit = target - current_val
            if deficit > 0:
                deficits[key] = round(deficit, 2)

        # 초과 제한 영양소 (상한선)
        limits = {
            "sodium_mg": max(0, float(report_data.get("sodiumLimit", 2000)) - current.get("sodium_mg", 0))
        }

        # 비율 계산
        ratio = {}
        for key in ["protein_g", "carbohydrate_g", "fat_g"]:
            if current["energy_kcal"] > 0:
                ratio[key.replace("_g", "_ratio")] = round(
                    current.get(key, 0) / current["energy_kcal"],
                    3
                )

        logger.info(f"✅ Deficit 계산 완료: {deficits}")

        return {
            "deficits": deficits,
            "limits": limits,
            "current_nutrition": current,
            "daily_targets": daily_targets,
            "ratio": ratio
        }


# 싱글톤 인스턴스
deficit_service = DeficitService()
