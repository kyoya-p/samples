// セコム損保 火災保険見積もり 自動入力 Content Script
(async function () {
  console.log("[火災保険アシスタント] セコム損保 スクリプトロード:", window.location.href);

  const H = window.FI_HELPER;
  if (!H) return;

  const data = await H.getPropertyData();

  // フローティングパネル注入
  H.injectFloatingPanel("セコム損保", async (notify) => {
    await runAutoFill(notify);
  });

  // 初回自動試行
  setTimeout(() => {
    runAutoFill((msg) => console.log("[Secom AutoFill]", msg));
  }, 1000);

  async function runAutoFill(notify) {
    const text = document.body.innerText;

    // メンテナンス中の場合
    if (text.includes("メンテナンス中")) {
      notify("※只今Web受付メンテ中のため、最寄代理店での受付案内となっています");
      return;
    }

    // 一戸建て 見積スタートボタン
    const startBtns = Array.from(document.querySelectorAll("a, button")).filter(e => 
      e.innerText && e.innerText.includes("お見積りスタート")
    );
    if (startBtns.length >= 2) {
      notify("一戸建て見積もりを開始します...");
      startBtns[1].click();
      return;
    }

    notify("セコム安心マイホーム保険 画面を認識しました");
  }
})();
