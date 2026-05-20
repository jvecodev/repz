from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"


class Settings(BaseSettings):
    openrouter_api_key: str
    model: str = "google/gemma-4-31b-it:free"
    fallback_models: list[str] = [
        "deepseek/deepseek-v4-flash:free",
        "openai/gpt-oss-20b:free",
    ]
    base_url: str = "https://openrouter.ai/api/v1"

    model_config = SettingsConfigDict(env_file=str(_ENV_FILE), env_file_encoding="utf-8")


settings = Settings()