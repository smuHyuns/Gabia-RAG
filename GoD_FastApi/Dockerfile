# 1. Python 기반 경량 이미지 사용
FROM python:3.13.2-slim

# 2. 환경변수 및 작업 디렉토리 설정
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

# 3. 시스템 의존성 설치 (예: cert 관련 모듈 대비)
RUN apt-get update && apt-get install -y \
    gcc \
    libffi-dev \
    libssl-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 4. requirements 복사 및 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 5. 프로젝트 전체 복사
COPY . .

# 6. uvicorn으로 FastAPI 실행
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
