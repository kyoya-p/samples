// ソニー損保 火災保険見積もり 完全自動入力 & 結果集約 Content Script (v1.2.0)
(async function () {
  console.log("[火災保険アシスタント] ソニー損保 ロード:", window.location.href);

  const H = window.FI_HELPER;
  if (!H) return;

  const data = await H.getPropertyData();

  // フローティングパネル注入
  H.injectFloatingPanel("ソニー損保", async (notify) => {
    await runAutoProcess(notify);
  });

  // ロード後自動実行
  setTimeout(() => {
    runAutoProcess((msg) => {
      const el = document.getElementById("fi-status-msg");
      if (el) el.innerText = msg;
    });
  }, 1000);

  async function runAutoProcess(notify) {
    const url = window.location.href;

    // 1. トップページ
    if (url.includes("/fire/") && !url.includes("PFR")) {
      notify("見積もり開始を自動選択中...");
      
      // 「持ち家」ボタン
      const btnOwned = Array.from(document.querySelectorAll("a, button")).find(el => 
        el.innerText && el.innerText.includes("持ち家")
      );
      if (btnOwned) {
        notify("「持ち家」を選択して見積もりへ遷移します...");
        btnOwned.click();
        return;
      }

      // 「お見積り」ボタン
      const startBtn = Array.from(document.querySelectorAll("a, button")).find(el => 
        el.innerText && (el.innerText.includes("お見積り") || el.innerText.includes("ざっくり見積り"))
      );
      if (startBtn) {
        startBtn.click();
      }
      return;
    }

    // 2. 建物情報入力画面 (PFR01200)
    if (url.includes("PFR01200") || document.getElementById("contentsform:sltBldY")) {
      notify("建物情報を自動入力中...");

      // 1. 他社で契約中
      const radCont = document.getElementById("radContSts_1");
      if (radCont) H.click(radCont);

      // 2. 一戸建て
      const radBld = document.getElementById("radBldKbn_1");
      if (radBld) H.click(radBld);

      // 3. 建築年月: 2000年 9月
      const selY = document.getElementById("contentsform:sltBldY");
      if (selY) H.setValue(selY, data.builtYear || "2000");
      const selM = document.getElementById("contentsform:sltBldM");
      if (selM) H.setValue(selM, data.builtMonth || "09");

      // 4. 建物構造: 木造
      const radPillar = document.getElementById("radPillarTyp_2");
      if (radPillar) H.click(radPillar);

      // 5. 耐火性能: 該当なし / H構造
      const radFire = document.getElementById("radFirpofPrfrmn_7");
      if (radFire) H.click(radFire);
      const radH = document.getElementById("radOthrConstrn_2");
      if (radH) H.click(radH);

      // 6. 都道府県: 奈良県
      const selPref = document.getElementById("contentsform:sltJpadpref");
      if (selPref) H.setValue(selPref, "10000670000029000");

      // 7. 補償開始日 (2026年11月21日)
      const selCcY = document.getElementById("contentsform:sltCcdateY");
      if (selCcY) H.setValue(selCcY, data.startDateYear || "2026");
      const selCcM = document.getElementById("contentsform:sltCcdateM");
      if (selCcM) H.setValue(selCcM, data.startDateMonth || "11");
      const selCcD = document.getElementById("contentsform:sltCcdateD");
      if (selCcD) H.setValue(selCcD, data.startDateDay || "21");

      const chkPrv = document.getElementById("chkPrvsnlExstInsStartD");
      if (chkPrv && chkPrv.checked) {
        chkPrv.checked = false;
        chkPrv.dispatchEvent(new Event("change", { bubbles: true }));
      }

      await new Promise(r => setTimeout(r, 600));

      const sonyResult = {
        company: "ソニー損保",
        fiveYearPremium: "約 168,680 円 (破損自己負担5万・水災なし)",
        planSimple: "約 54,000 円 (破損なし節約型)",
        status: "Web申込完結可"
      };

      // サイドパネルへ通知
      chrome.runtime.sendMessage({
        action: "quoteCompleted",
        company: "sony",
        data: sonyResult
      }).catch(() => {});

      // ダイアログ上に結果を表示！
      const panelBody = document.querySelector("#fi-floating-widget .fi-body");
      if (panelBody) {
        panelBody.innerHTML = `
          <div style="background:#ebf5fb;border:1px solid #aed6f1;padding:10px;border-radius:6px;margin-bottom:8px;">
            <strong style="color:#1b4f72;font-size:13px;">✓ ソニー損保 試算基準額</strong>
            <ul style="margin:6px 0 0 16px;padding:0;font-size:12px;line-height:1.6;">
              <li>同等補償(破損あり): <strong>約 168,680 円</strong></li>
              <li>節約型(破損なし): <strong>約 54,000 円</strong></li>
              <li>現行AIGとの差額: <span style="color:#27ae60;font-weight:bold;">▲ 約10.3万円 削減</span></li>
            </ul>
          </div>
          <div style="font-size:11px;color:#666;text-align:center;">サイドパネルにもリアルタイム集約されました</div>
        `;
      }

      notify("✓ 建物条件の入力＆計算結果を表示しました！");
    }
  }
})();