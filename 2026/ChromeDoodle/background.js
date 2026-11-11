/**
 * ChromeDoodle - Background Service Worker
 */

// Toggle ChromeDoodle when extension icon is clicked
chrome.action.onClicked.addListener(async (tab) => {
  if (!tab || !tab.id || tab.url?.startsWith('chrome://') || tab.url?.startsWith('chrome-extension://')) {
    return;
  }

  try {
    // Try sending toggle message to content script
    const response = await chrome.tabs.sendMessage(tab.id, { type: 'CHROME_DOODLE_TOGGLE' });
    updateBadge(tab.id, response?.active);
  } catch (err) {
    // If content script was not injected yet, inject it dynamically
    try {
      await chrome.scripting.executeScript({
        target: { tabId: tab.id },
        files: ['content.js']
      });
      const response = await chrome.tabs.sendMessage(tab.id, { type: 'CHROME_DOODLE_TOGGLE' });
      updateBadge(tab.id, response?.active);
    } catch (injectErr) {
      console.warn('[ChromeDoodle] Unable to inject content script:', injectErr);
    }
  }
});

// Update badge when state changes
function updateBadge(tabId, isActive) {
  if (isActive) {
    chrome.action.setBadgeText({ tabId, text: 'ON' });
    chrome.action.setBadgeBackgroundColor({ tabId, color: '#6366f1' });
  } else {
    chrome.action.setBadgeText({ tabId, text: '' });
  }
}

// Handle runtime messages from content script
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'CHROME_DOODLE_CAPTURE_TAB') {
    const windowId = sender.tab ? sender.tab.windowId : chrome.windows.WINDOW_ID_CURRENT;
    chrome.tabs.captureVisibleTab(windowId, { format: 'png' }, (dataUrl) => {
      if (chrome.runtime.lastError) {
        sendResponse({ success: false, error: chrome.runtime.lastError.message });
      } else {
        sendResponse({ success: true, dataUrl });
      }
    });
    return true; // Keep message channel open for async response
  }

  if (message.type === 'CHROME_DOODLE_STATE_CHANGED') {
    if (sender.tab && sender.tab.id) {
      updateBadge(sender.tab.id, message.active);
    }
    sendResponse({ ok: true });
    return false;
  }
});
