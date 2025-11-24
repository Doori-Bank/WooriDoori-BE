# WAS 서버 설정 가이드

## 서버 정보
- **서버 IP**: 172.16.1.120
- **역할**: 애플리케이션 서버 (우리두리 인스턴스 + Nginx + Prometheus)

## 필요한 파일 구조

```
WooriDoori-App-BE/
├── docker-compose.loadbalancer.yml  # 메인 docker-compose 파일
├── Woori-Doori-BE/                  # 애플리케이션 코드
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/
├── nginx/                           # Nginx 설정
│   └── nginx.conf
└── prometheus/                      # Prometheus 설정
    └── prometheus.yml
```

## 설정 순서

### 1단계: 파일 준비

WAS 서버에 다음 파일들을 배치하세요:

```bash
# 프로젝트 디렉토리 생성
mkdir -p /opt/wooridoori
cd /opt/wooridoori

# 필요한 파일들 복사 (로컬에서)
# - docker-compose.loadbalancer.yml
# - nginx/ 폴더
# - prometheus/ 폴더
# - Woori-Doori-BE/ 폴더 (애플리케이션 코드)
```

### 2단계: 환경 변수 설정

**`.env` 파일 위치**: `docker-compose.loadbalancer.yml` 파일과 같은 디렉토리

```bash
cd /opt/wooridoori/WooriDoori-App-BE
nano .env
```

**중요**: `.env` 파일은 반드시 `docker-compose.loadbalancer.yml`과 같은 디렉토리에 있어야 합니다!

`.env` 파일 내용:

```bash
# 데이터베이스 설정 (db1 - 우리두리)
DB1_URL=jdbc:mysql://your-db1-host:3306/wooridoori_db1
DB1_USERNAME=your_db1_username
DB1_PASSWORD=your_db1_password

# 데이터베이스 설정 (db2 - 두리뱅크)
DB2_URL=jdbc:mysql://your-db2-host:3306/dooribank_db2
DB2_USERNAME=your_db2_username
DB2_PASSWORD=your_db2_password

# Redis 설정 (기존 Redis 사용)
REDIS_URL=host.docker.internal  # 호스트의 Redis
# 또는 외부 Redis 서버: REDIS_URL=192.168.1.100
REDIS_PORT=6379

# JWT 시크릿 키
JWT=your_jwt_secret_key_here

# Groq API 키 (AI 기능용)
GROQ_API_KEY=your_groq_api_key_here

# Ollama (선택사항, AI 기능 사용 시)
OLLAMA_BASE_URL=http://ollama:11434
```

### 3단계: Prometheus 설정 확인

`prometheus/prometheus.yml` 파일이 올바르게 설정되어 있는지 확인:

```yaml
# 메트릭 수집 대상이 172.16.1.120으로 설정되어 있는지 확인
targets: ['172.16.1.120:8080']  # 인스턴스 1
targets: ['172.16.1.120:8081']  # 인스턴스 2 (포트 확인 필요)
```

### 4단계: 애플리케이션 빌드 (선택사항)

Docker 이미지를 빌드하려면:

```bash
cd /opt/wooridoori/WooriDoori-App-BE/Woori-Doori-BE

# JAR 파일 빌드 (GitHub Actions에서 빌드했다면 생략 가능)
./gradlew clean bootJar -x test

# 또는 Docker 이미지 직접 빌드
docker build -t wooridoori-be:latest .
```

### 5단계: Docker Compose 실행

```bash
cd /opt/wooridoori/WooriDoori-App-BE

# 전체 스택 실행
docker-compose -f docker-compose.loadbalancer.yml up -d

# 로그 확인
docker-compose -f docker-compose.loadbalancer.yml logs -f

# 특정 서비스만 확인
docker-compose -f docker-compose.loadbalancer.yml logs -f nginx-lb
docker-compose -f docker-compose.loadbalancer.yml logs -f wooridoori-instance1
docker-compose -f docker-compose.loadbalancer.yml logs -f prometheus
```

### 6단계: 서비스 확인

#### Nginx 로드밸런서 확인
```bash
curl http://localhost:8080/health
# 또는
curl http://172.16.1.120:8080/health
```

#### 우리두리 인스턴스 확인
```bash
# 인스턴스 1 (내부)
docker exec wooridoori-instance1 curl http://localhost:8080/actuator/health

# 인스턴스 2 (내부)
docker exec wooridoori-instance2 curl http://localhost:8080/actuator/health
```

#### Prometheus 확인
```bash
curl http://localhost:9090/-/healthy
# 또는 브라우저에서
# http://172.16.1.120:9090
```

#### Prometheus 타겟 상태 확인
브라우저에서 `http://172.16.1.120:9090/targets` 접속하여 메트릭 수집 상태 확인

## 실행 중인 서비스 확인

```bash
# 모든 컨테이너 상태 확인
docker-compose -f docker-compose.loadbalancer.yml ps

# 네트워크 확인
docker network ls | grep wooridoori

# 볼륨 확인
docker volume ls | grep wooridoori
```

## 중지 및 재시작

```bash
# 중지
docker-compose -f docker-compose.loadbalancer.yml stop

# 재시작
docker-compose -f docker-compose.loadbalancer.yml restart

# 완전 중지 및 삭제 (데이터 유지)
docker-compose -f docker-compose.loadbalancer.yml down

# 완전 삭제 (데이터까지 삭제)
docker-compose -f docker-compose.loadbalancer.yml down -v
```

## 트러블슈팅

### 포트 충돌
```bash
# 포트 사용 확인
netstat -tulpn | grep -E '8080|9090'
# 또는
lsof -i :8080
lsof -i :9090
```

### 컨테이너가 시작되지 않음
```bash
# 로그 확인
docker-compose -f docker-compose.loadbalancer.yml logs

# 특정 컨테이너 로그
docker logs wooridoori-instance1
docker logs nginx-loadbalancer
docker logs prometheus
```

### 데이터베이스 연결 실패
- `.env` 파일의 DB 설정 확인
- DB 서버가 접근 가능한지 확인: `telnet db-host 3306`
- 방화벽 설정 확인

### Redis 연결 실패
- Redis 서버가 실행 중인지 확인
- `REDIS_URL` 환경 변수 확인
- 호스트의 Redis 접근: `redis-cli -h host.docker.internal ping`

## 주요 포트

- **8080**: Nginx 로드밸런서 (외부 접근)
- **9090**: Prometheus (외부 접근)
- **8080** (내부): 우리두리 인스턴스 1, 2 (Nginx를 통해서만 접근)

## 다음 단계

WAS 서버 설정이 완료되면:
1. 모니터링 서버에서 Grafana 설정
2. Grafana가 Prometheus(`http://172.16.1.120:9090`)에 연결되는지 확인

