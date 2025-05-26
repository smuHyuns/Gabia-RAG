import httpx
from app.common.constants import POST_URL


async def send_data(data: list[dict], dataset: str, email: str, is_failed: bool = False):
    # Spring 서버에 데이터 전달
    payload = {
        "dataSet": dataset,
        "email": email,
        "data": data,
        "isFailed": is_failed
    }

    async with httpx.AsyncClient(timeout=30.0) as client:
        res = await client.post(POST_URL, json=payload)
        res.raise_for_status()
        print(f"[POST] 성공적으로 전송됨 - 응답 코드: {res.status_code}")
