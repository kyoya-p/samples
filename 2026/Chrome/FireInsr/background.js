// Chrome Extension Background Service Worker (v1.2.0)

// アイコンクリック時にサイドパネルを固定表示として開く設定
if (chrome.sidePanel && chrome.sidePanel.setPanelBehavior) {
  chrome.sidePanel
    .setPanelBehavior({ openPanelOnActionClick: true })
    .catch((error) => console.error("SidePanel setting error:", error));
}

chrome.runtime.onInstalled.addListener(() => {
  console.log("火災保険 一括見積もりアシスタント インストール完了");

  const DEFAULT_DATA = {
    buildingType: "1",
    prefecture: "奈良県",
    prefCode: "29",
    postalCode: "6391015",
    address: "奈良県大和郡山市箕山町6-27",
    streetNumber: "６－２７",
    buildingStructure: "2",
    pillarType: "木造",
    builtYear: "2000",
    builtMonth: "09",
    buildingAge: "25",
    buildingAmount: "1500",
    goodsAmount: "500",
    earthquake: "1",
    allElectric: true,
    waterDamage: false,
    ownerLastName: "河野",
    ownerFirstName: "恭也",
    ownerKanaLast: "コウノ",
    ownerKanaFirst: "キョウヤ",
    startDateYear: "2026",
    startDateMonth: "11",
    startDateDay: "21",
    startDateHour: "16",
    floors: "2",
    area: "98"
  };

  chrome.storage.local.get(["propertyData"], (result) => {
    if (!result.propertyData) {
      chrome.storage.local.set({ propertyData: DEFAULT_DATA });
    }
  });
});

// メッセージ中継（各コンテンツスクリプトから試算結果を受信）
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === "quoteCompleted" || request.action === "saveResult") {
    console.log("[Background] 見積もり結果受信:", request);

    chrome.storage.local.get(["quoteResults"], (data) => {
      const results = data.quoteResults || {};
      if (request.company) {
        results[request.company] = request.data || request.price;
      }
      results.updatedAt = new Date().toISOString();

      chrome.storage.local.set({ quoteResults: results }, () => {
        // 開いているサイドパネルにも同報通知
        chrome.runtime.sendMessage({
          action: "updateUI",
          company: request.company,
          data: request.data || request.price
        }).catch(() => {});
        sendResponse({ status: "success" });
      });
    });
    return true;
  }
});