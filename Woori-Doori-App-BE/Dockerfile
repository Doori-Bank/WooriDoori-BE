# ---------- Build stage ----------
FROM gradle:8.9-jdk17 AS builder
WORKDIR /app

# Gradle 캐시 & 빌드 메타파일 먼저 복사 (캐시효과)
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN chmod +x gradlew

# 의존성 캐시를 위해 한번 내려받기 (네트워크 상태에 따라 실패해도 무방)
RUN ./gradlew --no-daemon dependencyInsight --configuration runtimeClasspath || true

# 소스 전체 복사 후 빌드
COPY . .
# 테스트 제외 빌드 (필요시 -x test 제거)
RUN ./gradlew --no-daemon clean bootJar -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-alpine
# 패키지 업데이트로 취약점 최소화
RUN apk update && apk upgrade && rm -rf /var/cache/apk/*

# Build args from CI
ENV TZ=Asia/Seoul
ENV JAVA_OPTS=""

WORKDIR /app

# 빌드 산출물 복사 (spring boot fat jar)
COPY --from=builder /app/build/libs/*.jar app.jar

# non-root 사용자 생성(보안 강화)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
 && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
