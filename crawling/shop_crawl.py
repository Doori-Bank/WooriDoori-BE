"""
최종 카테고리 재분류 크롤러
- 카페: 커피 + 디저트
- 식비: 제과제빵, 한식, 중식, 일식, 양식, 기타외국식, 치킨, 피자, 분식, 패스트푸드
- 술/유흥: 주점, PC방, 여가·오락
- 편의점/마트: 편의점
- 교육: 교육·유아, 스터디카페·독서실
- 쇼핑: 뷰티
- 기타: 도소매, 빨래방, 생활서비스
- 교통/자동차: (빈 카테고리)
- 주거: (빈 카테고리)
- 병원: (빈 카테고리)
"""
import requests
import os
import uuid
import csv
from datetime import datetime
import time

class KFranchiseFinalCrawler:
    def __init__(self, output_dir="csv_output_final"):
        self.base_url = "https://www.k-franchise.or.kr"
        self.api_url = f"{self.base_url}/brand/bprl/list/read"
        self.category_api_url = f"{self.base_url}/brand/bprl/getCategory"
        self.output_dir = output_dir
        
        os.makedirs(self.output_dir, exist_ok=True)
        
        # CSV 파일 경로 (소문자)
        self.category_csv = os.path.join(output_dir, "tbl_category.csv")
        self.file_csv = os.path.join(output_dir, "tbl_file.csv")
        self.franchise_csv = os.path.join(output_dir, "tbl_franchise.csv")
        
        # 데이터 저장
        self.categories = {}
        self.files = {}
        self.franchises = []
        
        # ID 카운터
        self.category_id_counter = 1
        self.file_id_counter = 1
        self.franchise_id_counter = 1
        
        # 세션
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        })
        
        # K-Franchise 대분류 코드
        self.main_categories = {
            "카페·디저트": "TP00000048",
            "음식점·주점": "TP00000052",
            "치킨·피자": "TP00000059",
            "분식·패스트푸드": "TP00000062",
            "판매업": "TP00000065",
            "서비스": "TP00000071"
        }
        
        # 세부 카테고리 → 새 카테고리 매핑
        self.category_mapping = {
            # 카페
            "커피": "카페",
            "디저트": "카페",
            
            # 식비
            "제과제빵": "식비",
            "한식": "식비",
            "중식": "식비",
            "일식": "식비",
            "양식": "식비",
            "기타 외국식": "식비",
            "치킨": "식비",
            "피자": "식비",
            "분식": "식비",
            "패스트푸드": "식비",
            
            # 술/유흥
            "주점": "술/유흥",
            "PC방": "술/유흥",
            "여가·오락": "술/유흥",
            
            # 편의점/마트
            "편의점": "편의점/마트",
            
            # 교육
            "교육·유아": "교육",
            "스터디카페·독서실": "교육",
            
            # 쇼핑
            "뷰티": "쇼핑",
            
            # 기타
            "도소매": "기타",
            "생활서비스": "기타",
            "빨래방": "기타",
            "서비스": "기타",
        }
        
        print(f"📁 출력 디렉토리: {self.output_dir}")
    
    def init_session(self):
        """세션 초기화"""
        try:
            response = self.session.get(f"{self.base_url}/brand/main")
            response.raise_for_status()
            print("✅ 세션 초기화 완료")
            return True
        except Exception as e:
            print(f"❌ 세션 초기화 실패: {e}")
            return False
    
    def get_subcategories(self, main_category_code):
        """세부 카테고리 목록 가져오기"""
        try:
            payload = {"kfaTpindLv1Cd": main_category_code}
            response = self.session.post(self.category_api_url, json=payload, timeout=30)
            data = response.json()
            
            if data.get('retCode') == 'CM0000':
                cat_list = data.get('data', {}).get('list', [])
                return cat_list
            return []
        except:
            return []
    
    def fetch_brand_list(self, main_code, sub_code="", page_num=1):
        """브랜드 목록 가져오기"""
        payload = {
            "kfaTpindLv1Cd": main_code,
            "kfaTpindLv2Cd": sub_code,
            "pageNum": page_num,
            "pagePerRows": 20,
            "pageCount": 10,
            "sortGubun": "1",
            "minCost": "",
            "maxCost": "",
            "minArea": "",
            "maxArea": ""
        }
        
        try:
            response = self.session.post(self.api_url, json=payload, timeout=30)
            data = response.json()
            
            if data.get('retCode') == 'CM0000':
                return data.get('data', {})
            return None
        except:
            return None
    
    def add_file_info(self, image_url, franchise_name):
        """파일 정보 추가"""
        try:
            if not image_url:
                return None
            
            # /resources 경로 추가
            if image_url.startswith('/brnd/'):
                image_url = f"{self.base_url}/resources{image_url}"
            elif image_url.startswith('/'):
                image_url = f"{self.base_url}{image_url}"
            
            file_uuid = str(uuid.uuid4())
            
            safe_name = "".join(c for c in franchise_name if c.isalnum() or c in (' ', '-', '_')).strip()
            if not safe_name:
                safe_name = "franchise"
            
            ext = 'jpg'
            if '.png' in image_url:
                ext = 'png'
            elif '.gif' in image_url:
                ext = 'gif'
            
            file_origin_name = f"{safe_name}.{ext}"
            
            return {
                'uuid': file_uuid,
                'file_origin_name': file_origin_name,
                'file_path': image_url,
                'file_type': f'image/{ext}'
            }
        except:
            return None
    
    def add_category(self, category_name):
        """카테고리 추가"""
        if category_name in self.categories:
            return self.categories[category_name]
        
        category_id = self.category_id_counter
        self.categories[category_name] = category_id
        self.category_id_counter += 1
        return category_id
    
    def add_file(self, file_info):
        """파일 추가"""
        file_uuid = file_info['uuid']
        if file_uuid in self.files:
            return self.files[file_uuid]
        
        file_id = self.file_id_counter
        self.files[file_uuid] = {
            'id': file_id,
            'uuid': file_uuid,
            'file_origin_name': file_info['file_origin_name'],
            'file_path': file_info['file_path'],
            'file_type': file_info['file_type']
        }
        self.file_id_counter += 1
        return file_id
    
    def add_franchise(self, fran_name, category_id, file_id):
        """프랜차이즈 추가"""
        for franchise in self.franchises:
            if franchise['fran_name'] == fran_name and franchise['category_id'] == category_id:
                return False
        
        franchise_id = self.franchise_id_counter
        self.franchises.append({
            'id': franchise_id,
            'category_id': category_id,
            'file_id': file_id,
            'fran_name': fran_name
        })
        self.franchise_id_counter += 1
        return True
    
    def crawl_all(self):
        """모든 카테고리 크롤링 및 재분류"""
        print("\n" + "="*60)
        print("  K-Franchise 최종 크롤러")
        print("  카테고리: 카페, 식비, 술/유흥, 편의점/마트,")
        print("           교육, 쇼핑, 기타, 교통/자동차, 주거, 병원")
        print("="*60 + "\n")
        
        if not self.init_session():
            return
        
        # 새로운 카테고리 미리 생성
        for new_cat in ["카페", "식비", "술/유흥", "편의점/마트", "교육", "쇼핑", "기타", "교통/자동차", "주거", "병원"]:
            self.add_category(new_cat)
        
        total_saved = 0
        
        for main_cat_name, main_cat_code in self.main_categories.items():
            print(f"\n{'='*60}")
            print(f"📁 {main_cat_name} 처리 중")
            print(f"{'='*60}")
            
            # 세부 카테고리 가져오기
            subcategories = self.get_subcategories(main_cat_code)
            
            if subcategories:
                print(f"  세부 카테고리: {len(subcategories)}개 발견")
                for subcat in subcategories:
                    sub_name = subcat.get('kfaTpindLv2', '')
                    sub_code = subcat.get('kfaTpindLv2Cd', '')
                    
                    # 새 카테고리 매핑
                    new_category = self.category_mapping.get(sub_name, "기타")
                    
                    print(f"\n  [{sub_name}] → [{new_category}]")
                    saved_count = self.crawl_subcategory(main_cat_code, sub_code, sub_name, new_category)
                    total_saved += saved_count
            else:
                # 세부 카테고리 없으면 전체 크롤링
                print(f"  세부 카테고리 없음 - 전체 크롤링")
                new_category = "기타"
                saved_count = self.crawl_subcategory(main_cat_code, "", main_cat_name, new_category)
                total_saved += saved_count
        
        # CSV 저장
        self.save_to_csv()
        
        print(f"\n{'='*60}")
        print(f"🎉 크롤링 완료!")
        print(f"📈 총 저장된 프랜차이즈: {total_saved}개")
        print(f"{'='*60}\n")
        
        self.print_stats()
    
    def crawl_subcategory(self, main_code, sub_code, sub_name, new_category):
        """세부 카테고리 크롤링"""
        new_category_id = self.categories[new_category]
        saved_count = 0
        page_num = 1
        
        while True:
            data = self.fetch_brand_list(main_code, sub_code, page_num)
            
            if not data:
                break
            
            brand_list = data.get('list', [])
            paging = data.get('paging', {})
            
            if not brand_list:
                break
            
            if page_num == 1:
                total = paging.get('total', 0)
                print(f"    📊 {total}개 항목 예상")
            
            for brand in brand_list:
                try:
                    fran_name = brand.get('brndNm', '').strip()
                    if not fran_name:
                        continue
                    
                    # 이미지 URL
                    image_url = brand.get('thumbFileAcesUrl', '') or brand.get('logoFileAcesUrl', '')
                    
                    file_info = self.add_file_info(image_url, fran_name)
                    
                    if not file_info:
                        if not hasattr(self, 'default_file_id'):
                            default_file_info = {
                                'uuid': 'default',
                                'file_origin_name': 'default.jpg',
                                'file_path': f"{self.base_url}/images/brand_default.jpg",
                                'file_type': 'image/jpeg'
                            }
                            if 'default' not in self.files:
                                self.default_file_id = self.add_file(default_file_info)
                        file_id = self.default_file_id
                    else:
                        file_id = self.add_file(file_info)
                    
                    if self.add_franchise(fran_name, new_category_id, file_id):
                        saved_count += 1
                    
                    # time.sleep(0.02)  # 속도 향상을 위해 딜레이 제거
                except:
                    continue
            
            total_page = paging.get('totalPage', 1)
            if page_num >= total_page:
                break
            
            page_num += 1
            time.sleep(0.1)  # 속도 향상
        
        if saved_count > 0:
            print(f"    ✅ {saved_count}개 저장")
        
        return saved_count
    
    def save_to_csv(self):
        """CSV 저장"""
        print(f"\n{'='*60}")
        print("💾 CSV 파일 저장 중...")
        print(f"{'='*60}\n")
        
        # 카테고리
        with open(self.category_csv, 'w', newline='', encoding='utf-8-sig') as f:
            writer = csv.writer(f)
            writer.writerow(['ID', 'CATEGORY_NAME', 'CATEGORY_COLOR', 'CREATED_AT'])
            for category_name, category_id in sorted(self.categories.items(), key=lambda x: x[1]):
                created_at = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                writer.writerow([category_id, category_name, '', created_at])
        print(f"✅ {self.category_csv} - {len(self.categories)}개")
        
        # 파일
        with open(self.file_csv, 'w', newline='', encoding='utf-8-sig') as f:
            writer = csv.writer(f)
            writer.writerow(['ID', 'UUID', 'FILE_ORIGIN_NAME', 'FILE_PATH', 'FILE_TYPE', 'CREATED_AT'])
            for file_info in sorted(self.files.values(), key=lambda x: x['id']):
                created_at = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                writer.writerow([
                    file_info['id'],
                    file_info['uuid'],
                    file_info['file_origin_name'],
                    file_info['file_path'],
                    file_info['file_type'],
                    created_at
                ])
        print(f"✅ {self.file_csv} - {len(self.files)}개")
        
        # 프랜차이즈
        with open(self.franchise_csv, 'w', newline='', encoding='utf-8-sig') as f:
            writer = csv.writer(f)
            writer.writerow(['ID', 'CATEGORY_ID', 'FILE_ID', 'FRAN_NAME', 'CREATED_AT'])
            for franchise in sorted(self.franchises, key=lambda x: x['id']):
                created_at = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                writer.writerow([
                    franchise['id'],
                    franchise['category_id'],
                    franchise['file_id'],
                    franchise['fran_name'],
                    created_at
                ])
        print(f"✅ {self.franchise_csv} - {len(self.franchises)}개\n")
    
    def print_stats(self):
        """통계 출력"""
        print("📊 카테고리별 통계:")
        print("="*60)
        
        category_stats = {}
        for franchise in self.franchises:
            cat_id = franchise['category_id']
            category_stats[cat_id] = category_stats.get(cat_id, 0) + 1
        
        id_to_name = {v: k for k, v in self.categories.items()}
        for cat_id in sorted(category_stats.keys()):
            cat_name = id_to_name.get(cat_id, f"ID:{cat_id}")
            count = category_stats[cat_id]
            print(f"  {cat_name}: {count}개")
        
        print(f"\n{'='*60}")
        print(f"  총 카테고리: {len(self.categories)}개")
        print(f"  총 프랜차이즈: {len(self.franchises)}개")
        print(f"  총 파일: {len(self.files)}개")
        print(f"{'='*60}\n")

def main():
    crawler = KFranchiseFinalCrawler()
    
    try:
        crawler.crawl_all()
        
        print("📝 다음 단계:")
        print("="*60)
        print("1. python import_to_mysql_final.py")
        print("2. python api_server_mysql.py 실행")
        print("="*60 + "\n")
        
    except KeyboardInterrupt:
        print("\n\n⚠️  중단되었습니다")
        crawler.save_to_csv()
    except Exception as e:
        print(f"\n❌ 오류: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()

