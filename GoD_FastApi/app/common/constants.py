from dotenv import load_dotenv
import os

load_dotenv()

POST_URL = os.getenv("POST_ENDPOINT")
MAX_TOKENS = int(os.getenv("MAX_TOKENS"))
OVERLAP_TOKENS = int(os.getenv("OVERLAP_TOKENS"))
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL")

YAML_PATH = "app/common/config/parsing/yaml"
