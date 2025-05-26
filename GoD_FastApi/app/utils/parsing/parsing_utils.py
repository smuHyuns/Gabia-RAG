import pandas as pd
from collections import defaultdict
from app.common.config.parsing.parsing_config import ParsingSettings
from pathlib import Path


# 1. 중복된 컬럼 대상으로 접미사 붙여서 고유하게 만드는 함수 ( subect -> subject.1 )
def deduplicate_columns(columns):
    counter = defaultdict(int)
    new_columns = []
    for col in columns:
        col_lower = col.lower()
        if counter[col_lower]:
            new_columns.append(f"{col_lower}.{counter[col_lower]}")
        else:
            new_columns.append(col_lower)
        counter[col_lower] += 1

    # print(new_columns)  # 디버깅용 출력
    return new_columns


#2. NaN 방지 및 공백 제거를 위한 안전한 문자열 반환 함수
def safe_str(value):
    return "" if pd.isna(value) else str(value).strip()

#3. 컬럼명 기반으로 설정된 mode를 자동으로 감지하는 함수
def detect_mode(df: pd.DataFrame, parsing_settings: ParsingSettings) -> str:
    df.columns = deduplicate_columns(df.columns)
    lower_cols = {col.lower() for col in df.columns}

    for mode, mapping in parsing_settings.mode_mappings.items():
        question_fields = [f.lower() for f in mapping.question_fields]
        answer_fields = [f.lower() for f in mapping.answer_fields]

        if set(question_fields + answer_fields).issubset(lower_cols):
            return mode

    return parsing_settings.default_mode

#4. 감지된 모드에 따라서 행을 파싱하는 함수
def get_row_parser(mode: str, parsing_settings: ParsingSettings):
    mapping = parsing_settings.mode_mappings.get(mode)
    if not mapping:
        return None

    def parser_fn(row: pd.Series):
        question = " ".join([safe_str(row.get(f, ""))
                            for f in mapping.question_fields])
        answer = " ".join([safe_str(row.get(f, ""))
                          for f in mapping.answer_fields])
        return question, answer

    return parser_fn


#5.dataframe의 컬럼명을 중복제거 후 반환
def preprocess_columns(df: pd.DataFrame) -> pd.DataFrame:
    # 엑셀 DataFrame의 컬럼명을 deduplicate_columns()를 통해 중복 제거 후 반환.
    df.columns = deduplicate_columns(df.columns)
    return df

#6. 데이터의 유형 분리 후 전달
def get_extension(filename: str):
    ext = Path(filename).suffix.lstrip(".").lower()
    return ext