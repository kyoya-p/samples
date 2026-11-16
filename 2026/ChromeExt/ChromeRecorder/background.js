// 🤖 AI: 拡張アイコンのクリックで録画ページをタブで開く(既に開いていればそのタブへ移動)
chrome.action.onClicked.addListener(async () => {
  const url = chrome.runtime.getURL("recorder.html");
  const [tab] = await chrome.tabs.query({ url });
  if (tab) {
    await chrome.tabs.update(tab.id, { active: true });
    await chrome.windows.update(tab.windowId, { focused: true });
  } else {
    await chrome.tabs.create({ url });
  }
});
