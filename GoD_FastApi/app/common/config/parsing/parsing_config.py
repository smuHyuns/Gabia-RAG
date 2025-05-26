# ✅ parsing_config.py

import yaml
import pandas as pd
from pathlib import Path
from pydantic import BaseModel
from typing import Dict, List, Callable
from app.common.constants import YAML_PATH

class ParsingMapping(BaseModel):
    question_fields: List[str]
    answer_fields: List[str]

class ParsingSettings(BaseModel):
    default_mode: str
    mode_mappings: Dict[str, ParsingMapping]

class ParsingConfig:

    @staticmethod
    def settings() -> ParsingSettings:
        path = Path(f"{YAML_PATH}/parsing_config.yaml")
        with open(path, "r", encoding="utf-8") as f:
            raw_data = yaml.safe_load(f)

        for mapping in raw_data["mode_mappings"].values():
            mapping["question_fields"] = [field.lower() for field in mapping["question_fields"]]
            mapping["answer_fields"] = [field.lower() for field in mapping["answer_fields"]]

        return ParsingSettings(
            default_mode=raw_data["default_mode"],
            mode_mappings={
                mode: ParsingMapping(**mapping)
                for mode, mapping in raw_data["mode_mappings"].items()
            }
        )

    @staticmethod
    def reader_map() -> Dict[str, Callable]:
        path = Path(f"{YAML_PATH}/reader_map.yaml")
        with open(path, "r", encoding="utf-8") as f:
            config = yaml.safe_load(f)

        reader_functions = {
            "read_csv": pd.read_csv,
            "read_csv_tsv": lambda f: pd.read_csv(f, sep="\t"),
            "read_excel": pd.read_excel,
            "read_json": pd.read_json
        }

        return {
            ext: reader_functions[func_name]
            for ext, func_name in config["reader_map"].items()
        }
