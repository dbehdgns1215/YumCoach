import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()


class Config:
    """RAG Service Configuration"""

    # ===== MySQL (AWS) =====
    MYSQL_HOST = os.getenv("MYSQL_HOST", "127.0.0.1")
    MYSQL_PORT = int(os.getenv("MYSQL_PORT", "3306"))
    MYSQL_USER = os.getenv("MYSQL_USER", "root")
    MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "")
    MYSQL_DB = os.getenv("MYSQL_DB", "yumcoach_db")

    # ===== Chroma (Local Vector DB) =====
    CHROMA_DIR = os.getenv("CHROMA_DIR", "./data/chroma")
    CHROMA_COLLECTION = os.getenv("CHROMA_COLLECTION", "food_items_v1")
    CHROMA_BATCH_SIZE = int(os.getenv("CHROMA_BATCH_SIZE", "500"))

    # ===== OpenAI / LLM =====
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    OPENAI_BASE_URL = os.getenv(
        "OPENAI_BASE_URL", "https://gms.ssafy.io/gmsapi/api.openai.com/v1")
    OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-5-nano")

    # ===== Prompts =====
    PROMPTS_DIR = Path(__file__).parent / "prompts"

    # ===== Nutrition Targets (Daily Recommendations) =====
    # Reference: 보건복지부 영양소 섭취기준
    NUTRITION_TARGETS = {
        "adult_male": {
            "protein_g": 80,
            "carbohydrate_g": 350,
            "fat_g": 70,
            "dietary_fiber_g": 30,
            "calcium_mg": 800,
            "iron_mg": 10,
            "sodium_mg": 2300,
            "potassium_mg": 3500,
            "vitamin_c_mg": 100,
        },
        "adult_female": {
            "protein_g": 65,
            "carbohydrate_g": 280,
            "fat_g": 55,
            "dietary_fiber_g": 25,
            "calcium_mg": 700,
            "iron_mg": 18,
            "sodium_mg": 2000,
            "potassium_mg": 3000,
            "vitamin_c_mg": 100,
        }
    }

    # ===== Rerank Config =====
    RECOMMENDED_TOP_K = 5  # 최종 추천 음식 개수
    RETRIEVER_TOP_K = 50  # Chroma에서 가져올 후보 개수

    # ===== Hashtag =====
    DIET_HASHTAG = "#식단"


# 싱글톤 인스턴스
config = Config()
