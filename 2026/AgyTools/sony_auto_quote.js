const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

// 物件条件パラメータ（デフォルト値）
const DEFAULT_PARAMS = {
  // 契約状況: 1:他社で加入中(乗換), 2:新規
  contractStatus: '1',
  // 建物種類: 1:一戸建て, 2:マンション
  buildingType: '1',
  // 所在地
  prefecture: '奈良県',
  prefCode: '10000670000029000', // ソニー損保内部の奈良県コード
  postalCode: '6391015',
  address: '大和郡山市箕山町6-27',
  // 建築年月
  builtYear: '2000',
  builtMonth: '09',
  // 構造
  pillarType: '木造',          // radPillarTyp_2
  fireProofPerformance: 'H構造', // 耐火性能なし radFirpofPrfrmn_7 / radOthrConstrn_2
  // 補償開始日
  startDateYear: '2026',
  startDateMonth: '11',
  startDateDay: '21',
  // 金額
  buildingAmount: '1500', // 万円
  goodsAmount: '500',     // 万円
  waterDamage: false,     // 水災除外
  earthquake: true        // 地震保険あり
};

async function runSonyAutoQuote(customParams = {}) {
  const p = { ...DEFAULT_PARAMS, ...customParams };
  console.log('========================================================');
  console.log('  ソニー損保 火災保険 Playwright 自動入力スクリプト');
  console.log('========================================================');
  console.log(`[条件] ${p.prefecture}${p.address} / ${p.builtYear}年${p.builtMonth}月築 / ${p.pillarType}(${p.fireProofPerformance})`);
  console.log(`[補償] 建物: ${p.buildingAmount}万 / 家財: ${p.goodsAmount}万 / 水災: ${p.waterDamage ? 'あり' : 'なし(除外)'}`);

  const browser = await chromium.launch({
    headless: false, // 動作確認のため可視化（不要なら true）
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  const context = await browser.newContext({
    viewport: { width: 1280, height: 950 },
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
  });

  const page = await context.newPage();

  try {
    // 1. ソニー損保 火災保険トップへアクセス
    console.log('\n[Step 1] 火災保険トップページを開いています...');
    await page.goto('https://www.sonysonpo.co.jp/fire/', { waitUntil: 'domcontentloaded', timeout: 30000 });
    await page.waitForTimeout(1500);

    // 2. モーダルが出現した場合の「持ち家」選択
    console.log('[Step 2] 物件区分の選択（持ち家）...');
    const btnOwned = await page.$('a:has-text("持ち家"), button:has-text("持ち家")');
    if (btnOwned) {
      console.log('  -> モーダルの「持ち家」をクリック');
      await btnOwned.click();
      await page.waitForTimeout(2000);
    }

    // 「詳細見積りへ」または「お見積り・お申込み」ボタンをクリック
    const btnDetail = await page.$('a:has-text("詳細見積りへ"), a:has-text("お見積り・お申込み"), a:has-text("ざっくり見積りへ")');
    if (btnDetail) {
      console.log('  -> 見積もりボタンをクリックしてフォームへ進みます');
      await btnDetail.click();
    }

    // 3. 入力画面 (PFR01200) への到達待機
    console.log('\n[Step 3] 建物情報入力フォームへの到達を待機しています...');
    await page.waitForSelector('#contentsform\\:sltBldY, #radBldKbn_1, #radContSts_1', { timeout: 20000 });
    await page.waitForTimeout(1000);

    // 4. パラメータ自動入力
    console.log('\n[Step 4] パラメータを自動入力中...');

    // (1) ご契約の状況: 他社で契約中
    const radCont = await page.$('#radContSts_1');
    if (radCont) {
      console.log('  -> ご契約状況: 他社で契約中');
      await radCont.click();
    }

    // (2) 建物の種類: 一戸建て
    const radBld = await page.$('#radBldKbn_1');
    if (radBld) {
      console.log('  -> 建物種類: 一戸建て');
      await radBld.click();
    }

    // (3) 建築年月
    console.log(`  -> 建築年月: ${p.builtYear}年 ${p.builtMonth}月`);
    await page.selectOption('#contentsform\\:sltBldY', p.builtYear);
    await page.selectOption('#contentsform\\:sltBldM', p.builtMonth);

    // (4) 柱の種類: 木造 (radPillarTyp_2)
    console.log(`  -> 柱の種類: ${p.pillarType}`);
    await page.click('#radPillarTyp_2');

    // (5) 耐火性能: 該当なし / H構造 (radFirpofPrfrmn_7)
    console.log(`  -> 耐火性能: ${p.fireProofPerformance}`);
    await page.click('#radFirpofPrfrmn_7');
    const radH = await page.$('#radOthrConstrn_2');
    if (radH) await radH.click();

    // (6) 所在地 (都道府県)
    const selPref = await page.$('#contentsform\\:sltJpadpref');
    if (selPref) {
      console.log(`  -> 都道府県: ${p.prefecture}`);
      await selPref.selectOption({ label: p.prefecture }).catch(async () => {
        await selPref.selectOption(p.prefCode);
      });
    }

    // (7) 補償開始日
    console.log(`  -> 補償開始日: ${p.startDateYear}年${p.startDateMonth}月${p.startDateDay}日`);
    const selCcY = await page.$('#contentsform\\:sltCcdateY');
    if (selCcY) {
      await selCcY.selectOption(p.startDateYear);
      await page.selectOption('#contentsform\\:sltCcdateM', p.startDateMonth);
      await page.selectOption('#contentsform\\:sltCcdateD', p.startDateDay);
    }

    // (8) 満期日と合わせる等の仮チェック解除
    const chkPrv = await page.$('#chkPrvsnlExstInsStartD');
    if (chkPrv && await chkPrv.isChecked()) {
      await chkPrv.uncheck();
    }

    // 5. スクリーンショットの撮影・保存
    console.log('\n[Step 5] 入力完了エビデンスのスクリーンショットを撮影中...');
    const outDir = 'G:\\マイドライブ\\2026\\火災保険更新\\verification_screenshots';
    if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });
    
    const shotPath = path.join(outDir, 'sony_playwright_autofill.png');
    await page.screenshot({ path: shotPath, fullPage: false });
    console.log(`✓ スクリーンショット保存完了: ${shotPath}`);

    console.log('\n✓ ソニー損保 フォームへの自動入力がすべて完了しました！');
    await page.waitForTimeout(5000);

  } catch (err) {
    console.error('エラー発生:', err);
  } finally {
    await browser.close();
  }
}

// 実行
if (require.main === module) {
  runSonyAutoQuote();
}

module.exports = { runSonyAutoQuote };