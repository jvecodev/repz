from typing import AsyncGenerator, List, Dict

from openai import AsyncOpenAI

from app.config import settings


def _get_client() -> AsyncOpenAI:
    return AsyncOpenAI(
        base_url=settings.base_url,
        api_key=settings.openrouter_api_key,
        default_headers={"X-Title": "Repz AI Service"},
        timeout=120.0,
    )


async def chat_stream(messages: List[Dict[str, str]]) -> AsyncGenerator[str, None]:
    client = _get_client()
    response = await client.chat.completions.create(
        model=settings.model,
        messages=messages,
        stream=True,
        extra_body={"models": [settings.model, *settings.fallback_models]},
    )
    async for chunk in response:
        if chunk.choices and chunk.choices[0].delta.content:
            yield chunk.choices[0].delta.content


async def report_buffer(prompt: str, system_prompt: str) -> str:
    client = _get_client()
    response = await client.chat.completions.create(
        model=settings.model,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": prompt},
        ],
        extra_body={"models": [settings.model, *settings.fallback_models]},
        stream=False,
    )
    return response.choices[0].message.content
