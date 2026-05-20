from fastapi import APIRouter
from pydantic import BaseModel

from app.services.ai_service import report_buffer

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
    content = await report_buffer(request.prompt, request.system_prompt)
    return ReportResponse(content=content)
