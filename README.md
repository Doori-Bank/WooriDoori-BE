# 우리두리 Backend

> 우리두리 서비스의 백엔드 애플리케이션입니다.

## 📋 목차

- [팀원 소개](#-팀원-소개)
- [API 명세](#-api-명세)
- [ERD](#-erd)
- [메인 로직](#-메인-로직)
- [성능 개선](#-성능-개선)
- [트러블슈팅](#-트러블슈팅)

---

## 👥 팀원 소개

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/ChatHongPT">
        <img src="https://avatars.githubusercontent.com/u/129854575?v=4" width="180px;" alt="최홍석"/><br />
        <sub><b>최홍석</b></sub>
      </a><br />
      <sub>PM</sub>
    </td>
    <td align="center">
      <a href="https://github.com/yunkihong-dev">
        <img src="https://avatars.githubusercontent.com/u/81303136?v=4" width="180px;" alt="홍윤기"/><br />
        <sub><b>홍윤기</b></sub>
      </a><br />
      <sub>PL</sub>
    </td>
    <td align="center">
      <a href="https://github.com/menzzi">
        <img src="https://avatars.githubusercontent.com/u/124131845?v=4" width="180px;" alt="서민지"/><br />
        <sub><b>서민지</b></sub>
      </a><br />
      <sub>Backend</sub>
    </td>
    <td align="center">
      <a href="https://github.com/GodNowoon">
        <img src="https://avatars.githubusercontent.com/u/59138974?v=4" width="180px;" alt="이노운"/><br />
        <sub><b>이노운</b></sub>
      </a><br />
      <sub>Frontend</sub>
    </td>
    <td align="center">
      <a href="https://github.com/songhajang">
        <img src="https://avatars.githubusercontent.com/u/87272634?v=4" width="180px;" alt="장송하"/><br />
        <sub><b>장송하</b></sub>
      </a><br />
      <sub>Frontend</sub>
    </td>
    <td align="center">
      <a href="https://github.com/Jsumin07">
        <img src="https://avatars.githubusercontent.com/u/218750309?v=4" width="180px;" alt="전수민"/><br />
        <sub><b>전수민</b></sub>
      </a><br />
      <sub>Frontend</sub>
    </td>
  </tr>
</table>

---

## 📡 API 명세

### API 문서

- **Swagger UI**: https://api.wooridoori.site/swagger-ui.html
- **OpenAPI Spec**: SpringDoc OpenAPI 3.0 기반

### 인증 방식

- **JWT (JSON Web Token)** 기반 인증
- **Access Token**: 30분 만료
- **Refresh Token**: 7일 만료
- **Bearer Token** 방식 사용

### 주요 API 엔드포인트

#### 🔐 인증 (Auth)

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| POST | `/api/auth/join` | 회원가입 |
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/reissue` | 토큰 재발급 |
| POST | `/api/auth/logout` | 로그아웃 |
| GET | `/api/auth/user` | 사용자 정보 조회 |
| GET | `/api/auth/idCheck` | ID 중복 체크 |
| POST | `/api/auth/send-verification` | 이메일 인증번호 발송 |
| POST | `/api/auth/verify-email` | 이메일 인증 확인 |
| GET | `/api/auth/searchId` | 아이디 찾기 |
| POST | `/api/auth/genPw` | 임시 비밀번호 발급 |
| POST | `/api/auth/resetPw` | 비밀번호 재설정 |

#### 💳 카드 (Card)

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| GET | `/api/card` | 카드 목록 조회 |
| PATCH | `/api/card/putCard` | 카드 등록 |
| PATCH | `/api/card/editCard` | 카드 별명 수정 |
| PATCH | `/api/card/deleteCard` | 카드 삭제 |
| GET | `/api/card/recommend` | 카드 추천 |

#### 📝 소비 일기 (Diary)

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| GET | `/api/diary` | 소비 일기 전체 조회 (월별) |
| GET | `/api/diary/{diaryId}` | 소비 일기 상세 조회 |
| POST | `/api/diary/insertDiary` | 소비 일기 등록 |
| PUT | `/api/diary/updateDiary/{diaryId}` | 소비 일기 수정 |
| DELETE | `/api/diary/{diaryId}` | 소비 일기 삭제 |

#### 💰 결제 내역 (History)

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| GET | `/api/history/calendar` | 결제 내역 캘린더 조회 |
| GET | `/api/history/calendar/detail/{historyId}` | 결제 내역 상세 조회 |
| PATCH | `/api/history/calendar/{historyId}/{includeInTotal}` | 지출 합계 포함/제외 수정 |
| PATCH | `/api/history/calendar/{historyId}/category` | 카테고리 수정 |
| PATCH | `/api/history/calendar/{historyId}/dutchpay` | 더치페이 인원 수 수정 |
| PATCH | `/api/history/calendar/{historyId}/price` | 소비 금액 수정 |
| POST | `/api/history/calendar/sync` | 결제 내역 동기화 (두리뱅킹 → 우리두리) |

#### 🎯 목표 (Goal)

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| PUT | `/api/goal/setgoal` | 목표 설정 |
| POST | `/api/goal/calculate-scores` | 목표 점수 계산 |
| GET | `/api/goal/getgoalhistory` | 목표 히스토리 조회 |
| GET | `/api/goal/report` | 리포트 조회 |
| GET | `/api/goal/past` | 과거 목표 데이터 조회 |

#### 👤 회원 (Member)

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| POST | `/api/member/essential-categories` | 필수 카테고리 설정 |

#### 🏠 메인 (Main)

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| GET | `/api/main` | 메인 페이지 조회 |

#### 🏪 프랜차이즈 (Franchise)

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| GET | `/api/franchise/{category}` | 카테고리별 프랜차이즈 목록 조회 |

#### 💬 채팅 (Chat)

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| POST | `/api/chat` | AI 두리 비서와 채팅 |

#### 🔔 SSE (Server-Sent Events)

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| GET | `/api/sse/connect` | SSE 연결 |

#### 👨‍💼 관리자 (Admin)

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| GET | `/api/admin/card` | 카드 목록 조회 |
| POST | `/api/admin/createCard` | 카드 생성 |
| PUT | `/api/admin/editCard` | 카드 수정 |
| DELETE | `/api/admin/deleteCard/{cardId}` | 카드 삭제 |
| GET | `/api/admin/members` | 회원 목록 조회 |
| GET | `/api/admin/members/{memberName}` | 회원 검색 |
| PUT | `/api/admin/members/authority` | 회원 권한 변경 |
| POST | `/api/admin/send/diary` | 일기 작성 알림 전송 (특정 사용자) |
| POST | `/api/admin/send/report` | 리포트 알림 전송 (특정 사용자) |
| POST | `/api/admin/send/custom` | 커스텀 알림 전송 (특정 사용자) |
| POST | `/api/admin/send/alldiary` | 일기 작성 알림 전송 (전체 사용자) |
| POST | `/api/admin/send/allreport` | 리포트 알림 전송 (전체 사용자) |
| POST | `/api/admin/send/allcustom` | 커스텀 알림 전송 (전체 사용자) |
| POST | `/api/admin/upload/card-image` | 카드 이미지 업로드 |
| POST | `/api/admin/upload/card-banner` | 카드 배너 이미지 업로드 |

---

## 🗄️ ERD

### 데이터베이스 구조
<img width="2481" height="1294" alt="Image" src="https://github.com/user-attachments/assets/5d90a5c2-b385-4534-9f9a-b6cb974559e4" />

### 주요 테이블

| 테이블명 | 설명 |
|:---|:---|
| `member` | 회원 정보 |
| `card` | 카드 정보 |
| `member_card` | 회원-카드 매핑 |
| `diary` | 소비 일기 |
| `card_history` | 결제 내역 |
| `goal` | 목표 설정 |
| `goal_score` | 목표 점수 |
| `category_member` | 회원-카테고리 매핑 |
| `franchise` | 프랜차이즈 정보 |
| `file` | 파일 정보 |
| `notification` | 알림 정보 |

### 데이터베이스 구성

- **DB1 (wooridoori_db1)**: 우리두리 전용 데이터베이스
- **DB2 (dooribank_db2)**: 두리뱅킹 데이터베이스 (결제 내역 동기화용)

---

## ⚙️ 메인 로직

### 4.1 목표 점수 계산 로직

목표 점수는 **달성도**, **안정성**, **비율**, **지속성** 4가지 요소로 구성됩니다.

#### 달성도 점수 (40점 만점)
- 기본 35점
- 절약률 0~30%: 선형 보너스 (35~40점)
- 목표 초과: 초과율만큼 감점 (0~35점)

#### 안정성 점수 (30점 만점)
- 일별 지출의 표준편차 기반 계산
- 지출 패턴이 안정적일수록 높은 점수

#### 비율 점수 (20점 만점)
- 필수/비필수 지출 비율 기반 계산
- 필수 지출 비율이 적절할수록 높은 점수

#### 지속성 점수 (10점 만점)
- 지난 달 대비 소비 패턴 유지도 기반 계산
- 지속적인 소비 관리 시 높은 점수

### 4.2 Spring Batch를 통한 리포트 생성

| 항목 | 내용 |
|:---|:---|
| **Job** | `calculateGoalScoreJob` |
| **Chunk Size** | 10 |
| **멀티쓰레딩** | TaskExecutor (corePoolSize: 5, maxPoolSize: 10) |
| **실행 주기** | 매달 첫째 날 새벽 2시 |
| **대상** | 최근 3개월 내 로그인한 활성 사용자 |

### 4.3 SSE를 통한 실시간 알림

| 항목 | 내용 |
|:---|:---|
| **연결 방식** | Server-Sent Events (SSE) |
| **알림 타입** | `DIARY`, `REPORT`, `GOAL`, `CUSTOM` |
| **연결 관리** | `ConcurrentHashMap`을 통한 Emitter 관리 |

### 4.4 AI 두리 비서

| 항목 | 내용 |
|:---|:---|
| **모델** | Groq API (Llama 기반) |
| **Embedding** | Ollama (온프레미스) |
| **RAG** | Chroma DB를 통한 벡터 검색 |
| **프롬프트** | 사용자 목표 및 소비 내역 기반 개인화된 답변 생성 |

### 4.5 결제 내역 동기화

| 항목 | 내용 |
|:---|:---|
| **동기화 방식** | 두리뱅킹 → 우리두리 |
| **API** | `POST /api/history/calendar/sync` |
| **트리거** | 두리뱅킹에서 결제 발생 시 |

---

## 🚀 성능 개선

### 5.1 Spring Batch 도입을 통한 리포트 생성 성능 개선

#### 개선 전
- CPU 사용률: 70.9%
- DB 커넥션 풀 고갈
- 데이터 유실 현상 발생
- 실시간 API 응답 지연

#### 개선 후
- CPU 사용률: 57.9% (약 13% 감소)
- 데이터 유실 완전 제거
- 실시간 API 안정성 확보
- 배치 작업과 실시간 API 분리

#### 개선 방법
- Spring Batch를 통한 비동기 처리
- 청크 단위 처리 (Chunk Size: 10)
- 멀티쓰레딩 적용 (TaskExecutor)

### 5.2 Redis 캐싱을 통한 API 응답 속도 개선

#### 개선 내용
- 자주 조회되지만 변경이 적은 데이터 캐싱
- 사용자 토큰 검증 성능 향상
- 인기 카드 정보 조회 속도 개선
- DB 부하 감소

### 5.3 온프레미스 이중화 구조를 통한 고가용성 향상

#### 개선 내용
- **DB 이중화**: Master-Worker 구조로 자동 Failover
- **로드 밸런서 이중화**: 메인/서브 구조로 장애 대응
- **VIP 기반 구조**: 장비 교체 시 설정 변경 없이 안정적인 연결 유지
- **Health Check**: 주기적인 상태 검사 및 자동 전환

---

## 🔧 트러블슈팅

### 6.1 소비 리포트 대량 요청 시 API 서버 쓰레드 풀 고갈

#### 문제 상황
- K6 부하 테스트 시 8,000명의 가상 사용자가 리포트 조회
- 쓰레드 풀 고갈 및 DB 커넥션 풀 고갈 발생
- CPU 사용률 70.9%까지 상승
- 데이터 유실 현상 발생

#### 해결 방법
- Spring Batch 도입하여 리포트 생성 작업을 실시간 API와 분리
- 청크 단위 처리 및 멀티쓰레딩 적용
- JobRepository의 잠금 메커니즘으로 중복 실행 방지

#### 결과
- CPU 사용률 13% 감소
- 데이터 유실 완전 제거
- 실시간 API 안정성 확보

### 6.2 AI 서버 리소스 부족으로 인한 응답 지연

#### 문제 상황
- 온프레미스 Ollama 자체 호스팅 시 응답 시간 10초 이상
- 모델 품질 저하 및 동시 처리 제한 발생

#### 해결 방법
- Groq API 기반 하이브리드 구조로 전환
- Chat 모델은 Groq API 사용
- Embedding 모델은 기존 Ollama 유지
- Spring AI의 OpenAI 호환 인터페이스 활용

#### 결과
- 응답 속도 10초 → 1~2초로 약 80% 개선
- 모델 품질 및 안정성 향상
- 비용 및 운영 부담 감소

### 6.3 AWS CI/CD 환경변수 주입 실패

#### 문제 상황
- SSH 기반 환경변수 주입 방식이 CI/CD 파이프라인과 충돌
- 환경변수가 정상적으로 주입되지 않음

#### 해결 방법
- 도커 이미지에 환경변수를 직접 주입하는 전략으로 변경
- GHCR에서 이미지 버전 관리
- Cosign 이미지 서명 적용하여 보안 강화

#### 결과
- 환경변수 보안과 이미지 무결성 동시 확보
- CI/CD 파이프라인 안정화

---

## 🛠️ 기술 스택

| 분류 | 기술 |
|:---|:---|
| **프레임워크** | <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot"> <img src="https://img.shields.io/badge/Spring_Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Batch"> <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white" alt="Spring Security"> <img src="https://img.shields.io/badge/Spring_AI-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring AI"> |
| **언어** | <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"> |
| **데이터베이스** | <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"> <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis"> |
| **보안** | <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT"> |
| **API 문서화** | <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger"> |
| **AI** | <img src="https://img.shields.io/badge/Groq-000000?style=for-the-badge&logo=groq&logoColor=white" alt="Groq"> <img src="https://img.shields.io/badge/Ollama-000000?style=for-the-badge&logo=ollama&logoColor=white" alt="Ollama"> |
| **빌드 도구** | <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle"> |
| **컨테이너** | <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"> |

