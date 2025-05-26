import re
import tiktoken
from app.common.constants import MAX_TOKENS, OVERLAP_TOKENS, EMBEDDING_MODEL

_tokenizer = tiktoken.encoding_for_model(EMBEDDING_MODEL)


def count_tokens(text: str) -> int:
    return len(_tokenizer.encode(text))


def split_sentences(text: str) -> list[str]:
    sentence_endings = re.compile(r'(?<=[.!?])\s+')
    return [s.strip() for s in sentence_endings.split(text) if s.strip()]


def chunk_text_sentences(
    text: str,
    max_tokens: int = MAX_TOKENS,
    overlap_tokens: int = OVERLAP_TOKENS
) -> list[str]:
    sentences = split_sentences(text)
    chunks = []
    current_chunk = []
    current_tokens = 0

    for sentence in sentences:
        sent_tokens = count_tokens(sentence)

        if current_tokens + sent_tokens <= max_tokens:
            current_chunk.append(sentence)
            current_tokens += sent_tokens
        else:
            chunk_text = " ".join(current_chunk).strip()
            chunks.append(chunk_text)

            # 오버랩 처리
            if overlap_tokens > 0:
                encoded = _tokenizer.encode(chunk_text)
                overlap_chunk = encoded[-overlap_tokens:]
                overlap_text = _tokenizer.decode(overlap_chunk)
                current_chunk = [overlap_text]
                current_tokens = count_tokens(overlap_text)
            else:
                current_chunk = []
                current_tokens = 0

            current_chunk.append(sentence)
            current_tokens += sent_tokens

    if current_chunk:
        chunks.append(" ".join(current_chunk).strip())

    return chunks
