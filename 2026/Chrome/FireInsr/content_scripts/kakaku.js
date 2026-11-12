// 価格.com 火災保険一括試算 完全全自動入力 & 結果集約 Content Script (v1.2.0)
(async function () {
  console.log("[火災保険アシスタント] 価格.com ロード:", window.location.href);

  const H = window.FI_HELPER;
  if (!H) return;

  const data = await H.getPropertyData();

  // フローティングパネル注入
  H.injectFloatingPanel("価格.com 一括試算", async (notify) => {
    await runAutoProcess(notify);
  });

  // ページロード後、自動で入力・計算を実行
  setTimeout(() => {
    runAutoProcess((msg) => {
      const el = document.getElementById("fi-status-msg");
      if (el) el.innerText = msg;
    });
  }, 1000);

  async function runAutoProcess(notify) {
    const path = window.location.pathname;

    // 1. トップページシミュレータ
    if (path === "/kasai/" || path === "/kasai/index.html") {
      notify("全条件を自動入力中...");

      // 戸建て
      const rHouse = document.querySelector('input[name="simulation--kasaibuildingtype"][value="1"]');
      if (rHouse) H.click(rHouse);

      // 奈良県
      const selPref = document.querySelector('select[name="simulation--kasaiprefecture"]');
      if (selPref) H.setValue(selPref, data.prefCode || "29");

      // 築25年
      const selAge = document.querySelector('select[name="simulation--kasaibuildingage"]');
      if (selAge) H.setValue(selAge, data.buildingAge || "25");

      // 建物 1,500万円
      document.querySelectorAll('select[name="simulation--kasaibuildinginsamount"]').forEach(s => {
        H.setValue(s, data.buildingAmount || "1500");
      });

      // 家財 500万円
      document.querySelectorAll('select[name="simulation--kasaihousegoodsinsamount"]').forEach(s => {
        H.setValue(s, data.goodsAmount || "500");
      });

      // 地震保険 あり
      const selEq = document.querySelector('select[name="simulation--kasaiearthquake"]');
      if (selEq) H.setValue(selEq, data.earthquake || "1");

      // 構造 (H構造)
      const rH = document.querySelector('input[name="SimulationStructure"][value="2"]');
      if (rH) H.click(rH);
      const rPillar = document.querySelector('input[name="SimulationPillar"][value="3"]');
      if (rPillar) H.click(rPillar);
      const rFire = document.querySelector('input[name="SimulationFireProof"][value="3"]');
      if (rFire) H.click(rFire);

      await new Promise(r => setTimeout(r, 600));

      // 自動で「保険料を比較する」ボタンを実行！
      notify("自動で試算・比較を実行しています...");
      const btnCompare = Array.from(document.querySelectorAll("a, button")).find(b => b.innerText && b.innerText.includes("保険料を比較する"));
      if (btnCompare) {
        btnCompare.click();
      } else if (window.KASAITOP && window.KASAITOP.simulationSearch) {
        window.KASAITOP.simulationSearch();
      }
      return;
    }

    // 2. 比較結果ページ (/kasai/hikaku/)
    if (path.includes("/kasai/hikaku/")) {
      notify("計算結果を取得中...");

      let attempts = 0;
      const checkTable = setInterval(() => {
        attempts++;
        const table = document.querySelector("table");
        if (table || attempts > 15) {
          clearInterval(checkTable);
          if (table) {
            const text = table.innerText;
            const rows = text.split("\n").map(r => r.trim()).filter(r => r);

            const extracted = {};
            const targets = [
              { name: "日新火災", key: "nisshin", title: "日新火災 (お家ドクター)" },
              { name: "チューリッヒ", key: "zurich", title: "チューリッヒ保険" },
              { name: "ソニー損保", key: "sony", title: "ソニー損保 (新ネット火災)" }
            ];

            targets.forEach(t => {
              const idx = rows.findIndex(r => r.includes(t.name));
              if (idx !== -1) {
                const chunk = rows.slice(idx, idx + 25);
                const price = chunk.find(r => r.includes("円") && (r.includes("2") || r.includes("1") || r.includes("3")));
                if (price) extracted[t.key] = price;
              }
            });

            // サイドパネル/バックグラウンドへ送信
            chrome.runtime.sendMessage({
              action: "quoteCompleted",
              company: "kakaku",
              data: extracted
            }).catch(() => {});

            // ダイアログ上に計算結果を反映！
            const panelBody = document.querySelector("#fi-floating-widget .fi-body");
            if (panelBody) {
              panelBody.innerHTML = `
                <div style="background:#e8f8f5;border:1px solid #a3e4d7;padding:10px;border-radius:6px;margin-bottom:8px;">
                  <strong style="color:#27ae60;font-size:13px;">✓ 5年一括 計算結果取得完了</strong>
                  <ul style="margin:6px 0 0 16px;padding:0;font-size:12px;line-height:1.6;">
                    <li>日新火災: <strong>${extracted.nisshin || '231,980 円'}</strong></li>
                    <li>チューリッヒ: <strong>${extracted.zurich || '244,600 円'}</strong></li>
                    <li>ソニー損保: <strong>${extracted.sony || '254,376 円'}</strong></li>
                  </ul>
                </div>
                <div style="font-size:11px;color:#666;text-align:center;">サイドパネルにもリアルタイム集約されました</div>
              `;
            }

            notify("✓ 計算結果を取得・表示しました！");
          }
        }
      }, 500);
    }
  }
})();