const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

(async () => {
  console.log('=== Verifying [Undo] After Game Win ===');
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
    const sid = 'verify_undo_' + Date.now().toString(36);
    const url = `http://localhost:8090/?sid=${sid}`;
    console.log(`Opening URL: ${url}`);
    await page.goto(url, { waitUntil: 'domcontentloaded' });
    await new Promise(r => setTimeout(r, 1000));

    // リプレイファイル (280809 seed) と同じシードで新規ゲーム開始
    console.log('Starting game with seed 280809...');
    await page.evaluate(async (session) => {
      await fetch(`/api/new?sid=${session}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deck1: 'deck-kogyo.yaml', deck2: 'deck-kogyo.yaml', seed: 280809, format: 'STANDARD' })
      });
    }, sid);

    await page.goto(url, { waitUntil: 'domcontentloaded' });
    await new Promise(r => setTimeout(r, 1000));

    // 高速でステップを進めて winner != null に到達させる
    console.log('Replaying moves to reach game-winning state...');
    const result = await page.evaluate(async (session) => {
      let count = 0;
      while (count < 300) {
        count++;
        const res = await fetch(`/api/state?sid=${session}`);
        const data = await res.json();
        if (data.winner != null) {
          return { finished: true, winner: data.winner, count };
        }

        // アタック宣言ステップでアタックできるスピリットがあればアタックを優先
        if (data.groups && data.groups.length > 0) {
          let attackGroup = data.groups.find(g => (g.category || '').includes('【アタック】'));
          if (attackGroup && attackGroup.options && attackGroup.options.length > 0) {
            await fetch(`/api/act?sid=${session}`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ index: attackGroup.options[0].index })
            });
            continue;
          }

          // ブロック宣言時はブロックしないを優先 (速やかにライフを削る)
          let passBlock = data.groups.find(g => (g.category || '').includes('ブロックしない'));
          if (passBlock && passBlock.options && passBlock.options.length > 0) {
            await fetch(`/api/act?sid=${session}`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ index: passBlock.options[0].index })
            });
            continue;
          }

          // メインステップで召喚できるなら召喚
          let summonGroup = data.groups.find(g => (g.category || '').includes('【召喚】'));
          if (summonGroup && summonGroup.options && summonGroup.options.length > 0) {
            await fetch(`/api/act?sid=${session}`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ index: summonGroup.options[0].index })
            });
            continue;
          }
        }

        // それ以外はステップ進行 (n_index)
        if (data.n_index != null && !data.n_forbidden) {
          await fetch(`/api/act?sid=${session}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ index: data.n_index })
          });
        } else if (data.groups && data.groups.length > 0 && data.groups[0].options?.[0]) {
          await fetch(`/api/act?sid=${session}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ index: data.groups[0].options[0].index })
          });
        } else {
          break;
        }
      }
      return { finished: false, count };
    }, sid);

    console.log('Advancement result:', JSON.stringify(result));

    // 最新状態を画面にリフレッシュ
    await page.goto(url, { waitUntil: 'domcontentloaded' });
    await new Promise(r => setTimeout(r, 1500));
    await page.screenshot({ path: path.join(screenshotDir, 'win_verify_01_game_over.png') });

    // 1. 勝利状態の確認
    const winStatus = await page.evaluate(() => {
      const banner = document.querySelector('#winner:not(.hidden)');
      const undoBtn = document.querySelector('#btn-undo');
      const bannerUndoBtn = banner ? banner.querySelector('button.btn-history, button[onclick="undo()"]') : null;
      return {
        bannerVisible: banner != null,
        bannerText: banner ? banner.textContent.trim() : null,
        toolbarUndoEnabled: undoBtn ? !undoBtn.disabled : false,
        bannerUndoExists: bannerUndoBtn != null
      };
    });

    console.log('Win status evaluated:', JSON.stringify(winStatus, null, 2));
    check('1. 勝利バナーの表示 (ゲーム決着)', winStatus.bannerVisible, winStatus.bannerText);
    check('2. ツールバーの[◀ 戻る]ボタンが有効', winStatus.toolbarUndoEnabled);
    check('3. 勝利バナー内の[◀ 1手戻す]ボタンが存在', winStatus.bannerUndoExists);

    // 2. 勝利画面で [戻る] ボタンを押下
    console.log('Clicking Undo button in victory state...');
    const undoClicked = await page.evaluate(() => {
      const bannerBtn = document.querySelector('#winner button.btn-history, #winner button[onclick="undo()"]');
      if (bannerBtn) {
        bannerBtn.click();
        return { clicked: true, target: 'banner-undo' };
      }
      const toolbarBtn = document.querySelector('#btn-undo:not(:disabled)');
      if (toolbarBtn) {
        toolbarBtn.click();
        return { clicked: true, target: 'toolbar-undo' };
      }
      return { clicked: false };
    });

    check('4. 勝利画面での[戻る]ボタン押下実行', undoClicked.clicked, `Clicked target: ${undoClicked.target}`);
    await new Promise(r => setTimeout(r, 1500));
    await page.screenshot({ path: path.join(screenshotDir, 'win_verify_02_after_undo.png') });

    // 3. 巻き戻し後の盤面状態を検証
    const postUndoState = await page.evaluate(() => {
      const banner = document.querySelector('#winner');
      const isBannerHidden = banner == null || banner.classList.contains('hidden');
      const redoBtn = document.querySelector('#btn-redo');
      const groups = Array.from(document.querySelectorAll('#groups .group'));
      const phase = document.querySelector('#phase')?.textContent?.trim();
      const p1Life = document.querySelector('#p1-life')?.textContent?.trim();
      const p2Life = document.querySelector('#p2-life')?.textContent?.trim();
      return {
        isBannerHidden,
        redoEnabled: redoBtn ? !redoBtn.disabled : false,
        actionGroupCount: groups.length,
        phase,
        p1Life,
        p2Life
      };
    });

    console.log('Post-undo state evaluated:', JSON.stringify(postUndoState, null, 2));
    check('5. 勝利バナーの非表示化 (決着解除)', postUndoState.isBannerHidden);
    check('6. [進む ▶]ボタンの有効化 (Redo可能)', postUndoState.redoEnabled);
    check('7. 決着直前のフェーズに復帰', postUndoState.phase != null, `Phase: ${postUndoState.phase}`);
    check('8. 選択可能なアクション一覧の復活', postUndoState.actionGroupCount > 0, `グループ数: ${postUndoState.actionGroupCount}`);

    // 4. [進む ▶] (Redo) を押して、元の勝利局面に再び進めるか検証
    console.log('Clicking Redo button to re-advance to victory...');
    const redoClicked = await page.evaluate(() => {
      const btn = document.querySelector('#btn-redo:not(:disabled)');
      if (btn) {
        btn.click();
        return true;
      }
      return false;
    });

    check('9. [進む ▶] (Redo) 実行', redoClicked);
    await new Promise(r => setTimeout(r, 1500));
    await page.screenshot({ path: path.join(screenshotDir, 'win_verify_03_after_redo.png') });

    const postRedoState = await page.evaluate(() => {
      const banner = document.querySelector('#winner:not(.hidden)');
      return {
        bannerVisible: banner != null,
        bannerText: banner ? banner.textContent.trim() : null
      };
    });

    check('10. 再び勝利状態への進行確認 (Redo正常稼働)', postRedoState.bannerVisible, postRedoState.bannerText);

    console.log('\n=== Win Undo Test Summary ===');
    const total = testReport.length;
    const passed = testReport.filter(r => r.pass).length;
    const failed = testReport.filter(r => !r.pass).length;
    console.log(`Summary: Total: ${total}, PASS: ${passed}, FAIL: ${failed}`);

    fs.writeFileSync(path.join(screenshotDir, 'win_undo_report.json'), JSON.stringify({ total, passed, failed, tests: testReport }, null, 2), 'utf8');

  } catch (err) {
    console.error('Error during win undo verification:', err);
    await page.screenshot({ path: path.join(screenshotDir, 'win_verify_error.png') });
  } finally {
    await browser.close();
  }
})();
