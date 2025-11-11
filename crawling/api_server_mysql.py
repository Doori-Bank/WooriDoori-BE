"""
K-Franchise API 서버 - MySQL 버전
FastAPI + MySQL 연동
"""
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import mysql.connector
from mysql.connector import Error
from contextlib import contextmanager
import os

# FastAPI 앱 생성
app = FastAPI(
    title="K-Franchise API",
    description="한국 프랜차이즈 정보 API - MySQL 버전",
    version="1.0.0"
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# MySQL DB 설정
DB_CONFIG = {
    'host': '192.168.0.143',
    'port': 3306,
    'database': 'wooridoori',
    'user': 'woori',
    'password': 'doori',
    'charset': 'utf8mb4'
}

# Pydantic 모델
class Category(BaseModel):
    id: int
    category_name: str
    category_color: Optional[str] = None
    created_at: Optional[str] = None

class File(BaseModel):
    id: int
    uuid: str
    file_origin_name: str
    file_path: str
    file_type: str
    created_at: Optional[str] = None

class Franchise(BaseModel):
    id: int
    category_id: int
    file_id: int
    fran_name: str
    created_at: Optional[str] = None

class FranchiseDetail(BaseModel):
    id: int
    fran_name: str
    category_id: int
    category_name: str
    file_id: int
    file_path: str
    file_origin_name: str
    created_at: Optional[str] = None

# DB 연결 컨텍스트 매니저
@contextmanager
def get_db_connection():
    """MySQL DB 연결"""
    connection = None
    try:
        connection = mysql.connector.connect(**DB_CONFIG)
        yield connection
    except Error as e:
        print(f"DB 연결 오류: {e}")
        raise HTTPException(status_code=500, detail=f"Database connection error: {str(e)}")
    finally:
        if connection and connection.is_connected():
            connection.close()

# API 엔드포인트

@app.get("/")
async def root():
    """API 루트"""
    return {
        "message": "K-Franchise API - MySQL",
        "version": "1.0.0",
        "endpoints": {
            "docs": "/docs",
            "health": "/api/health",
            "categories": "/api/categories",
            "franchises": "/api/franchises",
            "franchise_by_id": "/api/franchises/{id}",
            "franchises_by_category": "/api/categories/{category_id}/franchises",
            "search": "/api/search?q={query}",
            "stats": "/api/stats"
        }
    }

@app.get("/api/health")
async def health_check():
    """헬스 체크"""
    try:
        with get_db_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT 1")
            cursor.fetchone()
        return {"status": "healthy", "database": "connected", "type": "MySQL"}
    except Exception as e:
        return {"status": "unhealthy", "error": str(e)}

@app.get("/api/categories", response_model=List[Category])
async def get_categories():
    """모든 카테고리 조회"""
    try:
        with get_db_connection() as conn:
            cursor = conn.cursor(dictionary=True)
            cursor.execute("""
                SELECT id, category_name, category_color,
                       DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') as created_at
                FROM tbl_category
                ORDER BY id
            """)
            
            categories = cursor.fetchall()
            return categories
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/franchises", response_model=List[FranchiseDetail])
async def get_franchises(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    category_id: Optional[int] = None
):
    """프랜차이즈 목록 조회"""
    try:
        with get_db_connection() as conn:
            cursor = conn.cursor(dictionary=True)
            
            if category_id:
                query = f"""
                    SELECT f.ID as id, f.FRAN_NAME as fran_name, 
                           f.CATEGORY_ID as category_id, c.CATEGORY_NAME as category_name,
                           f.FILE_ID as file_id, fi.FILE_PATH as file_path, 
                           fi.FILE_ORIGIN_NAME as file_origin_name,
                           DATE_FORMAT(f.CREATED_AT, '%Y-%m-%d %H:%i:%s') as created_at
                    FROM tbl_franchise f
                    JOIN tbl_category c ON f.category_id = c.id
                    JOIN tbl_file fi ON f.file_id = fi.id
                    WHERE f.CATEGORY_ID = {category_id}
                    ORDER BY f.id
                    LIMIT {skip}, {limit}
                """
                cursor.execute(query)
            else:
                query = f"""
                    SELECT f.ID as id, f.FRAN_NAME as fran_name, 
                           f.CATEGORY_ID as category_id, c.CATEGORY_NAME as category_name,
                           f.FILE_ID as file_id, fi.FILE_PATH as file_path, 
                           fi.FILE_ORIGIN_NAME as file_origin_name,
                           DATE_FORMAT(f.CREATED_AT, '%Y-%m-%d %H:%i:%s') as created_at
                    FROM tbl_franchise f
                    JOIN tbl_category c ON f.category_id = c.id
                    JOIN tbl_file fi ON f.file_id = fi.id
                    ORDER BY f.id
                    LIMIT {skip}, {limit}
                """
                cursor.execute(query)
            
            franchises = cursor.fetchall()
            return franchises
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/franchises/{franchise_id}", response_model=FranchiseDetail)
async def get_franchise(franchise_id: int):
    """특정 프랜차이즈 조회"""
    try:
        with get_db_connection() as conn:
            cursor = conn.cursor(dictionary=True)
            query = f"""
                SELECT f.ID as id, f.FRAN_NAME as fran_name, 
                       f.CATEGORY_ID as category_id, c.CATEGORY_NAME as category_name,
                       f.FILE_ID as file_id, fi.FILE_PATH as file_path, 
                       fi.FILE_ORIGIN_NAME as file_origin_name,
                       DATE_FORMAT(f.CREATED_AT, '%Y-%m-%d %H:%i:%s') as created_at
                FROM TBL_FRANCHISE f
                JOIN TBL_CATEGORY c ON f.CATEGORY_ID = c.ID
                JOIN TBL_FILE fi ON f.FILE_ID = fi.ID
                WHERE f.id = {franchise_id}
            """
            cursor.execute(query)
            
            franchise = cursor.fetchone()
            if not franchise:
                raise HTTPException(status_code=404, detail="Franchise not found")
            
            return franchise
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/categories/{category_id}/franchises", response_model=List[FranchiseDetail])
async def get_franchises_by_category(
    category_id: int,
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000)
):
    """카테고리별 프랜차이즈 조회"""
    return await get_franchises(skip=skip, limit=limit, category_id=category_id)

@app.get("/api/search", response_model=List[FranchiseDetail])
async def search_franchises(
    q: str = Query(..., min_length=1),
    category_id: Optional[int] = None,
    limit: int = Query(100, ge=1, le=1000)
):
    """프랜차이즈 검색"""
    try:
        with get_db_connection() as conn:
            cursor = conn.cursor(dictionary=True)
            
            search_term = f'%{q}%'
            
            if category_id:
                query = f"""
                    SELECT f.ID as id, f.FRAN_NAME as fran_name, 
                           f.CATEGORY_ID as category_id, c.CATEGORY_NAME as category_name,
                           f.FILE_ID as file_id, fi.FILE_PATH as file_path, 
                           fi.FILE_ORIGIN_NAME as file_origin_name,
                           DATE_FORMAT(f.CREATED_AT, '%Y-%m-%d %H:%i:%s') as created_at
                    FROM tbl_franchise f
                    JOIN tbl_category c ON f.category_id = c.id
                    JOIN tbl_file fi ON f.file_id = fi.id
                    WHERE f.CATEGORY_ID = {category_id} 
                      AND (f.FRAN_NAME LIKE %s OR c.CATEGORY_NAME LIKE %s)
                    ORDER BY f.FRAN_NAME
                    LIMIT {limit}
                """
                cursor.execute(query, (search_term, search_term))
            else:
                query = f"""
                    SELECT f.ID as id, f.FRAN_NAME as fran_name, 
                           f.CATEGORY_ID as category_id, c.CATEGORY_NAME as category_name,
                           f.FILE_ID as file_id, fi.FILE_PATH as file_path, 
                           fi.FILE_ORIGIN_NAME as file_origin_name,
                           DATE_FORMAT(f.CREATED_AT, '%Y-%m-%d %H:%i:%s') as created_at
                    FROM tbl_franchise f
                    JOIN tbl_category c ON f.category_id = c.id
                    JOIN tbl_file fi ON f.file_id = fi.id
                    WHERE f.FRAN_NAME LIKE %s OR c.CATEGORY_NAME LIKE %s
                    ORDER BY f.FRAN_NAME
                    LIMIT {limit}
                """
                cursor.execute(query, (search_term, search_term))
            
            franchises = cursor.fetchall()
            return franchises
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/stats")
async def get_statistics():
    """통계 정보"""
    try:
        with get_db_connection() as conn:
            cursor = conn.cursor(dictionary=True)
            
            # 전체 통계
            cursor.execute("SELECT COUNT(*) as count FROM tbl_category")
            total_categories = cursor.fetchone()['count']
            
            cursor.execute("SELECT COUNT(*) as count FROM tbl_franchise")
            total_franchises = cursor.fetchone()['count']
            
            cursor.execute("SELECT COUNT(*) as count FROM tbl_file")
            total_files = cursor.fetchone()['count']
            
            # 카테고리별 통계
            cursor.execute("""
                SELECT c.category_name as category_name, COUNT(f.id) as count
                FROM tbl_category c
                LEFT JOIN tbl_franchise f ON c.id = f.category_id
                GROUP BY c.category_name
                ORDER BY count DESC
            """)
            
            category_stats = cursor.fetchall()
            
            return {
                "total_categories": total_categories,
                "total_franchises": total_franchises,
                "total_files": total_files,
                "by_category": category_stats
            }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    print("\n" + "="*60)
    print("  K-Franchise API 서버 시작")
    print("  MySQL 연결")
    print("="*60 + "\n")
    print(f"🌐 서버 주소: http://localhost:8000")
    print(f"📚 API 문서: http://localhost:8000/docs")
    print(f"🔍 Redoc: http://localhost:8000/redoc\n")
    
    uvicorn.run(app, host="0.0.0.0", port=8000)

