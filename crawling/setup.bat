@echo off
echo 🚀 K-Franchise 크롤러 설치를 시작합니다...

REM 가상환경 생성
echo 📦 가상환경 생성 중...
python -m venv venv

REM 가상환경 활성화
echo ✅ 가상환경 활성화...
call venv\Scripts\activate.bat

REM pip 업그레이드
echo ⬆️  pip 업그레이드...
python -m pip install --upgrade pip

REM 필요한 패키지 설치
echo 📚 필요한 패키지 설치 중...
pip install -r requirements.txt

echo.
echo ✅ 설치 완료!
echo.
echo 📝 다음 단계:
echo 1. MySQL에서 데이터베이스를 생성하세요:
echo    CREATE DATABASE franchise_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
echo.
echo 2. crolling.py 파일의 db_config를 수정하세요
echo.
echo 3. 가상환경을 활성화하고 실행하세요:
echo    venv\Scripts\activate
echo    python crolling.py
echo.
pause

