# syntax=docker/dockerfile:1.4
# ---------- Build stage ----------
    FROM eclipse-temurin:17-jdk-alpine AS builder
    WORKDIR /build
    
    # Gradle Wrapper 및 설정 파일 복사
    COPY gradle ./gradle
    COPY gradlew ./
    COPY build.gradle settings.gradle ./
    
    # Gradle 캐시 디렉토리 생성 (BuildKit 캐시 마운트용)
    RUN mkdir -p /root/.gradle
    
    # 의존성 다운로드 (BuildKit 캐시 활용)
    RUN --mount=type=cache,target=/root/.gradle/caches \
        chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true
    
    # 소스 코드 복사 및 빌드 (BuildKit 캐시 활용)
    COPY src ./src
    # clean 제거 (Docker 빌드에서는 불필요, 캐시 활용)
    RUN --mount=type=cache,target=/root/.gradle/caches \
        ./gradlew bootJar -x test --no-daemon
    
    # ---------- Runtime stage ----------
    FROM eclipse-temurin:17-jre-alpine
    WORKDIR /app
    
    # 타임존, JVM 옵션
    ENV TZ=Asia/Seoul
    ENV JAVA_OPTS=""
    
    # 빌드된 JAR 파일 복사
    COPY --from=builder /build/build/libs/*.jar app.jar
    
    # # non-root 사용자 생성(보안)
    # RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    #  && chown -R appuser:appgroup /app
    # USER appuser
    
    EXPOSE 8080
    
    ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]