// Chrome Extension Popup / SidePanel Script (v1.3.0)
document.addEventListener("DOMContentLoaded", () => {
  const PREF_CODES = {
    "奈良県": "29", "東京都": "13", "大阪府": "27", "神奈川県": "14",
    "愛知県": "23", "京都府": "26", "兵庫県": "28"
  };

  const DEFAULT_DATA = {
    buildingType: "1",
    prefecture: "奈良県",
    prefCode: "29",
    postalCode: "6391015",
    address: "大和郡山市箕山町6-27",
    buildingStructure: "2",
    pillarType: "木造",
    builtYear: "2000",
    builtMonth: "9",
    buildingAge: "25",
    area: "98",
    buildingAmount: "1500",
    goodsAmount: "500",
    waterDamage: "0",
    earthquake: "1",
    accidentalDamage: "5",
    allElectric: "1",
    ownerLastName: "河野",
    ownerFirstName: "恭也",
    ownerKanaLast: "コウノ",
    ownerKanaFirst: "キョウヤ",
    startYear: "2026",
    startMonth: "11",
    startDay: "21",
    policyPeriod: "5"
  };

  // 1. 保存済みパラメータのロード
  chrome.storage.local.get(["propertyData", "quoteResults"], (res) => {
    const d = res.propertyData || DEFAULT_DATA;
    setFormValues(d);
    if (res.quoteResults) updateResultUI(res.quoteResults);
  });

  function setFormValues(d) {
    if (d.buildingType) document.getElementById("buildingType").value = d.buildingType;
    if (d.prefecture) document.getElementById("prefecture").value = d.prefecture;
    if (d.postalCode) document.getElementById("postalCode").value = d.postalCode;
    if (d.address) document.getElementById("address").value = d.address;
    if (d.buildingStructure) document.getElementById("buildingStructure").value = d.buildingStructure;
    if (d.pillarType) document.getElementById("pillarType").value = d.pillarType;
    if (d.builtYear) document.getElementById("builtYear").value = d.builtYear;
    if (d.builtMonth) document.getElementById("builtMonth").value = d.builtMonth;
    if (d.buildingAge) document.getElementById("buildingAge").value = d.buildingAge;
    if (d.area) document.getElementById("area").value = d.area;
    if (d.buildingAmount) document.getElementById("buildingAmount").value = d.buildingAmount;
    if (d.goodsAmount) document.getElementById("goodsAmount").value = d.goodsAmount;
    if (d.waterDamage) document.getElementById("waterDamage").value = d.waterDamage;
    if (d.earthquake) document.getElementById("earthquake").value = d.earthquake;
    if (d.accidentalDamage) document.getElementById("accidentalDamage").value = d.accidentalDamage;
    if (d.allElectric) document.getElementById("allElectric").value = d.allElectric;
    if (d.ownerLastName) document.getElementById("ownerLastName").value = d.ownerLastName;
    if (d.ownerFirstName) document.getElementById("ownerFirstName").value = d.ownerFirstName;
    if (d.ownerKanaLast) document.getElementById("ownerKanaLast").value = d.ownerKanaLast;
    if (d.ownerKanaFirst) document.getElementById("ownerKanaFirst").value = d.ownerKanaFirst;
    if (d.startYear) document.getElementById("startYear").value = d.startYear;
    if (d.startMonth) document.getElementById("startMonth").value = d.startMonth;
    if (d.startDay) document.getElementById("startDay").value = d.startDay;
    if (d.policyPeriod) document.getElementById("policyPeriod").value = d.policyPeriod;
  }

  function getFormValues() {
    const pref = document.getElementById("prefecture").value;
    return {
      buildingType: document.getElementById("buildingType").value,
      prefecture: pref,
      prefCode: PREF_CODES[pref] || "29",
      postalCode: document.getElementById("postalCode").value.replace("-", ""),
      address: document.getElementById("address").value,
      buildingStructure: document.getElementById("buildingStructure").value,
      pillarType: document.getElementById("pillarType").value,
      builtYear: document.getElementById("builtYear").value,
      builtMonth: document.getElementById("builtMonth").value,
      buildingAge: document.getElementById("buildingAge").value,
      area: document.getElementById("area").value,
      buildingAmount: document.getElementById("buildingAmount").value,
      goodsAmount: document.getElementById("goodsAmount").value,
      waterDamage: document.getElementById("waterDamage").value,
      earthquake: document.getElementById("earthquake").value,
      accidentalDamage: document.getElementById("accidentalDamage").value,
      allElectric: document.getElementById("allElectric").value,
      ownerLastName: document.getElementById("ownerLastName").value,
      ownerFirstName: document.getElementById("ownerFirstName").value,
      ownerKanaLast: document.getElementById("ownerKanaLast").value,
      ownerKanaFirst: document.getElementById("ownerKanaFirst").value,
      startYear: document.getElementById("startYear").value,
      startMonth: document.getElementById("startMonth").value,
      startDay: document.getElementById("startDay").value,
      policyPeriod: document.getElementById("policyPeriod").value
    };
  }

  // 初期値リセット
  document.getElementById("btnResetDefault").addEventListener("click", () => {
    setFormValues(DEFAULT_DATA);
    chrome.storage.local.set({ propertyData: DEFAULT_DATA });
  });

  // ⚡ メイン見積ボタン実行
  document.getElementById("btnRunQuote").addEventListener("click", () => {
    const data = getFormValues();
    chrome.storage.local.set({ propertyData: data }, () => {
      console.log("[SidePanel] 全パラメータ保存完了:", data);

      // 動的クエリ付き価格.com直通URL
      const kakakuUrl = `https://hoken.kakaku.com/kasai/hikaku/house/?fr_BuildingStructureType=${data.buildingStructure}&fr_Prefecture=${data.prefCode}&fr_BuildingAge=${data.buildingAge}&fr_BuildingInsAmount=${data.buildingAmount}&fr_HouseGoodsInsAmount=${data.goodsAmount}&fr_EarthquakeInsType=${data.earthquake}`;

      // 各社サイトをバックグラウンドタブで開く
      chrome.tabs.create({ url: kakakuUrl, active: false });
      setTimeout(() => chrome.tabs.create({ url: "https://www.sonysonpo.co.jp/fire/", active: false }), 600);
      setTimeout(() => chrome.tabs.create({ url: "https://kasai.sbisonpo.co.jp/portal/EPCP1020.do?cp=2401000", active: false }), 1200);

      // ボタンアニメーション
      const btn = document.getElementById("btnRunQuote");
      btn.innerText = "⚡ 見積もり実行中（各サイト自動巡回）...";
      btn.style.background = "#e67e22";
      setTimeout(() => {
        btn.innerText = "⚡ この条件で一括見積もりを実行する";
        btn.style.background = "linear-gradient(135deg, #27ae60, #2ecc71)";
      }, 4000);
    });
  });

  // 個別ボタン
  document.getElementById("btnOpenKakaku").addEventListener("click", () => {
    const d = getFormValues();
    const url = `https://hoken.kakaku.com/kasai/hikaku/house/?fr_BuildingStructureType=${d.buildingStructure}&fr_Prefecture=${d.prefCode}&fr_BuildingAge=${d.buildingAge}&fr_BuildingInsAmount=${d.buildingAmount}&fr_HouseGoodsInsAmount=${d.goodsAmount}&fr_EarthquakeInsType=${d.earthquake}`;
    chrome.tabs.create({ url });
  });

  document.getElementById("btnOpenSony").addEventListener("click", () => {
    chrome.tabs.create({ url: "https://www.sonysonpo.co.jp/fire/" });
  });

  document.getElementById("btnOpenSBI").addEventListener("click", () => {
    chrome.tabs.create({ url: "https://kasai.sbisonpo.co.jp/portal/EPCP1020.do?cp=2401000" });
  });

  document.getElementById("btnOpenSecom").addEventListener("click", () => {
    chrome.tabs.create({ url: "https://direct.secom-anshinmyhome.jp/amh_direct_page/" });
  });

  // リアルタイム結果更新リスナー
  chrome.runtime.onMessage.addListener((msg) => {
    if (msg.action === "updateUI") {
      if (msg.company === "kakaku" && msg.data) {
        if (msg.data.nisshin) highlight("val-nisshin", msg.data.nisshin);
        if (msg.data.zurich) highlight("val-zurich", msg.data.zurich);
        if (msg.data.sony) highlight("val-sony", msg.data.sony);
      }
      if (msg.company === "sony" && msg.data) {
        highlight("val-sony", msg.data.fiveYearPremium || msg.data);
      }
      if (msg.company === "sbi" && msg.data) {
        highlight("val-sbi", msg.data.premium || msg.data);
      }
    }
  });

  function updateResultUI(results) {
    if (results.kakaku) {
      if (results.kakaku.nisshin) document.getElementById("val-nisshin").innerText = results.kakaku.nisshin;
      if (results.kakaku.zurich) document.getElementById("val-zurich").innerText = results.kakaku.zurich;
    }
    if (results.sony) document.getElementById("val-sony").innerText = results.sony.fiveYearPremium || results.sony;
    if (results.sbi) document.getElementById("val-sbi").innerText = results.sbi.premium || results.sbi;
  }

  function highlight(id, text) {
    const el = document.getElementById(id);
    if (!el) return;
    el.innerText = text;
    el.style.backgroundColor = "#fff3cd";
    setTimeout(() => { el.style.backgroundColor = ""; }, 1500);
  }
});