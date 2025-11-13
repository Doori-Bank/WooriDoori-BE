"""
최종 CSV를 MySQL로 임포트 (테이블명 소문자)
"""
import csv
import mysql.connector
from mysql.connector import Error

DB_CONFIG = {
    'host': '192.168.0.143',
    'port': 3306,
    'database': 'wooridoori',
    'user': 'woori',
    'password': 'doori',
    'charset': 'utf8mb4'
}

def connect_db():
    """MySQL DB 연결"""
    try:
        connection = mysql.connector.connect(**DB_CONFIG)
        print(f"✅ MySQL DB 연결 성공!")
        print(f"   서버: {DB_CONFIG['host']}:{DB_CONFIG['port']}")
        print(f"   데이터베이스: {DB_CONFIG['database']}\n")
        return connection
    except Error as e:
        print(f"❌ DB 연결 실패: {e}")
        return None

def create_tables(connection):
    """테이블 생성 (소문자)"""
    cursor = connection.cursor()
    
    print("📋 테이블 생성 중...")
    
    # 기존 테이블 삭제 (대문자, 소문자 모두)
    cursor.execute("DROP TABLE IF EXISTS tbl_franchise")
    cursor.execute("DROP TABLE IF EXISTS TBL_FRANCHISE")
    cursor.execute("DROP TABLE IF EXISTS tbl_file")
    cursor.execute("DROP TABLE IF EXISTS TBL_FILE")
    cursor.execute("DROP TABLE IF EXISTS tbl_category")
    cursor.execute("DROP TABLE IF EXISTS TBL_CATEGORY")
    print("  - 기존 테이블 모두 삭제\n")
    
    # tbl_category
    cursor.execute("""
        CREATE TABLE tbl_category (
            id BIGINT PRIMARY KEY,
            category_name VARCHAR(255) NOT NULL UNIQUE,
            category_color VARCHAR(50),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """)
    print("  ✅ tbl_category 생성")
    
    # tbl_file
    cursor.execute("""
        CREATE TABLE tbl_file (
            id BIGINT PRIMARY KEY,
            uuid VARCHAR(255) NOT NULL UNIQUE,
            file_origin_name VARCHAR(255) NOT NULL,
            file_path VARCHAR(500) NOT NULL,
            file_type VARCHAR(50) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """)
    print("  ✅ tbl_file 생성")
    
    # tbl_franchise
    cursor.execute("""
        CREATE TABLE tbl_franchise (
            id BIGINT PRIMARY KEY,
            category_id BIGINT NOT NULL,
            file_id BIGINT NOT NULL,
            fran_name VARCHAR(255) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (category_id) REFERENCES tbl_category(id),
            FOREIGN KEY (file_id) REFERENCES tbl_file(id),
            INDEX idx_category (category_id),
            INDEX idx_file (file_id),
            INDEX idx_name (fran_name)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    """)
    print("  ✅ tbl_franchise 생성\n")
    
    connection.commit()

def import_data(connection, csv_dir="csv_output_final"):
    """CSV 임포트"""
    cursor = connection.cursor()
    
    # 1. 카테고리
    print("📁 카테고리 임포트...")
    with open(f'{csv_dir}/tbl_category.csv', 'r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        for row in reader:
            cursor.execute("""
                INSERT INTO tbl_category (id, category_name, category_color, created_at)
                VALUES (%s, %s, %s, %s)
            """, (int(row['ID']), row['CATEGORY_NAME'], 
                  row['CATEGORY_COLOR'] if row['CATEGORY_COLOR'] else None, row['CREATED_AT']))
    connection.commit()
    cursor.execute("SELECT COUNT(*) FROM tbl_category")
    print(f"  ✅ {cursor.fetchone()[0]}개 완료\n")
    
    # 2. 파일
    print("📁 파일 임포트...")
    with open(f'{csv_dir}/tbl_file.csv', 'r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        batch = []
        count = 0
        for row in reader:
            batch.append((int(row['ID']), row['UUID'], row['FILE_ORIGIN_NAME'], 
                         row['FILE_PATH'], row['FILE_TYPE'], row['CREATED_AT']))
            if len(batch) >= 1000:
                cursor.executemany("""
                    INSERT INTO tbl_file (id, uuid, file_origin_name, file_path, file_type, created_at)
                    VALUES (%s, %s, %s, %s, %s, %s)
                """, batch)
                count += len(batch)
                print(f"  📊 {count}개...")
                batch = []
        
        if batch:
            cursor.executemany("""
                INSERT INTO tbl_file (id, uuid, file_origin_name, file_path, file_type, created_at)
                VALUES (%s, %s, %s, %s, %s, %s)
            """, batch)
            count += len(batch)
    
    connection.commit()
    print(f"  ✅ {count}개 완료\n")
    
    # 3. 프랜차이즈
    print("📁 프랜차이즈 임포트...")
    with open(f'{csv_dir}/tbl_franchise.csv', 'r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        batch = []
        count = 0
        for row in reader:
            batch.append((int(row['ID']), int(row['CATEGORY_ID']), int(row['FILE_ID']), 
                         row['FRAN_NAME'], row['CREATED_AT']))
            if len(batch) >= 1000:
                cursor.executemany("""
                    INSERT INTO tbl_franchise (id, category_id, file_id, fran_name, created_at)
                    VALUES (%s, %s, %s, %s, %s)
                """, batch)
                count += len(batch)
                print(f"  📊 {count}개...")
                batch = []
        
        if batch:
            cursor.executemany("""
                INSERT INTO tbl_franchise (id, category_id, file_id, fran_name, created_at)
                VALUES (%s, %s, %s, %s, %s)
            """, batch)
            count += len(batch)
    
    connection.commit()
    print(f"  ✅ {count}개 완료\n")

def verify(connection):
    """데이터 확인"""
    cursor = connection.cursor()
    
    print("="*60)
    print("📊 최종 데이터베이스 통계")
    print("="*60 + "\n")
    
    cursor.execute("""
        SELECT c.category_name, COUNT(f.id) as count
        FROM tbl_category c
        LEFT JOIN tbl_franchise f ON c.id = f.category_id
        GROUP BY c.category_name
        ORDER BY c.id
    """)
    
    for row in cursor:
        print(f"  {row[0]}: {row[1]}개")
    
    cursor.execute("SELECT COUNT(*) FROM tbl_franchise")
    total = cursor.fetchone()[0]
    
    print(f"\n{'='*60}")
    print(f"  총 프랜차이즈: {total}개")
    print(f"{'='*60}\n")

connection = connect_db()
if connection:
    try:
        create_tables(connection)
        import_data(connection)
        verify(connection)
        print("✅ MySQL 임포트 완료!")
        print("   API 서버 실행: python api_server_mysql.py\n")
    finally:
        connection.close()

