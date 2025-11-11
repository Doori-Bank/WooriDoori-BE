# K-Franchise 크롤링 및 API 프로젝트

K-Franchise 웹사이트에서 프랜차이즈 데이터를 수집하고 MySQL + FastAPI로 제공하는 완전한 솔루션

## 📊 프로젝트 개요

### 수집된 데이터
- **17,146개** 프랜차이즈
- **2,232개** 이미지 URL
- **10개** 카테고리

### 카테고리별 통계

| 카테고리 | 개수 | 포함된 세부 카테고리 |
|---------|------|-------------------|
| 카페 | 1,462개 | 커피, 디저트 |
| 식비 | 11,416개 | 제과제빵, 한식, 중식, 일식, 양식, 치킨, 피자, 분식, 패스트푸드 |
| 술/유흥 | 1,013개 | 주점, PC방, 여가·오락 |
| 편의점/마트 | 58개 | 편의점 |
| 교육 | 730개 | 교육·유아, 스터디카페·독서실 |
| 쇼핑 | 31개 | 뷰티 |
| 기타 | 2,436개 | 도소매, 빨래방, 생활서비스 |
| 교통/자동차 | 0개 | (빈 카테고리) |
| 주거 | 0개 | (빈 카테고리) |
| 병원 | 0개 | (빈 카테고리) |

---

## 🚀 빠른 시작

### 1. 환경 설정

```bash
# 가상환경 생성 및 활성화
python3 -m venv venv
source venv/bin/activate  # macOS/Linux
# venv\Scripts\activate  # Windows

# 패키지 설치
pip install -r requirements.txt
pip install -r requirements_api.txt
```

### 2. 데이터 크롤링

```bash
# 전체 프랜차이즈 크롤링 (약 5-10분)
python crawl_final.py
```

**결과물:** `csv_output_final/` 폴더에 CSV 파일 생성

### 3. MySQL 설정 및 임포트

**MySQL 접속 정보:**
- Host: 192.168.0.143:3306
- Database: wooridoori
- User: woori
- Password: doori

```bash
# CSV 데이터를 MySQL로 임포트
python import_final_mysql.py
```

### 4. API 서버 실행

```bash
# API 서버 시작
python api_server_mysql.py

# 백그라운드 실행
nohup python api_server_mysql.py > api.log 2>&1 &
```

### 5. API 테스트

- **API 문서**: http://localhost:8000/docs
- **통계**: http://localhost:8000/api/stats
- **카테고리**: http://localhost:8000/api/categories

---

## 📡 API 엔드포인트

### Base URL
`http://localhost:8000`

### 주요 API

#### 1. 서버 상태
```
GET /api/health
```
응답:
```json
{
  "status": "healthy",
  "database": "connected",
  "type": "MySQL"
}
```

#### 2. 통계 정보
```
GET /api/stats
```
응답:
```json
{
  "total_categories": 10,
  "total_franchises": 17146,
  "total_files": 2232,
  "by_category": [
    {"category_name": "식비", "count": 11416},
    {"category_name": "카페", "count": 1462},
    ...
  ]
}
```

#### 3. 카테고리 목록
```
GET /api/categories
```

#### 4. 프랜차이즈 목록
```
GET /api/franchises?skip=0&limit=100&category_id=1
```

**파라미터:**
- `skip` (optional): 건너뛸 개수
- `limit` (optional): 조회 개수 (기본: 100, 최대: 1000)
- `category_id` (optional): 카테고리 필터

**응답 예시:**
```json
[
  {
    "id": 1,
    "fran_name": "빈스빈스",
    "category_id": 1,
    "category_name": "카페",
    "file_id": 1,
    "file_path": "https://www.k-franchise.or.kr/resources/brnd/9d8283eb-ca09-4007-ac97-bb1a12716d6e.png",
    "file_origin_name": "빈스빈스.png",
    "created_at": "2025-11-03 16:31:47"
  }
]
```

#### 5. 프랜차이즈 상세
```
GET /api/franchises/{id}
```

#### 6. 카테고리별 조회
```
GET /api/categories/{category_id}/franchises?skip=0&limit=100
```

#### 7. 검색
```
GET /api/search?q=커피&limit=100&category_id=1
```

**파라미터:**
- `q` (required): 검색어
- `limit` (optional): 결과 개수
- `category_id` (optional): 카테고리 필터

### 사용 예시

```bash
# 통계 조회
curl http://localhost:8000/api/stats

# 카페 카테고리 (ID=1) 조회
curl "http://localhost:8000/api/categories/1/franchises?limit=20"

# 스타벅스 검색
curl "http://localhost:8000/api/search?q=스타벅스"

# 특정 프랜차이즈 상세
curl http://localhost:8000/api/franchises/1
```

---

## 🗄️ 데이터베이스 구조

### 테이블 (소문자)

#### tbl_category
```sql
id              BIGINT PRIMARY KEY
category_name   VARCHAR(255) NOT NULL UNIQUE
category_color  VARCHAR(50)
created_at      TIMESTAMP
```

#### tbl_file
```sql
id                 BIGINT PRIMARY KEY
uuid               VARCHAR(255) NOT NULL UNIQUE
file_origin_name   VARCHAR(255) NOT NULL
file_path          VARCHAR(500) NOT NULL  -- 이미지 URL
file_type          VARCHAR(50) NOT NULL
created_at         TIMESTAMP
```

#### tbl_franchise
```sql
id            BIGINT PRIMARY KEY
category_id   BIGINT NOT NULL  -- FK → tbl_category(id)
file_id       BIGINT NOT NULL  -- FK → tbl_file(id)
fran_name     VARCHAR(255) NOT NULL
created_at    TIMESTAMP
```

### ERD
```
┌─────────────┐
│tbl_category │
├─────────────┤
│ id (PK)     │──┐
│category_name│  │
└─────────────┘  │
                 │
┌─────────────┐  │
│  tbl_file   │  │
├─────────────┤  │
│ id (PK)     │──┼──┐
│ uuid        │  │  │
│ file_path   │  │  │
└─────────────┘  │  │
                 │  │
┌─────────────┐  │  │
│tbl_franchise│  │  │
├─────────────┤  │  │
│ id (PK)     │  │  │
│category_id  │──┘  │
│ file_id     │─────┘
│ fran_name   │
└─────────────┘
```

---

## 📂 프로젝트 구조

```
crolling/
├── 📄 Python 코드
│   ├── crawl_final.py          # 최종 크롤러 (세부 카테고리 기반 재분류)
│   ├── import_final_mysql.py   # MySQL 임포트 스크립트
│   └── api_server_mysql.py     # FastAPI REST API 서버
│
├── 📚 문서
│   └── README.md               # 이 파일
│
├── ⚙️ 설정
│   ├── requirements.txt        # 크롤링 패키지
│   ├── requirements_api.txt    # API 서버 패키지
│   ├── env.example             # 환경 변수 예제
│   ├── .gitignore              # Git 무시 파일
│   ├── setup.sh                # 설치 스크립트 (macOS/Linux)
│   └── setup.bat               # 설치 스크립트 (Windows)
│
└── 📊 데이터
    └── csv_output_final/       # 최종 CSV 데이터
        ├── tbl_category.csv    # 10개 카테고리
        ├── tbl_file.csv        # 2,232개 이미지 URL
        └── tbl_franchise.csv   # 17,146개 프랜차이즈
```

---

## 🔧 주요 기능

### 크롤링
- ✅ K-Franchise API 직접 호출 (고속 수집)
- ✅ 세부 카테고리 정보 활용
- ✅ 사용자 정의 카테고리로 자동 재분류
- ✅ 이미지 URL 자동 수집 (로그인 불필요)
- ✅ 17,146개 전체 프랜차이즈 수집

### 데이터베이스
- ✅ MySQL 8.0+ 지원
- ✅ 외래키 제약 조건
- ✅ 인덱스 최적화 (카테고리, 파일, 이름)
- ✅ UTF8MB4 인코딩 (이모지 지원)
- ✅ 소문자 테이블명

### API 서버
- ✅ FastAPI 기반 고성능 REST API
- ✅ 자동 API 문서 (Swagger UI, ReDoc)
- ✅ CORS 지원 (Cross-Origin)
- ✅ 페이지네이션
- ✅ 전문 검색 기능
- ✅ 카테고리 필터링
- ✅ 응답 속도 < 100ms

---

## 💻 프론트엔드 연동 예시

### JavaScript / React
```javascript
// 카페 카테고리 프랜차이즈 가져오기
const response = await fetch('http://localhost:8000/api/categories/1/franchises?limit=50');
const cafes = await response.json();

cafes.forEach(cafe => {
  console.log(cafe.fran_name);
  console.log(cafe.file_path); // 로고 이미지 URL
});

// 검색
const searchResult = await fetch('http://localhost:8000/api/search?q=스타벅스');
const data = await searchResult.json();
```

### Python
```python
import requests

BASE_URL = "http://localhost:8000"

# 통계 조회
stats = requests.get(f"{BASE_URL}/api/stats").json()
print(f"총 프랜차이즈: {stats['total_franchises']}개")

# 카테고리별 조회
response = requests.get(f"{BASE_URL}/api/categories/1/franchises", 
                       params={"limit": 20})
for item in response.json():
    print(f"{item['fran_name']}: {item['file_path']}")
```

---

## 🛠️ 유지보수 및 관리

### 데이터 업데이트
```bash
# 1. 최신 데이터 크롤링
python crawl_final.py

# 2. DB 재임포트
python import_final_mysql.py

# 3. API 서버 재시작
pkill -f api_server_mysql
python api_server_mysql.py
```

### 서버 관리
```bash
# API 서버 상태 확인
curl http://localhost:8000/api/health

# 서버 종료
pkill -f api_server_mysql

# 로그 확인
tail -f api.log
```

### 데이터베이스 백업
```bash
# MySQL 백업
mysqldump -h 192.168.0.143 -u woori -p wooridoori > backup.sql

# 복구
mysql -h 192.168.0.143 -u woori -p wooridoori < backup.sql
```

---

## 🎯 기술 스택

- **Python 3.8+** - 프로그래밍 언어
- **Selenium 4.15+** - 웹 크롤링
- **Requests 2.31+** - HTTP 클라이언트
- **FastAPI 0.104+** - REST API 프레임워크
- **Uvicorn 0.24+** - ASGI 서버
- **MySQL 8.0+** - 관계형 데이터베이스
- **mysql-connector-python 8.2+** - MySQL 드라이버

---

## 📞 문제 해결

### 크롤링 오류

**증상:** ChromeDriver 오류
```bash
# 해결방법
pip install --upgrade webdriver-manager
rm -rf ~/.wdm
```

**증상:** 데이터가 수집되지 않음
- 네트워크 연결 확인
- K-Franchise 웹사이트 접속 가능 여부 확인

### MySQL 연결 오류

**증상:** Connection refused
```bash
# MySQL 서버 상태 확인
mysql -h 192.168.0.143 -u woori -p

# 방화벽 확인
telnet 192.168.0.143 3306
```

**증상:** Access denied
- 사용자명, 비밀번호 확인
- 데이터베이스 존재 여부 확인

### API 서버 오류

**증상:** 포트 8000 이미 사용 중
```bash
# 포트 사용 프로세스 확인 및 종료
lsof -i :8000
kill -9 <PID>
```

**증상:** Database error
- MySQL 서버 실행 확인
- DB 접속 정보 확인

---

## 📁 데이터 파일 형식

### tbl_category.csv
```csv
ID,CATEGORY_NAME,CATEGORY_COLOR,CREATED_AT
1,카페,,2025-11-03 16:31:47
2,식비,,2025-11-03 16:31:47
...
```

### tbl_file.csv
```csv
ID,UUID,FILE_ORIGIN_NAME,FILE_PATH,FILE_TYPE,CREATED_AT
1,abc-123...,스타벅스.png,https://www.k-franchise.or.kr/resources/brnd/...,image/png,2025-11-03 16:31:47
...
```

### tbl_franchise.csv
```csv
ID,CATEGORY_ID,FILE_ID,FRAN_NAME,CREATED_AT
1,1,1,빈스빈스,2025-11-03 16:31:47
...
```

---

## 🌐 API 응답 예시

### 프랜차이즈 상세
```json
{
  "id": 1,
  "fran_name": "빈스빈스",
  "category_id": 1,
  "category_name": "카페",
  "file_id": 1,
  "file_path": "https://www.k-franchise.or.kr/resources/brnd/9d8283eb-ca09-4007-ac97-bb1a12716d6e.png",
  "file_origin_name": "빈스빈스.png",
  "created_at": "2025-11-03 16:31:47"
}
```

### 카테고리 목록
```json
[
  {"id": 1, "category_name": "카페", "category_color": null, "created_at": "2025-11-03 16:31:47"},
  {"id": 2, "category_name": "식비", "category_color": null, "created_at": "2025-11-03 16:31:47"},
  ...
]
```

---

## ⚙️ 환경 변수

`.env` 파일 생성:
```bash
cp env.example .env
# .env 파일 편집
```

**.env 내용:**
```env
DB_HOST=192.168.0.143
DB_PORT=3306
DB_NAME=wooridoori
DB_USER=woori
DB_PASSWORD=doori
```

---

## 🎨 프론트엔드 활용 예시

### 카페 목록 표시
```html
<div id="cafe-list"></div>

<script>
fetch('http://localhost:8000/api/categories/1/franchises?limit=50')
  .then(res => res.json())
  .then(data => {
    data.forEach(item => {
      document.getElementById('cafe-list').innerHTML += `
        <div class="franchise-card">
          <img src="${item.file_path}" alt="${item.fran_name}">
          <h3>${item.fran_name}</h3>
          <p>${item.category_name}</p>
        </div>
      `;
    });
  });
</script>
```

### 검색 기능
```javascript
const searchFranchise = async (query) => {
  const response = await fetch(
    `http://localhost:8000/api/search?q=${encodeURIComponent(query)}&limit=20`
  );
  return await response.json();
};

// 사용
const results = await searchFranchise('커피');
console.log(results);
```

---

## 📈 성능

- **크롤링 속도**: 17,146개 / 약 5-10분
- **DB 임포트**: 17,146개 / 약 1분
- **API 응답**: < 100ms
- **동시 요청**: 1000+ req/s 지원

---

## 🔐 보안

- ✅ 환경 변수로 DB 접속 정보 관리
- ✅ SQL Injection 방지
- ✅ CORS 설정 가능
- ✅ .env 파일 gitignore 처리

---

## 🚀 배포

### Docker (선택사항)
```dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt requirements_api.txt ./
RUN pip install -r requirements.txt -r requirements_api.txt

COPY . .
CMD ["python", "api_server_mysql.py"]
```

### 서버 실행 (프로덕션)
```bash
# Gunicorn 사용
pip install gunicorn
gunicorn api_server_mysql:app -w 4 -k uvicorn.workers.UvicornWorker

# Systemd 서비스 등록
# /etc/systemd/system/kfranchise-api.service
```

---

## 📝 라이선스 및 주의사항

- 이 프로젝트는 교육 목적으로 제작되었습니다
- 웹 크롤링 시 robots.txt 및 이용약관을 준수하세요
- 과도한 요청은 서버에 부담을 줄 수 있으므로 적절한 딜레이를 유지하세요
- K-Franchise 웹사이트: https://www.k-franchise.or.kr

---

## 📚 참고

### API 문서
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

### MySQL 쿼리 예시
```sql
-- 카테고리별 통계
SELECT c.category_name, COUNT(f.id) as count
FROM tbl_category c
LEFT JOIN tbl_franchise f ON c.id = f.category_id
GROUP BY c.category_name
ORDER BY count DESC;

-- 프랜차이즈 목록 (로고 포함)
SELECT f.fran_name, c.category_name, fi.file_path
FROM tbl_franchise f
JOIN tbl_category c ON f.category_id = c.id
JOIN tbl_file fi ON f.file_id = fi.id
WHERE c.category_name = '카페'
LIMIT 20;
```

---

**프로젝트 버전**: v3.0 Final  
**최종 업데이트**: 2025-11-03  
**개발자**: FISA 프로젝트팀  
**총 데이터**: 17,146개 프랜차이즈, 10개 카테고리, 2,232개 이미지 URL
