# 우리두리 전체 워크플로우 스트레스 테스트

## 개요

실제 DooriBank에 있는 회원 데이터를 사용하여 우리두리의 전체 사용자 워크플로우를 시뮬레이션하는 스트레스 테스트입니다.

## 테스트 시나리오

1. **회원가입**: 두리뱅크에 내역이 있는 사람이 우리두리에 회원가입
2. **카드 등록**: 그 사람들이 한꺼번에 자신의 카드를 등록
3. **목표 설정**: 그달의 목표 등록
4. **소비 리포트**: 하필 그날이 월말이라 소비리포트가 날라올 때임 → 소비 리포트 발행

## 사전 준비

### 1. 서버 실행 확인
- **DooriBank-BE**: 포트 8081 (실제 회원 데이터 조회용)
- **WooriDoori-BE**: 포트 8080 (우리두리 백엔드)

### 2. k6 설치
```bash
# macOS
brew install k6

# Linux
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

## 사용 방법

### 기본 실행
```bash
# localhost 환경
k6 run load_test/full_workflow_stress_test.js

# 커스텀 URL 설정
DOORIBANK_URL=http://113.198.66.75:18177 WOORIDOORI_URL=http://172.16.1.120:8080 \
k6 run load_test/full_workflow_stress_test.js

# 결과를 JSON으로 저장
k6 run --out json=results.json load_test/full_workflow_stress_test.js

# 결과를 InfluxDB로 전송 (Grafana 시각화)
k6 run --out influxdb=http://localhost:8086/k6 load_test/full_workflow_stress_test.js
```

### 커스텀 옵션 실행
```bash
# 더 많은 사용자로 테스트
k6 run --vus 5000 --duration 5m load_test/full_workflow_stress_test.js

# 특정 서버 URL로 테스트
DOORIBANK_URL=http://your-dooribank:8081 WOORIDOORI_URL=http://your-wooridoori:8080 \
k6 run load_test/full_workflow_stress_test.js
```

## 워크플로우 상세

### 1단계: 회원가입
- DooriBank의 실제 회원 데이터 사용
- 이름, 전화번호, 주민번호는 실제 데이터 사용
- 이메일은 고유하게 생성 (타임스탬프 + 랜덤 문자열)

**API**: `POST /auth/join`

### 2단계: 로그인
- 회원가입 시 사용한 이메일과 비밀번호로 로그인
- JWT 액세스 토큰 획득

**API**: `POST /auth/login`

### 3단계: 카드 등록
- 랜덤 카드 번호 생성 (16자리)
- 카드 비밀번호, 유효기간, CVC 랜덤 생성
- 주민번호는 회원가입 시 사용한 값 사용

**API**: `PATCH /card/putCard`

### 4단계: 목표 설정
- 직업: 직장인 (EMPLOYEE)
- 월 수입: 300만원
- 목표 소비 금액: 200만원
- 필수 카테고리: 식비, 교통

**API**: `PUT /goal/setgoal`

### 5단계: 소비 리포트 발행
- 메인 페이지 조회를 통해 리포트 정보 확인
- 리포트는 스케줄러가 자동으로 발행하지만, 조회 API를 호출하여 확인

**API**: `GET /main`

## 예상 병목 지점

### 1. 회원가입 단계
- **DB 커넥션 풀 고갈**: 동시 회원가입 시 DB 커넥션 부족
- **이메일 발송 서비스 과부하**: 인증번호 이메일 발송

### 2. 카드 등록 단계
- **카드 검증 로직**: 카드 번호, 비밀번호, CVC 검증
- **DB 트랜잭션**: 카드 정보 저장

### 3. 목표 설정 단계
- **목표 계산 로직**: 목표 달성률 계산
- **DB 트랜잭션**: 목표 정보 저장

### 4. 리포트 발행 단계
- **리포트 생성 로직**: 월간 소비 리포트 생성
- **대량 리포트 생성**: 월말에 모든 사용자의 리포트 동시 생성

## 모니터링 포인트

### 서버 리소스
```bash
# CPU/메모리 모니터링
htop

# 네트워크 연결 수 확인
watch -n 1 'netstat -an | grep :8080 | wc -l'
```

### 데이터베이스
```sql
-- 활성 커넥션 수 확인
SHOW STATUS LIKE 'Threads_connected';

-- 현재 실행 중인 쿼리 확인
SHOW PROCESSLIST;

-- 트랜잭션 상태 확인
SHOW ENGINE INNODB STATUS;
```

### 애플리케이션 로그
```bash
# 실시간 로그 확인
tail -f logs/application.log | grep -E "ERROR|WARN|Exception"

# 에러 카운트
grep -c "ERROR" logs/application.log
```

## 결과 분석

### k6 결과 해석

```
우리두리 전체 워크플로우 스트레스 테스트 결과
(실제 DooriBank 회원 데이터 사용)
============================================================
총 요청 수: 50000
성공률: 92.50%
평균 응답 시간: 1200.50ms
최대 응답 시간: 8500.00ms
95% 응답 시간: 3200.00ms
에러율: 7.50%

단계별 통계:
  Signup: 평균 800.00ms, 최대 3000.00ms
  Login: 평균 200.00ms, 최대 500.00ms
  CardRegistration: 평균 1500.00ms, 최대 5000.00ms
  SetGoal: 평균 1000.00ms, 최대 4000.00ms
  ReportCheck: 평균 500.00ms, 최대 2000.00ms
============================================================
```

**분석 포인트**:
- **카드 등록**: 가장 느린 단계 (평균 1.5초)
- **회원가입**: 두 번째로 느린 단계 (평균 0.8초)
- **에러율**: 7.5%는 높음 (목표: 5% 미만)

## 문제 해결

### 회원가입 실패율이 높은 경우

**증상**: 회원가입 단계에서 많은 실패 발생

**해결 방법**:
1. DB 커넥션 풀 크기 증가 (`maximum-pool-size: 50` 이상)
2. 이메일 발송 서비스를 비동기 처리로 변경
3. 트랜잭션 타임아웃 시간 증가

### 카드 등록 실패율이 높은 경우

**증상**: 카드 등록 단계에서 많은 실패 발생

**해결 방법**:
1. 카드 검증 로직 최적화
2. DB 인덱스 추가 (카드 번호, 회원 ID)
3. 트랜잭션 격리 수준 조정

### 목표 설정 실패율이 높은 경우

**증상**: 목표 설정 단계에서 많은 실패 발생

**해결 방법**:
1. 목표 계산 로직 최적화
2. DB 쿼리 최적화
3. 캐싱 적용 (Redis)

### 리포트 발행 지연

**증상**: 리포트 조회 시 응답 시간이 매우 느림

**해결 방법**:
1. 리포트 생성 로직을 비동기 처리로 변경
2. 리포트를 미리 생성하여 캐싱
3. 리포트 생성 작업을 큐 시스템으로 분산 처리

## 주의사항

1. **프로덕션 환경 금지**: 이 테스트는 테스트 환경에서만 사용하세요.
2. **데이터 정리**: 테스트 후 생성된 더미 계정들을 정리하는 것을 권장합니다.
3. **서버 모니터링**: 테스트 중 서버 리소스를 지속적으로 모니터링하세요.
4. **점진적 증가**: 한 번에 최대 부하를 주지 말고 단계적으로 증가하세요.

## 체크리스트

- [ ] k6 설치 완료
- [ ] DooriBank-BE 서버 실행 확인
- [ ] WooriDoori-BE 서버 실행 확인
- [ ] 테스트 서버 URL 확인
- [ ] 서버 모니터링 도구 준비
- [ ] 데이터베이스 백업 완료
- [ ] 테스트 데이터 정리 계획 수립
- [ ] 서버 리소스 모니터링 시작
- [ ] 스트레스 테스트 실행
- [ ] 결과 분석 및 리포트 작성
- [ ] 서버 튜닝 적용
- [ ] 재테스트

