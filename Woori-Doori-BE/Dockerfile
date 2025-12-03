# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Gradle Wrapper 및 설정 파일 복사
COPY gradle ./gradle
COPY gradlew ./
COPY build.gradle settings.gradle ./

# 의존성 다운로드 (캐시 활용)
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 타임존, JVM 옵션
ENV TZ=Asia/Seoul
ENV JAVA_OPTS=""

# 빌드된 JAR 파일 복사
COPY --from=builder /build/build/libs/*.jar app.jar

# non-root 사용자 생성(보안)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
 && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
