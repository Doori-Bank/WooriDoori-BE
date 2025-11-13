# -*- coding: utf-8 -*-
import re, requests, csv, time, itertools, uuid, os
from pathlib import Path
from bs4 import BeautifulSoup  # pip install beautifulsoup4

BASE_URL = "https://api.card-gorilla.com:8080/v1/cards/search"
DETAIL_URL = "https://www.card-gorilla.com/card/detail/{idx}"
CORP_ID   = 5   # 우리카드
PER_PAGE  = 30

SAVE_DIR = Path("/Users/hongttochi/crawling/cards")
SAVE_DIR.mkdir(parents=True, exist_ok=True)
CSV_MAIN = SAVE_DIR / "woori_cards.csv"   # idx, card_name, kor_fee, for_fee, img_file_id, benefits
CSV_FILE = SAVE_DIR / "card_file.csv"     # id, UUID, file_origin_name, file_path, file_type

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                  "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept": "application/json,text/html;q=0.9,*/*;q=0.8",
    "Accept-Language": "ko,en;q=0.8",
    "Connection": "keep-alive",
    "Referer": "https://www.card-gorilla.com/",
}

def fetch_page(p: int):
    r = requests.get(
        BASE_URL,
        headers=HEADERS,
        params={"p": p, "perPage": PER_PAGE, "corp": CORP_ID},
        timeout=12,
    )
    r.raise_for_status()
    return r.json()

def clean_fee_cell(text: str) -> str:
    if not text:
        return ""
    t = text.strip().replace("[", "").replace("]", "")
    t = t.replace("원", "").strip()
    t = re.sub(r"\s+", " ", t)
    return t

def split_annual_fee(annual_fee_basic: str):
    """국내/해외 최대 두 개만"""
    if not annual_fee_basic:
        return ("", "")
    parts = [clean_fee_cell(p) for p in annual_fee_basic.split("/") if p.strip()]
    parts += [""] * (2 - len(parts))
    return tuple(parts[:2])

def fetch_benefits_from_detail(idx: int) -> str:
    url = DETAIL_URL.format(idx=idx)
    r = requests.get(url, headers=HEADERS, timeout=12)
    r.raise_for_status()
    soup = BeautifulSoup(r.text, "html.parser")

    sale = soup.select_one("div.sale")
    if not sale:
        return ""

    benefits = []
    for p in sale.select("p"):
        cat = p.select_one("i.store")
        num = p.select_one("span.num")
        cat_txt = (cat.get_text(strip=True) if cat else "").strip()
        num_txt = (num.get_text(" ", strip=True) if num else "").strip()
        num_txt = re.sub(r"\s+", " ", num_txt)
        if cat_txt and num_txt:
            benefits.append(f"{cat_txt} {num_txt}")
    return " | ".join(benefits)

def fallback_benefits_from_top(tb_list) -> str:
    parts = []
    for t in (tb_list or []):
        title = (t.get("title") or "").strip()
        tags  = " ".join(t.get("tags", [])).strip()
        if title and tags:
            parts.append(f"{title} {tags}")
        elif title:
            parts.append(title)
    return " | ".join(parts)

def guess_file_type(url: str) -> str:
    ext = os.path.splitext(url.split("?")[0])[1].lower()
    if ext in [".png"]:
        return "image/png"
    if ext in [".jpg", ".jpeg"]:
        return "image/jpeg"
    if ext in [".webp"]:
        return "image/webp"
    return "image/png"

if __name__ == "__main__":
    print("🚀 우리카드 수집 + 파일 메타 생성")

    seen_ids = set()
    saved_idx = 0
    file_id = 2233   # ← card_file.csv의 시작 ID (요청: 2233부터)

    with CSV_MAIN.open("w", encoding="utf-8-sig", newline="") as f_main, \
         CSV_FILE.open("w", encoding="utf-8-sig", newline="") as f_file:

        # 기본 writer: 콤마 포함 필드는 자동 인용 → 수치 "12,000" 안전 저장
        w_main = csv.writer(f_main)
        w_file = csv.writer(f_file)

        # 헤더
        w_main.writerow(["idx", "card_name", "kor_fee", "for_fee", "img_file_id", "benefits"])
        w_file.writerow(["id", "UUID", "file_origin_name", "file_path", "file_type"])

        for p in itertools.count(1):
            js = fetch_page(p)
            items = js.get("data") or []
            print(f"  📦 page {p} | {len(items)} items")

            if not items:
                print("  ⛳ 더 이상 항목 없음. 종료")
                break

            for it in items:
                try:
                    idx     = it.get("idx")
                    name    = it.get("name", "")
                    img_url = (it.get("card_img") or {}).get("url", "")

                    # 중복 제거
                    dedup_id = idx if idx is not None else f"{name}|{img_url}"
                    if dedup_id in seen_ids:
                        continue
                    seen_ids.add(dedup_id)

                    kor_fee, for_fee = split_annual_fee(it.get("annual_fee_basic", ""))

                    # 혜택
                    benefits = ""
                    if idx:
                        try:
                            benefits = fetch_benefits_from_detail(idx)
                        except Exception:
                            benefits = ""
                    if not benefits:
                        benefits = fallback_benefits_from_top(it.get("top_benefit", []))

                    # ➜ benefits에서 큰따옴표와 쉼표 제거하여 인용 없이 저장되도록
                    if benefits:
                        benefits = benefits.replace('"', '').replace(',', '')

                    # card_file.csv 한 줄 생성 (이미지 있으면)
                    img_fk = ""
                    if img_url:
                        file_uuid = str(uuid.uuid4())
                        file_type = guess_file_type(img_url)
                        ext = os.path.splitext(img_url.split("?")[0])[1] or ".png"
                        file_origin_name = f"card_{file_id}{ext}"
                        w_file.writerow([file_id, file_uuid, file_origin_name, img_url, file_type])
                        img_fk = file_id
                        file_id += 1

                    # woori_cards.csv 저장 (이미지는 file id 참조)
                    w_main.writerow([saved_idx, name, kor_fee, for_fee, img_fk, benefits])
                    saved_idx += 1

                except Exception as e:
                    print(f"    ⚠️ idx={it.get('idx')} 오류: {e}")
                    continue

            if len(items) < PER_PAGE:
                print("  ✅ 마지막 페이지로 판단. 종료")
                break

            time.sleep(0.2)

    print(f"\n🎉 카드 {saved_idx}개 저장 완료")
    print(f"📂 woori_cards.csv → {CSV_MAIN}")
    print(f"📂 card_file.csv  → {CSV_FILE}")
