const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

(async () => {
  console.log('=== Battle Spirits Playmat GUI Test ===');
  const screenshotDir = path.join(__dirname, 'test_screenshots');
  if (!fs.existsSync(screenshotDir)) fs.mkdirSync(screenshotDir, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1400, height: 900 } });
  const page = await context.newPage();

  const results = [];
  function assert(name, condition, details = '') {
    if (condition) {
      console.log(`[PASS] ${name} ${details ? '(' + details + ')' : ''}`);
      results.push({ name, status: 'PASS', details });
    } else {
      console.error(`[FAIL] ${name} ${details ? '(' + details + ')' : ''}`);
      results.push({ name, status: 'FAIL', details });
    }
  }

  try {
    const roomId = 'gui_' + Date.now().toString(36);
    const targetUrl = `http://localhost:8090/?sid=${roomId}`;
    console.log(`Navigating to ${targetUrl}...`);
    await page.goto(targetUrl, { waitUntil: 'networkidle' });

    // 1. Initial page load
    assert('Page Title Check', (await page.title()).includes('BS Rule Engine'));
    await page.screenshot({ path: path.join(screenshotDir, '01_initial_load.png') });

    // 2. Start new game
    const newGameBtn = page.locator('#btn-new-game');
    assert('New Game Button Exists', await newGameBtn.isVisible());
    await newGameBtn.click();
    await page.waitForTimeout(1000);
    await page.screenshot({ path: path.join(screenshotDir, '02_game_started.png') });

    const phaseText = await page.locator('#phase').textContent();
    assert('Phase Displayed', phaseText.includes('メインステップ'), `Phase: ${phaseText}`);

    const p1Life = await page.locator('#p1-life').textContent();
    assert('P1 Life Rendered', p1Life.includes('◆'), `Life: ${p1Life}`);

    const p1Reserve = await page.locator('#p1-reserve').textContent();
    assert('P1 Reserve Rendered', p1Reserve.length > 0, `Reserve: ${p1Reserve}`);

    // 3. Inspect Action List in Sidebar
    const groupsBox = page.locator('#groups');
    const groupCount = await groupsBox.locator('.group').count();
    assert('Action Groups Rendered', groupCount > 0, `Group count: ${groupCount}`);

    // Check if any card can be played (summon/deploy) to put cards on field
    // Look for deploy nexus or summon spirit
    let actionChosen = false;
    const playActions = groupsBox.locator('.group:not(.core-all)');
    const totalPlayActions = await playActions.count();
    console.log(`Found ${totalPlayActions} non-core action groups.`);

    for (let i = 0; i < totalPlayActions; i++) {
      const act = playActions.nth(i);
      const text = await act.textContent();
      if (text.includes('配置') || text.includes('召喚')) {
        console.log(`Found summon/deploy action: ${text.trim().substring(0, 30)}...`);
        // If single button, click directly; if accordion, expand and click first
        const headBtn = act.locator('button.head');
        await headBtn.click();
        await page.waitForTimeout(300);

        const subBtn = act.locator('.opts button.opt-item, .opts button.source-opt-btn, button.head');
        if (await subBtn.count() > 0) {
          await subBtn.first().click();
        }
        actionChosen = true;
        break;
      }
    }

    if (actionChosen) {
      await page.waitForTimeout(1000);
      await page.screenshot({ path: path.join(screenshotDir, '03_after_deploy_or_summon.png') });

      // Check field cards
      const fieldCards = page.locator('#p1-field .field-card');
      const fieldCount = await fieldCards.count();
      assert('Card Placed on Field', fieldCount > 0, `Field cards: ${fieldCount}`);

      // Check core moves in groups box
      const coreMoveGroups = groupsBox.locator('.group').filter({ hasText: '【コア移動】' });
      const coreMoveCount = await coreMoveGroups.count();
      console.log(`Core move groups visible: ${coreMoveCount}`);
      assert('Core Move Visible (Not Collapsed/Hidden)', coreMoveCount > 0, `Visible core move groups: ${coreMoveCount}`);

      // 4. Test Core Direct Click Interaction
      console.log('Testing direct core mouse move (Reserve -> Field Card)...');
      const reserveCore = page.locator('#p1-reserve .core-icon-group').first();
      if (await reserveCore.isVisible()) {
        await reserveCore.click();
        await page.waitForTimeout(300);

        const isSourceSelected = await page.evaluate(() => window.selectedCoreMoveSource != null);
        assert('Reserve Core Click Selects Source', isSourceSelected, `Selected: ${JSON.stringify(await page.evaluate(() => window.selectedCoreMoveSource))}`);

        // Click target field card
        const targetCard = fieldCards.first();
        await targetCard.click();
        await page.waitForTimeout(1000);
        await page.screenshot({ path: path.join(screenshotDir, '04_after_direct_core_move.png') });

        const isCleared = await page.evaluate(() => window.selectedCoreMoveSource == null);
        assert('Source Selection Cleared After Move', isCleared);
      }
    }

    console.log('\n=== GUI Test Summary ===');
    const passed = results.filter(r => r.status === 'PASS').length;
    const failed = results.filter(r => r.status === 'FAIL').length;
    console.log(`Total: ${results.length}, PASS: ${passed}, FAIL: ${failed}`);

  } catch (err) {
    console.error('Fatal Error during GUI Test:', err);
    await page.screenshot({ path: path.join(screenshotDir, 'error.png') });
  } finally {
    await browser.close();
  }
})();
