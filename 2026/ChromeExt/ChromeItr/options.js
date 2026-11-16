// 🤖 AI 生成: 設定の保存
const DEFAULTS = { engine: 'auto', language: 'auto', translateTo: 'ja', whisperModel: 'onnx-community/whisper-base' };
const $ = (id) => document.getElementById(id);

chrome.storage.local.get(DEFAULTS).then((s) => {
  $('engine').value = s.engine;
  $('language').value = s.language;
  $('translateTo').value = s.translateTo;
  $('model').value = s.whisperModel;
});

$('save').addEventListener('click', async () => {
  await chrome.storage.local.set({
    engine: $('engine').value,
    language: $('language').value,
    translateTo: $('translateTo').value,
    whisperModel: $('model').value.trim() || DEFAULTS.whisperModel,
  });
  $('msg').textContent = '保存済み';
  setTimeout(() => { $('msg').textContent = ''; }, 1500);
});
