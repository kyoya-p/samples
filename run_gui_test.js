const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

(async () => {
  console.log('=== Starting Battle Spirits GUI Automation Test ===');
  const screenshotDir = path.join(__dirname, 'gui_test_artifacts');
  if (!fs.existsSync(screenshotDir)) fs.mkdirSync(screenshotDir, { recursive: true });

  const executablePath = fs.existsSync('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe')
    ? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
    : 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';

  console.log(`Using browser binary: ${executablePath}`);

  const browser = await puppeteer.launch({
    executablePath,
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1400, height: 900 });

  page.on('console', msg => console.log('[BROWSER CONSOLE]', msg.text()));
  page.on('pageerror', err => console.error('[BROWSER ERROR]', err));
  page.on('dialog', async dialog => {
    console.log('[BROWSER DIALOG]', dialog.type(), dialog.message());
    await dialog.dismiss();
  });

  const testReport = [];
  function check(name, pass, msg = '') {
    const item = { name, pass, msg };
    testReport.push(item);
    console.log(`[${pass ? 'PASS' : 'FAIL'}] ${name} ${msg ? '- ' + msg : ''}`);
  }

  try {
    const sid = 'test_gui_' + Date.now().toString(36);
    const url = `http://localhost:8090/?sid=${sid}`;
    console.log(`Opening URL: ${url}`);
    await page.goto(url, { waitUntil: 'domcontentloaded' });
    await new Promise(r => setTimeout(r, 1000));

    check('1. ページ読み込み', (await page.title()).includes('Playmat'));
    await page.screenshot({ path: path.join(screenshotDir, '01_initial_load.png') });

    // 1. 新規ゲーム開始
    console.log('Clicking #btn-new...');
    await page.click('#btn-new');
    await new Promise(r => setTimeout(r, 2000));
    await page.screenshot({ path: path.join(screenshotDir, '02_new_game_started.png') });

    const phase = await page.$eval('#phase', el => el.textContent.trim());
    check('2. メインステップ表示', phase.includes('メインステップ'), `Phase: ${phase}`);

    const p1Life = await page.$eval('#p1-life', el => el.textContent.trim());
    check('3. P1 ライフ表示', p1Life.includes('🔷') || p1Life.includes('◆'), `Life: ${p1Life}`);

    const p1Reserve = await page.$eval('#p1-reserve', el => el.textContent.trim());
    check('4. P1 リザーブコア表示', p1Reserve.length > 0, `Reserve: ${p1Reserve}`);

    // 2. アクション一覧の確認
    const groupCount = await page.$$eval('#groups .group', els => els.length);
    check('5. アクション一覧描画', groupCount > 0, `グループ数: ${groupCount}`);

    // 3. アクション（配置コアなしのネクサス配置、またはリザーブが残る召喚）を実行
    const clickedActionText = await page.evaluate(async () => {
      const groups = Array.from(document.querySelectorAll('#groups .group'));
      // ネクサス配置（配置コアなし）を優先
      let targetGroup = groups.find(g => (g.textContent || '').includes('【配置】')) ||
                        groups.find(g => (g.textContent || '').includes('【召喚】'));
      if (targetGroup) {
        const btn = targetGroup.querySelector('button.head');
        if (btn) {
          btn.click();
          await new Promise(r => setTimeout(r, 400));
          // 「配置コアなし」のオプションがあればそれを選択
          const subOpts = Array.from(targetGroup.querySelectorAll('.opts button.opt-item, .opts button.source-opt-btn, .opts button'));
          const noCoreOpt = subOpts.find(b => (b.textContent || '').includes('配置コアなし')) || subOpts[0];
          if (noCoreOpt) {
            noCoreOpt.click();
          }
          return targetGroup.textContent.trim();
        }
      }
      return null;
    });

    console.log(`Action clicked: ${clickedActionText}`);
    check('6. 召喚/配置アクション実行', clickedActionText != null, `アクション: ${clickedActionText}`);

    // APIとSSEの反映を待機
    await new Promise(r => setTimeout(r, 2000));
    await page.screenshot({ path: path.join(screenshotDir, '03_after_deploy_or_summon.png') });

    // フィールドにカードが出たか確認 (#p1-field .card)
    const fieldCardCount = await page.$$eval('#p1-field .card', els => els.length);
    check('7. フィールドカード配置', fieldCardCount > 0, `フィールドカード数: ${fieldCardCount}`);

    // コア移動の選択肢が表示されているか確認 (常時表示されていること)
    const coreMoveInfo = await page.evaluate(() => {
      const groups = Array.from(document.querySelectorAll('#groups .group'));
      const coreGroups = groups.filter(g => (g.textContent || '').includes('【コア移動】') || (g.textContent || '').includes('【ソウルコア移動】'));
      return {
        totalCoreGroups: coreGroups.length,
        texts: coreGroups.map(g => g.querySelector('.cat')?.textContent?.trim() || g.textContent?.trim())
      };
    });

    check('8. コア移動選択肢の常時表示', coreMoveInfo.totalCoreGroups > 0, `表示コア移動グループ: ${JSON.stringify(coreMoveInfo.texts)}`);

    // 4. 直接マウス操作のテスト:
    // リザーブにコアがある場合は Reserve ➔ Field、なければ Field ➔ Reserve
    const moveScenario = await page.evaluate(async () => {
      const reserveText = document.querySelector('#p1-reserve')?.textContent?.trim() || '';
      const hasReserveCore = reserveText !== '-' && reserveText !== '0' && reserveText.length > 0;
      
      if (hasReserveCore) {
        // Reserve -> Card
        const rEl = document.querySelector('#p1-reserve-box');
        rEl.click();
        await new Promise(r => setTimeout(r, 300));
        const highlighted = rEl.classList.contains('selected-core-source');
        
        // Target card click
        const card = document.querySelector('#p1-field .card');
        card.click();
        return { direction: 'Reserve -> Card', highlighted };
      } else {
        // Card -> Reserve: カード上のコアをクリックして移動元選択
        const cardCore = document.querySelector('#p1-field .card .soul-core-btn, #p1-field .card .normal-core-btn, #p1-field .card');
        cardCore.click();
        await new Promise(r => setTimeout(r, 300));
        const highlighted = document.querySelector('.selected-core-source') != null;
        
        // Target reserve click
        const rEl = document.querySelector('#p1-reserve-box');
        rEl.click();
        return { direction: 'Card -> Reserve', highlighted };
      }
    });

    console.log('Move Scenario Result:', JSON.stringify(moveScenario));
    check('9. コア移動元の選択ハイライト', moveScenario.highlighted, `Direction: ${moveScenario.direction}`);

    await new Promise(r => setTimeout(r, 2000));
    await page.screenshot({ path: path.join(screenshotDir, '04_direct_core_move_executed.png') });

    const isCleared = await page.evaluate(() => {
      return !document.querySelector('.selected-core-source');
    });
    check('10. コア直接移動の完了とハイライト解除', isCleared);

    // 5. フィールドカード内アコーディオンメニューの検証
    await page.evaluate(() => {
      const card = document.querySelector('#p1-field .card');
      if (card) card.click();
    });
    await new Promise(r => setTimeout(r, 500));
    await page.screenshot({ path: path.join(screenshotDir, '05_card_accordion_opened.png') });

    const cardMenuVisible = await page.evaluate(() => {
      const opt = document.querySelector('#p1-field .field-options:not(.hidden)');
      return opt != null;
    });
    check('11. フィールドカード内コア移動メニュー展開', cardMenuVisible);

    // 6. 連打セレクター（マルチクリックバッジ）の検証
    const multiClickResult = await page.evaluate(async () => {
      const coreGroup = Array.from(document.querySelectorAll('#groups .group')).find(g => (g.textContent || '').includes('【コア移動】'));
      if (!coreGroup) return { found: false };
      const head = coreGroup.querySelector('button.head');
      if (!head) return { found: false };

      // 1回クリック
      head.click();
      await new Promise(r => setTimeout(r, 100));
      // 2回クリック
      head.click();
      await new Promise(r => setTimeout(r, 100));
      const badge = head.querySelector('.click-count-badge:not(.hidden)');
      const badgeText = badge ? badge.textContent : null;
      return { found: true, badgeText };
    });

    check('12. 連打セレクター機能', multiClickResult.found, `連打バッジ: ${multiClickResult.badgeText || 'なし(単一選択肢)'}`);
    await page.screenshot({ path: path.join(screenshotDir, '06_multiclick_selector.png') });

    console.log('\n=== GUI Test Execution Completed ===');
    const total = testReport.length;
    const passed = testReport.filter(r => r.pass).length;
    const failed = testReport.filter(r => !r.pass).length;
    console.log(`Summary: Total: ${total}, PASS: ${passed}, FAIL: ${failed}`);

    fs.writeFileSync(path.join(screenshotDir, 'report.json'), JSON.stringify({ total, passed, failed, tests: testReport }, null, 2), 'utf8');

  } catch (err) {
    console.error('Error during test execution:', err);
    await page.screenshot({ path: path.join(screenshotDir, 'error.png') });
  } finally {
    await browser.close();
  }
})();
