import logging

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.services.ai_service import AllModelsUnavailableError, report_buffer

logger = logging.getLogger("repz.ai")

router = APIRouter(prefix="/report", tags=["report"])

_DEFAULT_SYSTEM_PROMPT = (
    "Você é um assistente especialista em gerar relatórios estruturados e detalhados "
    "sobre desempenho físico e frequência de alunos de academia."
)


class ReportRequest(BaseModel):
    prompt: str
    system_prompt: str = _DEFAULT_SYSTEM_PROMPT


class ReportResponse(BaseModel):
    content: str


@router.post("", response_model=ReportResponse)
async def generate_report(request: ReportRequest):
    try:
        content = await report_buffer(request.prompt, request.system_prompt)
        return ReportResponse(content=content)
    except AllModelsUnavailableError as e:
        logger.error("Todos os modelos indisponíveis: %s", e)
        raise HTTPException(
            status_code=503,
            detail="Serviço de IA temporariamente indisponível (modelos com rate-limit). "
                   "Tente novamente em alguns instantes.",
        )
    except Exception as e:
        logger.exception("Falha inesperada ao gerar relatório")
        raise HTTPException(status_code=502, detail=f"Erro ao gerar relatório: {e}")
