FROM amazoncorretto:17

WORKDIR /app

# JAR 복사
COPY build/libs/*.jar app.jar

# entrypoint.sh 복사
COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

# 애플리케이션 실행을 entrypoint.sh로 대체
ENTRYPOINT ["./entrypoint.sh"]