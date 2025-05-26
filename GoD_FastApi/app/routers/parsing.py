import os
import asyncio
import aiofiles
from fastapi import APIRouter, UploadFile, File, Query
from tempfile import gettempdir
from app.services.parsing_service import process_and_forward

router = APIRouter()
TEMP_DIR = os.path.join(gettempdir(), "god_temp_files")
os.makedirs(TEMP_DIR, exist_ok=True)


@router.post("")
async def parse_file(
    file: UploadFile = File(...),
    dataSet: str = Query(...),
    email: str = Query(...)
):
    temp_path = os.path.join(TEMP_DIR, file.filename)

    # 1. 파일 저장
    async with aiofiles.open(temp_path, "wb") as f:
        content = await file.read()
        await f.write(content)

    # 2. 사용자에게 먼저 응답하고, 파싱 및 전송은 백그라운드에서 처리
    asyncio.create_task(process_and_forward(
        temp_path, file.filename, dataSet, email))

    return {"status": "ok", "message": "파일 수신 완료. 처리 중입니다."}
