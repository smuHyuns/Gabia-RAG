# Genie of Document
가비아 사내문서 RAG 시스템

링크 : http://139.150.72.182/

**팀원** : Eddie(임정우), Ryan(최현수)

<br>

## 사용기술
**Back-End** : Spring Boot 3.4.4, Spring WebFlux, Java 17, RabbitMQ, WebSocket \
**Vector DataBase** : Qdrant \
**Embedding Model** : text-embedding-3-small \
**LLM Model** :  gpt-4o-mini 

<br>

## 제공 API
### 1. RAG 서비스
현재 카테고리는 `문의사항(INQUIRY)`, `고객의 소리(VOC)` 로 구성되어 있습니다.\
카테고리 선택 후, 관련된 질문을 입력할 시 해당 질문에 대한 응답을 제공합니다.

### 2. 파일 벡터 데이터베이스 업로드
벡터 데이터베이스에 요청받은 파일의 정보를 임베딩하여 업로드합니다.\
현재 업로드 가능한 파일의 양식은 csv 파일입니다.\
이외의 관련 정보로는
- 데이터베이스 : Qdrant
- 거리계산 방식 : Cosine
- 컬렉션 크기 : 1536

가 있습니다.

<br>

## 실행 화면
### 1. 카테고리 선택
![image](/uploads/6d6795e1b287c7a2b1dbc7388ef4a9c3/image.png)
<br>

### 2. 유효한 질문에 대한 답변 제공
![image](/uploads/0c8de5d73c897d5e2512f63bcf76909e/image.png)
<br>
### 3. 유효하지 않은 질문에 대한 답변 생성 X(할루시네이션 방지)
![image](/uploads/a03758280611e87f49c1a3ca5185707a/image.png)
<br>
### 4. 파일을 벡터데이터베이스에 업로드
![image](/uploads/ce0e12b29a8b5528e41640dd98f10acc/image.png)


<br>

## 시스템 아키텍쳐
추후 삽입 예정입니다. (아직 확정 X)