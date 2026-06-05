from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"


class Settings(BaseSettings):
    openrouter_api_key: str
    base_url: str = "https://openrouter.ai/api/v1"
    request_timeout: float = 90.0
    ai_models: str = (
        "google/gemma-4-31b-it:free,"
        "nvidia/nemotron-3-super-120b-a12b:free,"
        "google/gemma-4-26b-a4b-it:free,"
        "meta-llama/llama-3.3-70b-instruct:free,"
        "qwen/qwen3-next-80b-a3b-instruct:free"
    )

    model_config = SettingsConfigDict(
        env_file=str(_ENV_FILE),
        env_file_encoding="utf-8",
        protected_namespaces=(),
    )

    @property
    def model_chain(self) -> list[str]:
        return [m.strip() for m in self.ai_models.split(",") if m.strip()]


settings = Settings()
