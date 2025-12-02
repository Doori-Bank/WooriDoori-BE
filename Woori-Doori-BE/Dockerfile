# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk-alpine3.20 AS builder
WORKDIR /build

COPY gradle ./gradle
COPY gradlew ./
COPY build.gradle settings.gradle ./

RUN mkdir -p /root/.gradle

RUN --mount=type=cache,target=/root/.gradle/caches \
    chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle/caches \
    ./gradlew bootJar -x test --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-alpine3.20.2
WORKDIR /app

ENV TZ=Asia/Seoul
ENV JAVA_OPTS=""

COPY --from=builder /build/build/libs/*.jar app.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
 && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
