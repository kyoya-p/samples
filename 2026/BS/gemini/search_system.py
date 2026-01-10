import asyncio
import sys
import argparse
import re
import os
from urllib.parse import urljoin
from playwright.async_api import async_playwright
from bs4 import BeautifulSoup

SEARCH_URL = "https://batspi.com/index.php?%E3%82%AB%E3%83%BC%E3%83%89%E6%A4%9C%E7%B4%A2"
BASE_URL = "https://batspi.com/"

async def scrape_wiki_search(keito_name, effect_text=None):
    async with async_playwright() as p:
        print(f"[1/6] Launching browser...")
        browser = await p.chromium.launch(headless=False)
        page = await browser.new_page()
        
        print(f"[2/6] Accessing search page: {SEARCH_URL}")
        await page.goto(SEARCH_URL)
        
        if keito_name:
            print(f"[3/6] Filling system: {keito_name}")
            await page.fill('#KEITO_NAME', keito_name)
        
        if effect_text:
            print(f"[3/6] Filling effect: {effect_text}")
            await page.fill('#KOUKA', effect_text)

        print(f"[4/6] Submitting search form...")
        await page.click('input[name="Search"]')
        
        # Wait for search results
        print(f"[5/6] Waiting for results page to load...")
        try:
            await page.wait_for_selector('#body table', timeout=10000)
        except Exception:
            print("(!) No results found or timeout occurred.")
            await browser.close()
            return []
        
        print(f"[6/6] Parsing results...")
        content = await page.content()
        soup = BeautifulSoup(content, 'html.parser')
        
        cards = []
        body = soup.find('div', id='body')
        if body:
            tables = body.find_all('table')
            print(f"    - Found {len(tables)} potential card tables. Analyzing...")
            for table in tables:
                text = table.get_text()
                if 'カード番号' in text and 'カード名' in text:
                    links = table.find_all('a', href=True)
                    card_id = ""
                    card_name = ""
                    card_url = ""
                    
                    id_match = re.search(r'([A-Z\d]{2,}-\d+[A-Z\d]*)', text)
                    if id_match:
                        card_id = id_match.group(1)
                    
                    for a in links:
                        href = a['href']
                        if 'index.php?' in href and not any(x in href for x in ['cmd=', 'related=', 'Search']):
                            card_url = urljoin(BASE_URL, href)
                            card_name = a.get_text(strip=True)
                            if card_name and card_id:
                                break
                    
                    if card_id and card_name:
                        cards.append({"id": card_id, "name": card_name, "url": card_url})

        unique_cards = {c['id']: c for c in cards}.values()
        print(f"    - Successfully extracted {len(unique_cards)} unique cards.")
        
        print(f"    - Closing page...")
        try:
            await asyncio.wait_for(page.close(), timeout=2.0)
        except Exception:
            print("    - Page close timed out or failed.")

        print(f"    - Closing browser...")
        try:
            await asyncio.wait_for(browser.close(), timeout=3.0)
        except Exception:
            print("    - Browser close timed out or failed.")
            
        print(f"Done.")
        return list(unique_cards)

async def main():
    parser = argparse.ArgumentParser(description='バトスピWikiからカードを検索します。')
    parser.add_argument('query', nargs='?', help='検索する系統名 (簡易指定)')
    parser.add_argument('--system', '-s', help='検索する系統名')
    parser.add_argument('--effect', '-e', help='検索する効果テキスト')
    args = parser.parse_args()

    keito = args.system if args.system else args.query
    effect = args.effect

    # デフォルト動作: 何も指定がなければ「超星」を検索
    if not keito and not effect:
        keito = '超星'

    results = await scrape_wiki_search(keito, effect)
    
    search_terms = []
    if keito: search_terms.append(f"System: {keito}")
    if effect: search_terms.append(f"Effect: {effect}")
    
    print(f"\nFound {len(results)} cards for {', '.join(search_terms)}:\n")
    for card in results:
        print(f"{card['id']}: {card['name']} ({card['url']})")
    
    print("Program finished.")
    sys.stdout.flush()
    os._exit(0)



if __name__ == "__main__":
    
    
    asyncio.run(main())