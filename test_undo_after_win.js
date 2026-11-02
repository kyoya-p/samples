const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

(async () => {
  console.log('=== Testing [Undo] Functionality After Game Win ===');
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

  const testReport = [];
  function check(name, pass, msg = '') {
    const item = { name, pass, msg };
    testReport.push(item);
    console.log(`[${pass ? 'PASS' : 'FAIL'}] ${name} ${msg ? '- ' + msg : ''}`);
  }

  try {
    const sid = 'win_undo_' + Date.now().toString(36);
    const url = `http://localhost:8090/?sid=${sid}`;
    console.log(`Opening URL: ${url}`);
    await page.goto(url, { waitUntil: 'domcontentloaded' });
    await new Promise(r => setTimeout(r, 1000));

    // 新規ゲーム開始
    await page.click('#btn-new');
    await new Promise(r => setTimeout(r, 1500));

    // ゲームが決着するまで自動対戦（P1自動/P2自動、または手番ごとの最善手自動実行）で進める
    console.log('Advancing game to victory state...');
    let moves = 0;
    const maxMoves = 150;
    let isFinished = false;

    while (moves < maxMoves) {
      moves++;
      const state = await page.evaluate(async () => {
        // 現在の勝者確認
        const winnerEl = document.querySelector('#winner:not(.hidden)');
        const winnerText = winnerEl ? winnerEl.textContent : null;
        if (winnerText && winnerText.includes('勝利')) {
          return { finished: true, winnerText };
        }

        // 次の手番を実行 (一番上のアクションまたはステップ終了)
        const groups = Array.from(document.querySelectorAll('#groups .group:not(.core-all)'));
        if (groups.length > 0) {
          // 召喚・アタックなどの有効手があれば優先
          let actBtn = groups[0].querySelector('button.head');
          if (actBtn && !actBtn.disabled) {
            actBtn.click();
            await new Promise(r => setTimeout(r, 150));
            const sub = groups[0].querySelector('.opts button.opt-item, .opts button.source-opt-btn, .opts button');
            if (sub && !sub.disabled) sub.click();
            return { finished: false, action: actBtn.textContent.trim().substring(0, 25) };
          }
        }

        // なければステップ終了ボタン
        const nBtn = document.querySelector('#n-btn');
        if (nBtn && !nBtn.disabled) {
          nBtn.click();
          return { finished: false, action: 'ステップ終了' };
        }

        return { finished: false, action: 'none' };
      });

      if (state.finished) {
        console.log(`Game ended at move ${moves}! Winner text: ${state.winnerText}`);
        isFinished = true;
        break;
      }

      await new Promise(r => setTimeout(r, 200));
    }

    if (!isFinished) {
      console.log('Game did not naturally end within moves, checking current state via API...');
      // API直接で高速に勝敗まで進める
      const fastEnd = await page.evaluate(async (session) => {
        let count = 0;
        while (count < 200) {
          count++;
          const res = await fetch(`/api/state?sid=${session}`);
          const data = await res.json();
          if (data.winner != null) return { ok: true, winner: data.winner, count };
          if (data.n_index != null && !data.n_forbidden) {
            await fetch(`/api/step?sid=${session}&act=${data.n_index}`, { method: 'POST' });
          } else if (data.groups && data.groups.length > 0) {
            const firstOpt = data.groups[0].options?.[0];
            if (firstOpt && !firstOpt.forbidden) {
              await fetch(`/api/step?sid=${session}&act=${firstOpt.index}`, { method: 'POST' });
            } else {
              break;
            }
          } else {
            break;
          }
        }
        return { ok: false, count };
      }, sid);

      console.log('Fast-forward result:', fastEnd);
      // 再読み込みして最新状態を描画
      await page.goto(url, { waitUntil: 'domcontentloaded' });
      await new Promise(r => setTimeout(r, 1500));
    }

    await page.screenshot({ path: path.join(screenshotDir, 'win_01_game_over.png') });

    // 1. 勝利状態の確認
    const winStatus = await page.evaluate(() => {
      const banner = document.querySelector('#winner:not(.hidden)');
      const undoBtn = document.querySelector('#btn-undo');
      const bannerUndoBtn = banner ? banner.querySelector('button.btn-history') : null;
      return {
        bannerVisible: banner != null,
        bannerText: banner ? banner.textContent.trim() : null,
        toolbarUndoEnabled: undoBtn ? !undoBtn.disabled : false,
        bannerUndoExists: bannerUndoBtn != null
      };
    });

    console.log('Win status evaluated:', JSON.stringify(winStatus, null, 2));
    check('1. 勝利バナーの表示', winStatus.bannerVisible, winStatus.bannerText);
    check('2. ツールバーの[戻る]ボタン有効', winStatus.toolbarUndoEnabled);
    check('3. 勝利バナー内の[1手戻す]ボタン存在', winStatus.bannerUndoExists);

    // 2. [戻る] ボタンをクリックして巻き戻し
    console.log('Clicking Undo button after game win...');
    const undoClicked = await page.evaluate(() => {
      const btn = document.querySelector('#btn-undo:not(:disabled)') ||
                  document.querySelector('#winner button.btn-history');
      if (btn) {
        btn.click();
        return true;
      }
      return false;
    });

    check('4. [戻る]ボタン押下実行', undoClicked);
    await new Promise(r => setTimeout(r, 1500));
    await page.screenshot({ path: path.join(screenshotDir, 'win_02_after_undo.png') });

    // 3. 巻き戻し後の盤面検証
    const postUndoState = await page.evaluate(() => {
      const banner = document.querySelector('#winner');
      const isBannerHidden = banner == null || banner.classList.contains('hidden');
      const redoBtn = document.querySelector('#btn-redo');
      const groups = Array.from(document.querySelectorAll('#groups .group'));
      const phase = document.querySelector('#phase')?.textContent?.trim();
      return {
        isBannerHidden,
        redoEnabled: redoBtn ? !redoBtn.disabled : false,
        actionGroupCount: groups.length,
        phase
      };
    });

    console.log('Post-undo state evaluated:', JSON.stringify(postUndoState, null, 2));
    check('5. 勝利バナーの非表示化(決着解除)', postUndoState.isBannerHidden);
    check('6. [進む ▶]ボタンの有効化(再進行可能)', postUndoState.redoEnabled);
    check('7. 決着直前のフェーズ復帰', postUndoState.phase != null, `Phase: ${postUndoState.phase}`);
    check('8. 選択可能なアクション一覧の復活', postUndoState.actionGroupCount > 0, `グループ数: ${postUndoState.actionGroupCount}`);

    // 4. [進む ▶] ボタンをクリックして再進行（再び決着へ進めるか）
    console.log('Clicking Redo button to re-advance...');
    const redoClicked = await page.evaluate(() => {
      const btn = document.querySelector('#btn-redo:not(:disabled)');
      if (btn) {
        btn.click();
        return true;
      }
      return false;
    });

    check('9. [進む ▶]ボタン押下実行', redoClicked);
    await new Promise(r => setTimeout(r, 1500));
    await page.screenshot({ path: path.join(screenshotDir, 'win_03_after_redo.png') });

    const postRedoState = await page.evaluate(() => {
      const banner = document.querySelector('#winner:not(.hidden)');
      return {
        bannerVisible: banner != null,
        bannerText: banner ? banner.textContent.trim() : null
      };
    });

    check('10. 再び勝利状態への進行成功', postRedoState.bannerVisible, postRedoState.bannerText);

    console.log('\n=== Win Undo Test Summary ===');
    const total = testReport.length;
    const passed = testReport.filter(r => r.pass).length;
    const failed = testReport.filter(r => !r.pass).length;
    console.log(`Total: ${total}, PASS: ${passed}, FAIL: ${failed}`);

    fs.writeFileSync(path.join(screenshotDir, 'win_undo_report.json'), JSON.stringify({ total, passed, failed, tests: testReport }, null, 2), 'utf8');

  } catch (err) {
    console.error('Error during win undo test execution:', err);
    await page.screenshot({ path: path.join(screenshotDir, 'win_undo_error.png') });
  } finally {
    await browser.close();
  }
})();
