const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

(async () => {
  console.log('=== Testing Repeated Core Moves in Main Step ===');
  const screenshotDir = path.join(__dirname, 'gui_test_artifacts');
  if (!fs.existsSync(screenshotDir)) fs.mkdirSync(screenshotDir, { recursive: true });

  const executablePath = fs.existsSync('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe')
    ? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
    : 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';

  const browser = await puppeteer.launch({
    executablePath,
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1400, height: 900 });

  page.on('dialog', async d => {
    console.log('[DIALOG]', d.message());
    await d.dismiss();
  });

  try {
    const sid = 'repeat_test_' + Date.now().toString(36);
    const url = `http://localhost:8090/?sid=${sid}`;
    console.log(`Opening URL: ${url}`);
    await page.goto(url, { waitUntil: 'domcontentloaded' });
    await new Promise(r => setTimeout(r, 1000));

    // 新規ゲーム開始
    await page.click('#btn-new');
    await new Promise(r => setTimeout(r, 2000));

    // 場にカードを出す (召喚/配置)
    const cardPlayed = await page.evaluate(async () => {
      const groups = Array.from(document.querySelectorAll('#groups .group'));
      let target = groups.find(g => (g.textContent || '').includes('【配置】')) ||
                   groups.find(g => (g.textContent || '').includes('【召喚】'));
      if (target) {
        const btn = target.querySelector('button.head');
        if (btn) {
          btn.click();
          await new Promise(r => setTimeout(r, 400));
          const sub = target.querySelector('.opts button.opt-item, .opts button.source-opt-btn, .opts button');
          if (sub) sub.click();
          return true;
        }
      }
      return false;
    });

    console.log('Card played:', cardPlayed);
    await new Promise(r => setTimeout(r, 2000));

    // --- 1回目のコア移動の確認と実行 ---
    const move1Options = await page.evaluate(() => {
      const groups = Array.from(document.querySelectorAll('#groups .group'));
      return groups.filter(g => (g.textContent || '').includes('【コア移動】')).map(g => g.textContent.trim());
    });
    console.log('Move 1 available options:', move1Options);
    if (!move1Options.length) throw new Error('No core move options found after play!');

    // 1回目のコア移動を実行 (先頭のコア移動ボタンをクリック)
    const move1Executed = await page.evaluate(async () => {
      const g = Array.from(document.querySelectorAll('#groups .group')).find(g => (g.textContent || '').includes('【コア移動】'));
      if (!g) return false;
      const head = g.querySelector('button.head');
      head.click();
      await new Promise(r => setTimeout(r, 300));
      const sub = g.querySelector('.opts button');
      if (sub) sub.click();
      return true;
    });
    console.log('Move 1 executed:', move1Executed);
    await new Promise(r => setTimeout(r, 2000));
    await page.screenshot({ path: path.join(screenshotDir, 'repeat_after_move_1.png') });

    // --- 2回目のコア移動の確認 (移動後にもコア移動選択肢が存在するか？) ---
    const move2Options = await page.evaluate(() => {
      const groups = Array.from(document.querySelectorAll('#groups .group'));
      return groups.filter(g => (g.textContent || '').includes('【コア移動】')).map(g => g.textContent.trim());
    });
    console.log('Move 2 available options (AFTER MOVE 1):', move2Options);

    if (move2Options.length > 0) {
      console.log('SUCCESS: Core move is STILL available after move 1!');
    } else {
      console.error('FAILURE: Core move options disappeared after move 1!');
      throw new Error('Core move options disappeared after move 1');
    }

    // 2回目のコア移動を実行
    const move2Executed = await page.evaluate(async () => {
      const g = Array.from(document.querySelectorAll('#groups .group')).find(g => (g.textContent || '').includes('【コア移動】'));
      if (!g) return false;
      const head = g.querySelector('button.head');
      head.click();
      await new Promise(r => setTimeout(r, 300));
      const sub = g.querySelector('.opts button');
      if (sub) sub.click();
      return true;
    });
    console.log('Move 2 executed:', move2Executed);
    await new Promise(r => setTimeout(r, 2000));
    await page.screenshot({ path: path.join(screenshotDir, 'repeat_after_move_2.png') });

    // --- 3回目のコア移動の確認 (2回移動した後もさらに移動可能か？) ---
    const move3Options = await page.evaluate(() => {
      const groups = Array.from(document.querySelectorAll('#groups .group'));
      return groups.filter(g => (g.textContent || '').includes('【コア移動】')).map(g => g.textContent.trim());
    });
    console.log('Move 3 available options (AFTER MOVE 2):', move3Options);

    if (move3Options.length > 0) {
      console.log('VERIFIED: Repeated core moves confirmed (Move 1 -> Move 2 -> Move 3 all available)!');
    }

  } catch (err) {
    console.error('Error during repeated move test:', err);
  } finally {
    await browser.close();
  }
})();
