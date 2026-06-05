import logging
from typing import AsyncGenerator, Dict, List, Optional

from openai import APIError, AsyncOpenAI

from app.config import settings

logger = logging.getLogger("repz.ai")


def _get_client() -> AsyncOpenAI:
    return AsyncOpenAI(
        base_url=settings.base_url,
        api_key=settings.openrouter_api_key,
        default_headers={"X-Title": "Repz AI Service"},
        timeout=settings.request_timeout,
    )


class AllModelsUnavailableError(RuntimeError):
    """Levantado quando todos os modelos da cadeia falham (ex.: rate-limit em cascata)."""


async def chat_stream(messages: List[Dict[str, str]]) -> AsyncGenerator[str, None]:
    client = _get_client()
    last_error: Optional[Exception] = None

    for model in settings.model_chain:
        emitted = False
        try:
            response = await client.chat.completions.create(
                model=model,
                messages=messages,
                stream=True,
            )
            async for chunk in response:
                if chunk.choices and chunk.choices[0].delta.content:
                    emitted = True
                    yield chunk.choices[0].delta.content
            return
        except APIError as e:
            last_error = e
            # Se já enviamos parte da resposta, não dá para trocar de modelo no meio do stream.
            if emitted:
                raise
            logger.warning("Modelo %s indisponível no chat (%s); tentando próximo.", model, e)

    raise AllModelsUnavailableError(
        f"Nenhum modelo disponível para chat. Último erro: {last_error}"
    )


async def report_buffer(prompt: str, system_prompt: str) -> str:
    client = _get_client()
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": prompt},
    ]
    last_error: Optional[Exception] = None

    for model in settings.model_chain:
        try:
            logger.info("Gerando relatório com modelo %s", model)
            response = await client.chat.completions.create(
                model=model,
                messages=messages,
                max_tokens=700,
                stream=False,
            )
            content = response.choices[0].message.content
            if content and content.strip():
                logger.info("Relatório gerado com %s (%d chars)", model, len(content))
                return content
            logger.warning("Modelo %s retornou conteúdo vazio; tentando próximo.", model)
        except APIError as e:
            last_error = e
            logger.warning("Modelo %s indisponível (%s); tentando próximo.", model, e)

    raise AllModelsUnavailableError(
        f"Nenhum modelo conseguiu gerar o relatório. Último erro: {last_error}"
    )
