// 各社共通の入力ヘルパー関数 & デフォルトデータ定義
window.FI_HELPER = {
  // デフォルト物件データ（ストレージが空でも絶対に動作を保証）
  DEFAULT_DATA: {
    buildingType: "1",         // 1: 戸建て, 2: マンション
    prefecture: "奈良県",
    prefCode: "29",
    postalCode: "6391015",
    address: "奈良県大和郡山市箕山町6-27",
    streetNumber: "６－２７",
    buildingStructure: "2",     // 2: H構造(木造非耐火), 3: T構造
    pillarType: "木造",
    builtYear: "2000",
    builtMonth: "09",
    buildingAge: "25",
    buildingAmount: "1500",     // 万円
    goodsAmount: "500",         // 万円
    earthquake: "1",           // 1: あり, 2: なし
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
  },

  // 物件データを非同期取得（未保存時はデフォルト値を自動補完）
  async getPropertyData() {
    return new Promise((resolve) => {
      try {
        chrome.storage.local.get(["propertyData"], (res) => {
          if (res && res.propertyData && Object.keys(res.propertyData).length > 0) {
            resolve({ ...this.DEFAULT_DATA, ...res.propertyData });
          } else {
            chrome.storage.local.set({ propertyData: this.DEFAULT_DATA });
            resolve(this.DEFAULT_DATA);
          }
        });
      } catch (e) {
        resolve(this.DEFAULT_DATA);
      }
    });
  },

  // Vue.js / React / 通常フォームすべてに確実に値を反映させる
  setValue(el, val) {
    if (!el) return false;
    el.focus();
    
    // ネイティブプロトタイプのセッターを直接実行
    const prototype = el instanceof HTMLSelectElement ? HTMLSelectElement.prototype :
                      el instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
    const desc = Object.getOwnPropertyDescriptor(prototype, 'value');
    if (desc && desc.set) {
      desc.set.call(el, val);
    } else {
      el.value = val;
    }

    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
    el.dispatchEvent(new Event('blur', { bubbles: true }));
    return true;
  },

  // ラジオボタン・チェックボックス・ボタンを確実にクリック
  click(el) {
    if (!el) return false;
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    if (el.type === 'radio' || el.type === 'checkbox') el.checked = true;
    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    el.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
    el.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
    return true;
  },

  // 要素が現れるまで待機
  waitFor(selector, timeoutMs = 8000) {
    return new Promise((resolve) => {
      const getEl = () => typeof selector === 'function' ? selector() : document.querySelector(selector);
      const initial = getEl();
      if (initial) return resolve(initial);

      const startTime = Date.now();
      const interval = setInterval(() => {
        const found = getEl();
        if (found) {
          clearInterval(interval);
          resolve(found);
        } else if (Date.now() - startTime > timeoutMs) {
          clearInterval(interval);
          resolve(null);
        }
      }, 250);
    });
  },

  // 画面右下に常駐する操作アシスタントパネルを注入
  injectFloatingPanel(title, onRunCallback) {
    if (document.getElementById('fi-floating-widget')) return;

    const widget = document.createElement('div');
    widget.id = 'fi-floating-widget';
    widget.style.cssText = `
      position: fixed;
      bottom: 25px;
      right: 25px;
      background: white;
      border: 3px solid #1a5276;
      border-radius: 12px;
      box-shadow: 0 8px 30px rgba(0,0,0,0.3);
      z-index: 2147483647;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Hiragino Kaku Gothic ProN", Meiryo, sans-serif;
      width: 330px;
      overflow: hidden;
      line-height: 1.4;
    `;

    widget.innerHTML = `
      <style>
        #fi-floating-widget * { box-sizing: border-box; }
        .fi-head { background: linear-gradient(135deg, #1a5276, #2980b9); color: white; padding: 10px 14px; display: flex; justify-content: space-between; align-items: center; font-size: 13px; font-weight: bold; }
        .fi-body { padding: 12px; font-size: 12px; color: #333; }
        .fi-box { background: #eaf2f8; padding: 8px 10px; border-radius: 6px; margin-bottom: 10px; font-size: 11px; }
        .fi-btn-run { width: 100%; background: #27ae60; color: white; border: none; padding: 10px; border-radius: 6px; font-weight: bold; font-size: 13px; cursor: pointer; transition: background 0.2s; box-shadow: 0 2px 6px rgba(39,174,96,0.4); }
        .fi-btn-run:hover { background: #219653; }
        .fi-msg { margin-top: 8px; font-size: 11px; text-align: center; font-weight: bold; min-height: 18px; color: #27ae60; }
      </style>
      <div class="fi-head">
        <span>🛡️ 火災保険アシスタント [${title}]</span>
        <button style="background:none;border:none;color:white;cursor:pointer;font-size:16px;font-weight:bold;" onclick="this.closest('#fi-floating-widget').remove()">×</button>
      </div>
      <div class="fi-body">
        <div class="fi-box">
          <strong>物件:</strong> 奈良県大和郡山市箕山町6-27<br>
          <strong>建物:</strong> 木造H構造 / 築26年(2000年9月)<br>
          <strong>補償:</strong> 建物1,500万 / 家財500万 (水災除外)
        </div>
        <button class="fi-btn-run" id="fi-run-btn">⚡ このページに一括自動入力する</button>
        <div class="fi-msg" id="fi-status-msg">待機中</div>
      </div>
    `;

    document.body.appendChild(widget);

    const btn = widget.querySelector('#fi-run-btn');
    const msg = widget.querySelector('#fi-status-msg');

    btn.addEventListener('click', async () => {
      btn.disabled = true;
      btn.innerText = '自動入力中...';
      msg.style.color = '#2980b9';
      msg.innerText = '入力処理を実行中...';

      try {
        await onRunCallback((statusText) => {
          msg.innerText = statusText;
        });
        msg.style.color = '#27ae60';
        msg.innerText = '✓ 自動入力が完了しました！';
      } catch (err) {
        console.error(err);
        msg.style.color = '#c0392b';
        msg.innerText = '一部項目の入力に失敗しました';
      } finally {
        btn.disabled = false;
        btn.innerText = '⚡ 再度自動入力する';
      }
    });
  }
};
