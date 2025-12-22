import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    GMS_OPENAI_API_KEY: str = os.getenv("GMS_OPENAI_API_KEY")
    GMS_OPENAI_BASE_URL: str = os.getenv(
        "GMS_OPENAI_BASE_URL",
        "https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions"
    )
    GMS_OPENAI_MODEL: str = os.getenv("GMS_OPENAI_MODEL", "gpt-4o")


settings = Settings()
