from fastapi import FastAPI

from app.routes import chat, report

app = FastAPI(title="Repz AI Service", version="1.0.0")

app.include_router(chat.router)
app.include_router(report.router)


@app.get("/health", tags=["health"])
def health_check():
    return {"status": "ok"}
