from app.common.config.logging.log import logging
from app.common.config.parsing.parsing_config import ParsingConfig, ParsingSettings
from app.utils.parsing.parsing_utils import detect_mode, get_row_parser
from app.services.chunking_service import chunk_text_sentences
from dotenv import load_dotenv
from tempfile import gettempdir
from app.common.constants import POST_URL
from app.services.transfer_service import send_data
from app.utils.parsing.parsing_utils import preprocess_columns, get_extension
from app.utils.parsing.html_utils import strip_html_tags
import httpx
import traceback
import os
import pandas as pd
from pathlib import Path

TEMP_DIR = os.path.join(gettempdir(), "god_temp_files")
os.makedirs(TEMP_DIR, exist_ok=True)

load_dotenv()

SUPPORTED_FORMATS = os.getenv("SUPPORTED_FORMATS", "")
SUPPORTED_FORMATS = [ext.strip().lower() for ext in SUPPORTED_FORMATS.split(",") if ext.strip()]

# 파싱 후 Spring 서버로 데이터를 전송하는 메서드
async def process_and_forward(file_path: str, filename: str, dataset: str, email: str):
    is_failed = False

    # 1. 파싱
    try:
        check_file_ext(filename)
        data = parsing_data(file_path, filename)

    except Exception as e:
        print(f"[ERROR] 문서 파싱 실패: {e.__class__.__name__}: {e}")
        traceback.print_exc()
        is_failed = True
        data = []

    # 2. 전송
    try:
        await send_data(data, dataset, email, is_failed)
    except Exception as e:
        print(f"[ERROR] POST 전송 실패: {e.__class__.__name__}: {e}")
        traceback.print_exc()

    # 3. 파일 삭제
    try:
        os.remove(file_path)
        print(f"[CLEANUP] 파일 삭제 완료: {file_path}")
    except Exception as e:
        print(f"[CLEANUP ERROR] 파일 삭제 실패: {e}")


# 유형 확인 후 파싱
def check_file_ext(filename: str) -> list[dict]:
    ext = get_extension(filename)

    if ext not in SUPPORTED_FORMATS:
        raise ValueError(f"지원하지 않는 파일 형식입니다: .{ext}")


# 유형에 따라 문서 읽은 후 반환
def read_data(file_path: str, ext: str) -> pd.DataFrame:
    reader_map = ParsingConfig.reader_map()
    reader = reader_map.get(ext)

    if not reader:
        raise ValueError(f"지원하지 않는 확장자입니다: {ext}")

    return reader(file_path)


# 실제 데이터 파싱 진행 메서드
def parsing_data(file_path: str, filename: str) -> list[dict]:
    settings = ParsingConfig.settings()

    # 결과값 및 문서 시작 번호 초기화
    results = []
    doc_number = 1

    # 주어진 파일을 읽고 병렬로 파싱 처리
    try:
        ext = get_extension(filename)
        df = read_data(file_path, ext)
        df = preprocess_columns(df)  # 중복 컬럼 처리 반드시 해줘야 함
    except Exception as e:
        logging.error(f"[{ext} READ ERROR] {e}")
        raise

    # 파싱 모드 판별
    try:
        mode = detect_mode(df, settings)
        logging.info(f"[{ext} MODE 감지] :  {mode}")
    except Exception as e:
        logging.warning(f"{ext} 모드 자동 판별 실패")
        logging.error(str(e))
        raise e

    # 각 행을 파싱 후, answer를 청크 단위로 분할
    for _, row in df.iterrows():
        parsed = parse_row(row, mode, settings)

        question = parsed.get("question", "")
        answer = parsed.get("answer", "")

        answer_chunks = chunk_text_sentences(answer)

        for chunk in answer_chunks:
            chunk = chunk.strip()
            if not chunk:
                continue
            results.append({
                "doc_id": str(doc_number),
                "question": question,
                "answer": chunk
            })
            doc_number += 1

    return results


def parse_row(row: pd.Series, mode: str, settings: ParsingSettings) -> dict:
    # 주어진 행(row)을 파싱하여 question-answer 딕셔너리로 반환
    parser_fn = get_row_parser(mode, settings)

    if not parser_fn:
        logging.warning(f"[UNKNOWN MODE] {mode} → 기본 빈 문자열 처리")
        return {"question": "", "answer": ""}

    question, answer = parser_fn(row)
    return {
        "question": strip_html_tags(question),
        "answer": strip_html_tags(answer)
    }
