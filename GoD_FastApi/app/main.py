from fastapi import FastAPI
from app.routers import parsing

app = FastAPI()
app.include_router(parsing.router, prefix="/api/upload", tags=["Parsing"])


@app.get("/")
def read_root():
    return {"Welcome Genie of Document Embedding Server"}
