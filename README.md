# Genie of Document
가비아 사내문서 RAG 시스템

링크 : http://139.150.72.182/

**팀원** : Eddie(임정우), Ryan(최현수)

<br>

## 사용기술
**Back-End** : Spring Boot 3.4.4, Spring WebFlux, Java 17, RabbitMQ, WebSocket \
**Vector DataBase** : Qdrant \
**Embedding Model** : text-embedding-3-large \
**LLM Model** :  gpt-4.1 

<br>

## 제공 API
### 1. RAG 서비스
현재 카테고리는 `문의사항(INQUIRY)`, `고객의 소리(VOC)` 로 구성되어 있습니다.\
카테고리 선택 후, 관련된 질문을 입력할 시 해당 질문에 대한 응답을 제공합니다.

### 2. 파일 벡터 데이터베이스 업로드
벡터 데이터베이스에 요청받은 파일의 정보를 임베딩하여 업로드합니다.\
현재 업로드 가능한 파일의 양식은 csv, xlsx, json, tsv 파일입니다.\
이외의 관련 정보로는
- 데이터베이스 : Qdrant
- 거리계산 방식 : Cosine
- 컬렉션 크기 : 3072

가 있습니다.


<br>

## 실행 화면
### 1. 카테고리 선택
![초기화면](https://github.com/user-attachments/assets/85193794-0021-4ae2-a180-e39cdc0dfac6)

<br>
<br>

### 2. 유효한 질문에 대한 답변 제공
![MVP_정상대답](https://github.com/user-attachments/assets/d2c4edc3-bce2-4c66-8e28-e45771fb68e5)

<br>
<br>

### 3. 유효하지 않은 질문에 대한 답변 생성 X(할루시네이션 방지)
![MVP_오답](https://github.com/user-attachments/assets/b06c35de-96fc-43a4-9d0e-993f7a00be90)

<br>

### 4. 파일을 벡터데이터베이스에 업로드

### 4.1 사용자 선응답

![업로드 요청](https://github.com/user-attachments/assets/5ce0f9a5-711b-4c2a-a7cd-ccb2fd534643)

<br>
<br>

### 4.2 작업 완료 후 알림
![업로드 알림](https://github.com/user-attachments/assets/d9e632e6-be9d-4c5e-9c83-be51c6c57ff7)



<br>
<br>

### 5. 모니터링
### 5.1 RabbitMQ


https://github.com/user-attachments/assets/5fae038f-e7e1-4327-bf6f-4b656cf71c40


<br>
<br>

### 5.2 Prometheus & Grafana
![Grafana Prometheus](https://github.com/user-attachments/assets/9cea0a6d-efd2-4503-bb06-fe2ec813450a)


<br>
<br>
<br>


## 시스템 아키텍쳐
![아키텍쳐](https://github.com/user-attachments/assets/686f2d89-e54e-4462-a546-d663eefe9390)





