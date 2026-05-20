import json

from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from app.services.ai_service import chat_stream

router = APIRouter(prefix="/chat", tags=["chat"])


class ChatRequest(BaseModel):
    message: str


@router.post("/stream")
async def stream_chat(request: ChatRequest):
    messages = [{"role": "user", "content": request.message}]

    async def event_generator():
        async for chunk in chat_stream(messages):
            yield f"data: {json.dumps({'content': chunk})}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")
