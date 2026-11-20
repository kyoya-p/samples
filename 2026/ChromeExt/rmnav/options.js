(function () {
  const API_KEYS_STORAGE = "rmnav_api_keys";
  const SETTINGS_STORAGE = "rmnav_settings";

  function maskKey(k) {
    if (!k) return "";
    if (k.length <= 8) return "****";
    return k.slice(0, 4) + "..." + k.slice(-4);
  }

  async function loadSettings() {
    const data = await chrome.storage.local.get(SETTINGS_STORAGE);
    const settings = data[SETTINGS_STORAGE] || {};
    const mode = settings.openMode || "popup";
    document.querySelectorAll('input[name="openMode"]').forEach((el) => {
      el.checked = el.value === mode;
      el.onchange = async () => {
        if (el.checked) {
          await chrome.storage.local.set({ [SETTINGS_STORAGE]: { ...settings, openMode: el.value } });
          document.getElementById("mode-msg").textContent = "保存しました: " + (el.value === "popup" ? "ポップアップ" : "通常タブ");
        }
      };
    });
  }

  async function renderKeys() {
    const data = await chrome.storage.local.get(API_KEYS_STORAGE);
    const map = data[API_KEYS_STORAGE] || {};
    const tbody = document.getElementById("key-tbody");
    const entries = Object.entries(map);
    if (entries.length === 0) {
      tbody.innerHTML = '<tr><td colspan="3" style="color:#94a3b8;text-align:center;">登録された API キーはありません</td></tr>';
      return;
    }
    tbody.innerHTML = "";
    entries.forEach(([url, key]) => {
      const tr = document.createElement("tr");
      const tdUrl = document.createElement("td");
      tdUrl.textContent = url;
      const tdKey = document.createElement("td");
      tdKey.textContent = maskKey(key);
      tdKey.style.fontFamily = "monospace";
      const tdOp = document.createElement("td");
      const btn = document.createElement("button");
      btn.className = "btn btn-del";
      btn.textContent = "削除";
      btn.onclick = async () => {
        const cur = (await chrome.storage.local.get(API_KEYS_STORAGE))[API_KEYS_STORAGE] || {};
        delete cur[url];
        await chrome.storage.local.set({ [API_KEYS_STORAGE]: cur });
        renderKeys();
      };
      tdOp.appendChild(btn);
      tr.appendChild(tdUrl);
      tr.appendChild(tdKey);
      tr.appendChild(tdOp);
      tbody.appendChild(tr);
    });
  }

  document.getElementById("btn-save-key").onclick = async () => {
    const url = document.getElementById("inp-url").value.trim().replace(/\/$/, "");
    const key = document.getElementById("inp-key").value.trim();
    const msgEl = document.getElementById("key-msg");
    if (!url || !key) {
      msgEl.className = "msg err";
      msgEl.textContent = "Base URL と API Key を両方入力してください。";
      return;
    }
    const data = await chrome.storage.local.get(API_KEYS_STORAGE);
    const map = data[API_KEYS_STORAGE] || {};
    map[url] = key;
    await chrome.storage.local.set({ [API_KEYS_STORAGE]: map });
    document.getElementById("inp-key").value = "";
    msgEl.className = "msg";
    msgEl.textContent = "保存しました: " + url;
    renderKeys();
  };

  document.getElementById("btn-test-key").onclick = async () => {
    const url = document.getElementById("inp-url").value.trim().replace(/\/$/, "");
    const key = document.getElementById("inp-key").value.trim();
    const msgEl = document.getElementById("key-msg");
    if (!url) {
      msgEl.className = "msg err";
      msgEl.textContent = "Base URL を入力してください。";
      return;
    }
    msgEl.className = "msg";
    msgEl.textContent = "接続確認中...";
    try {
      const headers = { Accept: "application/json" };
      if (key) headers["X-Redmine-API-Key"] = key;
      const res = await fetch(url + "/users/current.json", { headers, credentials: key ? "omit" : "include" });
      if (res.ok) {
        const json = await res.json();
        const login = json.user ? json.user.login : "(unknown)";
        msgEl.className = "msg";
        msgEl.textContent = "接続成功 (HTTP 200) — login: " + login;
      } else {
        msgEl.className = "msg err";
        msgEl.textContent = "接続失敗: HTTP " + res.status;
      }
    } catch (e) {
      msgEl.className = "msg err";
      msgEl.textContent = "接続エラー: " + (e && e.message ? e.message : e);
    }
  };

  loadSettings();
  renderKeys();
})();
