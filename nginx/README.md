# Nginx 로드밸런서 설정

## 개요

이 디렉토리는 우리두리 서버를 두 개의 인스턴스로 실행하고 Nginx 로드밸런서를 통해 요청을 분산하는 설정을 포함합니다.

## 구조

```
[두리뱅킹] → [Nginx 로드밸런서:8080] → [우리두리 인스턴스1:8080]
                                      → [우리두리 인스턴스2:8080]
```

## 사용 방법

### 1. 환경 변수 설정

`.env` 파일을 프로젝트 루트에 생성하고 필요한 환경 변수를 설정하세요:

```bash
# 데이터베이스 설정
DB1_URL=jdbc:mysql://localhost:3306/wooridoori_db1
DB1_USERNAME=your_username
DB1_PASSWORD=your_password

DB2_URL=jdbc:mysql://localhost:3306/dooribank_db2
DB2_USERNAME=your_username
DB2_PASSWORD=your_password

# Redis 설정 (기존 Redis 사용)
# 호스트 머신의 Redis에 접근하려면 host.docker.internal 사용
# 또는 외부 Redis 서버의 IP/도메인 사용
REDIS_URL=host.docker.internal  # 호스트 머신의 Redis: host.docker.internal
                                 # 외부 서버: 192.168.1.100 또는 redis.example.com
REDIS_PORT=6379

# JWT 시크릿
JWT=your_jwt_secret_key

# Groq API 키
GROQ_API_KEY=your_groq_api_key
```

### 2. Docker Compose 실행

```bash
# 빌드 및 실행
docker-compose -f docker-compose.loadbalancer.yml up -d --build

# 로그 확인
docker-compose -f docker-compose.loadbalancer.yml logs -f

# 특정 서비스 로그만 확인
docker-compose -f docker-compose.loadbalancer.yml logs -f nginx-lb
docker-compose -f docker-compose.loadbalancer.yml logs -f wooridoori-instance1
docker-compose -f docker-compose.loadbalancer.yml logs -f wooridoori-instance2
```

### 3. 두리뱅킹 설정 변경

`DooriBank-BE`의 환경 변수 또는 `application.yml`에서 우리두리 API URL을 변경:

```yaml
# application.yml
wooridoori:
  api:
    url: ${WOORIDOORI_API_URL:http://nginx-lb:8080}
```

또는 환경 변수:
```bash
WOORIDOORI_API_URL=http://nginx-lb:8080
```

### 4. 중지 및 삭제

```bash
# 중지
docker-compose -f docker-compose.loadbalancer.yml stop

# 중지 및 컨테이너 삭제
docker-compose -f docker-compose.loadbalancer.yml down

# 볼륨까지 삭제 (주의: 데이터 삭제됨)
docker-compose -f docker-compose.loadbalancer.yml down -v
```

## 로드밸런싱 방식

### 기본: 라운드로빈 (Round Robin)
요청을 순차적으로 각 인스턴스에 분산합니다.

### 가중치 방식 (Weighted Round Robin)
특정 인스턴스에 더 많은 요청을 보내고 싶을 때 사용:

```nginx
server wooridoori-instance1:8080 weight=3;
server wooridoori-instance2:8080 weight=1;
```

### 최소 연결 수 방식 (Least Connections)
현재 연결 수가 가장 적은 인스턴스에 요청을 보냅니다:

```nginx
least_conn;
```

## 헬스체크

- Nginx: `http://localhost:8080/health`
- 우리두리 인스턴스: 각 인스턴스의 `/actuator/health` 엔드포인트

## 트러블슈팅

### Nginx가 백엔드 서버를 찾을 수 없음
- `docker-compose.loadbalancer.yml`에서 `depends_on`이 올바르게 설정되어 있는지 확인
- 네트워크가 올바르게 설정되어 있는지 확인: `docker network ls`

### 요청이 한 인스턴스로만 가는 경우
- Nginx 설정 파일의 로드밸런싱 방식 확인
- 각 인스턴스의 헬스체크 상태 확인: `docker-compose ps`

### 로그 확인
```bash
# Nginx 액세스 로그
docker exec nginx-loadbalancer cat /var/log/nginx/wooridoori_access.log

# Nginx 에러 로그
docker exec nginx-loadbalancer cat /var/log/nginx/wooridoori_error.log
```

## 성능 최적화

### Keepalive 연결
Nginx 설정에서 `keepalive 32`로 설정하여 연결을 재사용합니다.

### 프록시 버퍼링
대용량 응답을 위한 버퍼링 설정이 포함되어 있습니다.

## 모니터링 (k6 + Prometheus + Grafana)

부하 테스트 및 메트릭 모니터링을 위해 k6, Prometheus, Grafana가 포함되어 있습니다.

### 접속 정보
- **Prometheus**: http://localhost:9090 (이 서버)
- **Grafana**: 별도 서버에서 실행 (포트 3000)
  - 실행: `docker-compose -f docker-compose.grafana.yml up -d`
  - 설정: `grafana/README.md` 참고

### k6 테스트 실행
```bash
# k6 컨테이너에서 실행 (v0.47.0+)
docker exec -it k6-runner k6 run \
  --out experimental-prometheus-rw=http://prometheus:9090/api/v1/write \
  /scripts/full_workflow_stress_test.js

# 또는 JSON 출력 (모든 버전 지원)
docker exec -it k6-runner k6 run \
  --out json=/tmp/k6-results.json \
  /scripts/full_workflow_stress_test.js
```

자세한 내용은 `load_test/README_K6_MONITORING.md`를 참고하세요.

## 주의사항

1. **세션 관리**: 세션을 사용하는 경우 세션 스티키니스(Sticky Session) 설정이 필요할 수 있습니다.
2. **파일 업로드**: `client_max_body_size`를 필요에 따라 조정하세요.
3. **환경 변수**: 모든 필요한 환경 변수가 설정되어 있는지 확인하세요.
4. **모니터링**: Prometheus와 Grafana는 기본적으로 실행되며, k6는 `--profile loadtest`로 실행할 수 있습니다.

