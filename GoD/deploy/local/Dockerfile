FROM openjdk:17-alpine

# 서버 헬스체크 용 curl 추가
RUN apk add --no-cache curl

# 인증서 복사 (context 기준으로 경로 지정)
COPY deploy/local/gabia_ssl.crt /usr/local/share/ca-certificates/gabia_ssl.crt

# 인증서 등록
RUN apk add --no-cache ca-certificates && \
    update-ca-certificates && \
    keytool -importcert \
      -alias gabia \
      -file /usr/local/share/ca-certificates/gabia_ssl.crt \
      -keystore $JAVA_HOME/lib/security/cacerts \
      -storepass changeit \
      -noprompt

# 애플리케이션 JAR 복사
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 실행
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=dev", "-jar", "/app.jar"]
