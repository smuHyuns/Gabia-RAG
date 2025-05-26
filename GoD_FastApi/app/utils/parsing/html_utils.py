import warnings
from bs4 import BeautifulSoup, MarkupResemblesLocatorWarning

warnings.filterwarnings("ignore", category=MarkupResemblesLocatorWarning)


def strip_html_tags(text: str) -> str:
    # HTML 태그를 제거하여 텍스트만 반환
    if not text or not isinstance(text, str):
        return ""
    soup = BeautifulSoup(text, "html.parser")
    return soup.get_text(separator=" ", strip=True)
