/**
 * ChromeDoodle - Content Script
 * Webページに描画・注釈、図形、テキスト編集、暗転クロップ、クリップボード連携を行うChrome拡張機能
 */

(function () {
  // Prevent duplicate injection
  if (window.__CHROME_DOODLE_INSTANCE__) {
    return;
  }

  // --- State & Variables ---
  let isActive = false;
  let currentTool = 'freehand'; // 'select' | 'freehand' | 'line' | 'arrow' | 'rect' | 'ellipse' | 'text' | 'crop'
  let currentColor = '#ef4444'; // Red default
  let currentStrokeWidth = 4;
  let currentFillMode = 'none'; // 'none' | 'semi' | 'solid'
  let currentFontSize = 24;
  let currentStamp = '';
  const DEFAULT_STAMPS = ['Remove', 'Append', 'Move', 'Larger', 'Smaller', 'OK', 'NG', 'Fix', 'Check', '?', 'Tell me'];
  function loadStamps() {
    try {
      const s = JSON.parse(localStorage.getItem('cd.stamps') || 'null');
      if (Array.isArray(s) && s.length) {
        for (const d of DEFAULT_STAMPS) if (!s.includes(d)) s.push(d);
        return s;
      }
    } catch (e) {}
    return DEFAULT_STAMPS.slice();
  }
  function saveStamps(list) {
    try { localStorage.setItem('cd.stamps', JSON.stringify(list)); } catch (e) {}
  }

  let objects = []; // Array of drawn objects
  let undoStack = [];
  let redoStack = [];

  let selectedObject = null;
  let copiedDoodleObject = null;
  let isDraggingSelected = false;
  let isResizingSelected = false;
  let activeResizeHandle = null;
  let framingMode = false; // AI: Ctrl(Cmd)押下中 true。画像選択中は隠れ部分を半透明表示
  let framingRect = null;  // AI: フレーミング用ドラッグ矩形 {x0,y0,x1,y1}
  let isFraming = false; // AI: Ctrl+ハンドルドラッグ = 貼付画像のフレーミング(表示範囲のトリミング。拡縮しない)
  let dragStartPos = { x: 0, y: 0 };
  let selectedObjectInitialState = null;

  // Drawing state
  let isDrawing = false;
  let draftObject = null;

  // Crop state
  let cropRect = null; // { x, y, w, h }
  let isCropping = false;
  let cropDragHandle = null; // 'create' | 'move' | 'nw' | 'ne' | 'se' | 'sw' | 'n' | 'e' | 's' | 'w'
  let cropStartPos = { x: 0, y: 0 };
  let cropInitialRect = null;

  // Eyedropper state
  let eyedropperHoverPos = null;
  let eyedropperSnapshotCanvas = null;
  let eyedropperSnapshotCtx = null;
  let previousTool = 'freehand';

  // Full-page scroll capture & Desktop capture state
  let isScrollCapturing = false;
  let scrollCaptureCancelled = false;
  let lastTabCaptureTs = 0;
  let pendingCaptureBlob = null;
  let pendingCaptureObjectUrl = null;
  let currentPageZoom = 1;

  // DOM Elements inside ShadowRoot
  let hostEl = null;
  let shadow = null;
  let canvasWrapEl = null;
  let canvas = null;
  let ctx = null;
  let toolbarEl = null;
  let htmlHoverBoxEl = null;
  let cropOverlayEl = null;
  let textEditorEl = null;
  let toastEl = null;
  let captureModalEl = null;

  // HTML edit mode hover state
  let hoveredHtmlEl = null;
  let lastHtmlMousePos = null;

  // --- Palette presets (9 colors + eyedropper + custom picker) ---
  const PRESET_COLORS = [
    '#ef4444', // Red
    '#f97316', // Orange
    '#eab308', // Yellow
    '#22c55e', // Green
    '#06b6d4', // Cyan
    '#3b82f6', // Blue
    '#a855f7', // Purple
    '#ffffff', // White
    '#1e293b'  // Dark/Black
  ];

  const STROKE_WIDTHS = [2, 4, 8, 14];
  const FONT_SIZES = [18, 24, 32, 48];

  // --- Initialize UI & Canvas in Shadow DOM ---
  function init() {
    hostEl = document.createElement('div');
    hostEl.id = 'chrome-doodle-host';
    hostEl.style.cssText = 'all: initial; position: absolute; top: 0; left: 0; z-index: 2147483647;';
    
    shadow = hostEl.attachShadow({ mode: 'open' });

    // Stylesheet inside shadow root
    const style = document.createElement('style');
    style.textContent = getShadowStyles();
    shadow.appendChild(style);

    // Canvas container
    canvasWrapEl = document.createElement('div');
    canvasWrapEl.className = 'cd-canvas-wrap';

    canvas = document.createElement('canvas');
    canvas.className = 'cd-main-canvas';
    ctx = canvas.getContext('2d');
    canvasWrapEl.appendChild(canvas);
    shadow.appendChild(canvasWrapEl);

    // Text Editor overlay (hidden by default)
    textEditorEl = document.createElement('textarea');
    textEditorEl.className = 'cd-text-editor';
    textEditorEl.placeholder = 'テキストを入力 (Shift+Enterで改行、Escで完了)';
    shadow.appendChild(textEditorEl);

    // HTML Edit Mode Hover Highlight
    htmlHoverBoxEl = document.createElement('div');
    htmlHoverBoxEl.className = 'cd-html-hover-box';
    shadow.appendChild(htmlHoverBoxEl);

    // Crop UI Container
    cropOverlayEl = document.createElement('div');
    cropOverlayEl.className = 'cd-crop-overlay';
    shadow.appendChild(cropOverlayEl);

    // Toolbar
    toolbarEl = document.createElement('div');
    toolbarEl.className = 'cd-toolbar';
    toolbarEl.innerHTML = buildToolbarHTML();
    shadow.appendChild(toolbarEl);

    // Capture Result / Fallback Modal
    captureModalEl = document.createElement('div');
    captureModalEl.className = 'cd-capture-modal';
    shadow.appendChild(captureModalEl);

    // Toast Container
    toastEl = document.createElement('div');
    toastEl.className = 'cd-toast';
    shadow.appendChild(toastEl);

    document.documentElement.appendChild(hostEl);

    // Bind event handlers
    setupCanvasEvents();
    setupToolbarEvents();
    setupKeyboardEvents();
    setupPasteHandler();
    setupResizeHandler();
    setupHtmlEditHighlightEvents();

    applyPageZoom(currentPageZoom);
    syncPageZoom();
    resizeCanvas();
  }

  function applyPageZoom(zoom) {
    const z = (typeof zoom === 'number' && isFinite(zoom) && zoom > 0.1) ? zoom : 1;
    currentPageZoom = z;
    const inv = 1 / z;
    if (hostEl) {
      hostEl.style.setProperty('--cd-inv-zoom', String(inv));
    }
    clampToolbarToViewport();
    if (currentTool === 'crop' && cropRect) {
      updateCropOverlay();
    }
    if (currentTool === 'edit') {
      renderHtmlHoverHighlight();
    }
  }

  function syncPageZoom() {
    if (typeof chrome !== 'undefined' && chrome.runtime?.sendMessage) {
      try {
        chrome.runtime.sendMessage({ type: 'CHROME_DOODLE_GET_ZOOM' }, (res) => {
          if (chrome.runtime?.lastError) return;
          if (res && typeof res.zoom === 'number') {
            applyPageZoom(res.zoom);
          }
        });
      } catch (_) {}
    }
  }

  function clampToolbarToViewport() {
    if (!toolbarEl || !toolbarEl.classList.contains('positioned')) return;
    const inv = 1 / (currentPageZoom || 1);
    const margin = 8 * inv;
    const rect = toolbarEl.getBoundingClientRect();
    const maxX = Math.max(margin, window.innerWidth - rect.width - margin);
    const maxY = Math.max(margin, window.innerHeight - rect.height - margin);
    if (rect.right > window.innerWidth - margin || rect.left < margin || rect.bottom > window.innerHeight - margin || rect.top < margin) {
      const newX = toolbarEl.classList.contains('minimized') ? 0 : Math.max(margin, Math.min(maxX, rect.left));
      const newY = Math.max(margin, Math.min(maxY, rect.top));
      toolbarEl.style.right = 'auto';
      toolbarEl.style.left = `${newX}px`;
      toolbarEl.style.top = `${newY}px`;
    }
  }

  // --- High DPI Canvas Resizing ---
  function resizeCanvas() {
    if (!canvas) return;
    const dpr = window.devicePixelRatio || 1;
    const width = window.innerWidth;
    const height = window.innerHeight;

    canvas.width = Math.round(width * dpr);
    canvas.height = Math.round(height * dpr);
    canvas.style.width = width + 'px';
    canvas.style.height = height + 'px';

    ctx.scale(dpr, dpr);
    render();
  }

  function setupResizeHandler() {
    window.addEventListener('resize', () => {
      if (isActive) {
        syncPageZoom();
        resizeCanvas();
        clampToolbarToViewport();
        if (currentTool === 'edit') {
          renderHtmlHoverHighlight();
        }
      }
    });
  }

  // --- Activation / Toggle ---
  function setActive(active) {
    isActive = active;
    if (active) {
      if (!hostEl) {
        init();
      }
      hostEl.style.display = 'block';
      document.body.classList.add('cd-doodle-active-body');
      if (currentTool === 'edit') {
        document.designMode = 'on';
        if (canvasWrapEl) canvasWrapEl.style.pointerEvents = 'none';
        if (canvas) canvas.style.pointerEvents = 'none';
      }
      syncPageZoom();
      resizeCanvas();
      notifyState(true);
    } else {
      clearHtmlHoverHighlight();
      if (document.designMode === 'on') {
        document.designMode = 'off';
      }
      if (canvasWrapEl) canvasWrapEl.style.pointerEvents = 'auto';
      if (canvas) canvas.style.pointerEvents = 'auto';
      if (hostEl) {
        hostEl.style.display = 'none';
        closeTextEditor();
      }
      document.body.classList.remove('cd-doodle-active-body');
      notifyState(false);
    }
  }

  function notifyState(active) {
    try {
      if (typeof chrome !== 'undefined' && chrome.runtime?.sendMessage) {
        chrome.runtime.sendMessage({ type: 'CHROME_DOODLE_STATE_CHANGED', active });
      }
    } catch (_) {}
  }

  // --- Object Cloning & Restoration (preserving image references & dataUrl) ---
  function cloneDoodleObject(obj) {
    if (!obj) return null;
    const copy = { ...obj };
    if (obj.points) {
      copy.points = obj.points.map(p => ({ x: p.x, y: p.y }));
    }
    if (obj.img) {
      copy.img = obj.img;
    }
    if (obj.dataUrl) {
      copy.dataUrl = obj.dataUrl;
    }
    return copy;
  }

  function restoreDoodleObject(target, source) {
    if (!target || !source) return;
    const imgRef = target.img || source.img;
    const dataUrlRef = target.dataUrl || source.dataUrl;
    Object.assign(target, cloneDoodleObject(source));
    if (imgRef) target.img = imgRef;
    if (dataUrlRef) target.dataUrl = dataUrlRef;
  }

  // --- History & Undo / Redo System ---
  function pushAction(action) {
    undoStack.push(action);
    redoStack = []; // Clear redo stack on new action
    updateUndoRedoButtons();
    render();
  }

  function undo() {
    if (undoStack.length === 0) return;
    const action = undoStack.pop();
    redoStack.push(action);

    if (action.type === 'ADD') {
      const idx = objects.findIndex(o => o.id === action.object.id);
      if (idx !== -1) objects.splice(idx, 1);
      if (selectedObject?.id === action.object.id) selectedObject = null;
    } else if (action.type === 'REMOVE') {
      const restored = cloneDoodleObject(action.object);
      objects.splice(action.index, 0, restored);
      selectedObject = restored;
    } else if (action.type === 'MODIFY') {
      const target = objects.find(o => o.id === action.objectId);
      if (target) {
        restoreDoodleObject(target, action.before);
      }
    } else if (action.type === 'CLEAR') {
      objects = action.prevObjects.map(o => cloneDoodleObject(o));
    }

    updateUndoRedoButtons();
    render();
    showToast('↩️ 元に戻しました', 'info');
  }

  function redo() {
    if (redoStack.length === 0) return;
    const action = redoStack.pop();
    undoStack.push(action);

    if (action.type === 'ADD') {
      const added = cloneDoodleObject(action.object);
      objects.push(added);
      selectedObject = added;
    } else if (action.type === 'REMOVE') {
      const idx = objects.findIndex(o => o.id === action.object.id);
      if (idx !== -1) objects.splice(idx, 1);
      if (selectedObject?.id === action.object.id) selectedObject = null;
    } else if (action.type === 'MODIFY') {
      const target = objects.find(o => o.id === action.objectId);
      if (target) {
        restoreDoodleObject(target, action.after);
      }
    } else if (action.type === 'CLEAR') {
      objects = [];
      selectedObject = null;
    }

    updateUndoRedoButtons();
    render();
    showToast('↪️ やり直しました', 'info');
  }

  function clearAll() {
    if (objects.length === 0) return;
    pushAction({
      type: 'CLEAR',
      prevObjects: objects.map(o => cloneDoodleObject(o))
    });
    objects = [];
    selectedObject = null;
    cropRect = null;
    updateCropOverlay();
    render();
    showToast('🗑️ すべての描画を消去しました', 'info');
  }

  function updateUndoRedoButtons() {
    const undoBtn = shadow.getElementById('cd-btn-undo');
    const redoBtn = shadow.getElementById('cd-btn-redo');
    if (undoBtn) undoBtn.classList.toggle('disabled', undoStack.length === 0);
    if (redoBtn) redoBtn.classList.toggle('disabled', redoStack.length === 0);
  }

  // --- Rendering Engine ---
  function render() {
    if (!ctx) return;
    const width = window.innerWidth;
    const height = window.innerHeight;

    ctx.clearRect(0, 0, width, height);

    // 1. Render all committed objects
    for (const obj of objects) {
      drawObject(ctx, obj);
    }

    // 2. Render draft object currently being drawn
    if (draftObject) {
      drawObject(ctx, draftObject);
    }

    // 3. Render selection outline if an object is selected
    if (selectedObject && currentTool === 'select') {
      drawSelectionBox(ctx, selectedObject);
    }

    // 4. Render crop overlay (mask & border) if crop mode is active
    if (currentTool === 'crop' && cropRect) {
      drawCropMask(ctx, cropRect, width, height);
    }

    // 5. Render precision crosshair target if eyedropper mode is active
    if (currentTool === 'eyedropper' && eyedropperHoverPos) {
      drawEyedropperTarget(ctx, eyedropperHoverPos.x, eyedropperHoverPos.y);
    }

    updateCropOverlay();
  }

  function drawObject(targetCtx, obj) {
    targetCtx.save();

    switch (obj.type) {
      case 'stroke':
        drawStroke(targetCtx, obj);
        break;
      case 'line':
        drawLine(targetCtx, obj);
        break;
      case 'arrow':
        drawArrow(targetCtx, obj);
        break;
      case 'rect':
        drawRect(targetCtx, obj);
        break;
      case 'ellipse':
        drawEllipse(targetCtx, obj);
        break;
      case 'text':
        drawText(targetCtx, obj);
        break;
      case 'image':
        drawImageObject(targetCtx, obj);
        break;
    }

    targetCtx.restore();
  }

  function drawStroke(c, obj) {
    if (!obj.points || obj.points.length === 0) return;
    c.strokeStyle = obj.color;
    c.lineWidth = obj.width;
    c.lineCap = 'round';
    c.lineJoin = 'round';

    c.beginPath();
    c.moveTo(obj.points[0].x, obj.points[0].y);

    if (obj.points.length === 1) {
      c.lineTo(obj.points[0].x + 0.1, obj.points[0].y + 0.1);
    } else {
      for (let i = 1; i < obj.points.length - 1; i++) {
        const xc = (obj.points[i].x + obj.points[i + 1].x) / 2;
        const yc = (obj.points[i].y + obj.points[i + 1].y) / 2;
        c.quadraticCurveTo(obj.points[i].x, obj.points[i].y, xc, yc);
      }
      const last = obj.points[obj.points.length - 1];
      c.lineTo(last.x, last.y);
    }
    c.stroke();
  }

  function drawLine(c, obj) {
    c.strokeStyle = obj.color;
    c.lineWidth = obj.width;
    c.lineCap = 'round';

    c.beginPath();
    c.moveTo(obj.x1, obj.y1);
    c.lineTo(obj.x2, obj.y2);
    c.stroke();
  }

  function drawArrow(c, obj) {
    const dx = obj.x2 - obj.x1;
    const dy = obj.y2 - obj.y1;
    const dist = Math.hypot(dx, dy);
    if (dist < 2) return;

    const angle = Math.atan2(dy, dx);
    const width = obj.width || 4;
    const headLen = Math.max(16, width * 3.5);
    const headAngle = Math.PI / 6.5; // ~27.7 degrees

    c.save();
    c.strokeStyle = obj.color;
    c.fillStyle = obj.color;
    c.lineCap = 'round';
    c.lineJoin = 'round';

    // Shaft stops behind arrowhead so line stroke cap never pokes through
    const shaftStop = Math.min(dist * 0.85, headLen * 0.7);
    const stopX = obj.x2 - shaftStop * Math.cos(angle);
    const stopY = obj.y2 - shaftStop * Math.sin(angle);

    c.lineWidth = width;
    c.beginPath();
    c.moveTo(obj.x1, obj.y1);
    c.lineTo(stopX, stopY);
    c.stroke();

    // Arrowhead polygon
    const p1X = obj.x2 - headLen * Math.cos(angle - headAngle);
    const p1Y = obj.y2 - headLen * Math.sin(angle - headAngle);
    const p2X = obj.x2 - headLen * Math.cos(angle + headAngle);
    const p2Y = obj.y2 - headLen * Math.sin(angle + headAngle);
    const backX = obj.x2 - (headLen * 0.72) * Math.cos(angle);
    const backY = obj.y2 - (headLen * 0.72) * Math.sin(angle);

    c.beginPath();
    c.moveTo(obj.x2, obj.y2);
    c.lineTo(p1X, p1Y);
    c.lineTo(backX, backY);
    c.lineTo(p2X, p2Y);
    c.closePath();
    c.fill();

    c.lineWidth = Math.max(1.5, width * 0.3);
    c.stroke();

    c.restore();
  }

  function drawRect(c, obj) {
    const x = Math.min(obj.x, obj.x + obj.w);
    const y = Math.min(obj.y, obj.y + obj.h);
    const w = Math.abs(obj.w);
    const h = Math.abs(obj.h);

    if (obj.fillColor) {
      c.fillStyle = obj.fillColor;
      c.fillRect(x, y, w, h);
    }

    if (obj.strokeColor && obj.width > 0) {
      c.strokeStyle = obj.strokeColor;
      c.lineWidth = obj.width;
      c.strokeRect(x, y, w, h);
    }
  }

  function drawEllipse(c, obj) {
    const cx = obj.cx;
    const cy = obj.cy;
    const rx = Math.abs(obj.rx);
    const ry = Math.abs(obj.ry);

    c.beginPath();
    c.ellipse(cx, cy, Math.max(0.1, rx), Math.max(0.1, ry), 0, 0, Math.PI * 2);

    if (obj.fillColor) {
      c.fillStyle = obj.fillColor;
      c.fill();
    }

    if (obj.strokeColor && obj.width > 0) {
      c.strokeStyle = obj.strokeColor;
      c.lineWidth = obj.width;
      c.stroke();
    }
  }

  function drawText(c, obj) {
    const fontSize = obj.fontSize || 24;
    c.font = `bold ${fontSize}px sans-serif`;
    c.textBaseline = 'top';

    const lines = (obj.text || '').split('\n');
    const lineHeight = fontSize * 1.25;

    c.fillStyle = obj.color || '#ef4444';

    for (let i = 0; i < lines.length; i++) {
      c.fillText(lines[i], obj.x, obj.y + i * lineHeight);
    }
  }

  function drawImageObject(c, obj) {
    if (obj.img && obj.img.complete && obj.img.naturalWidth > 0) {
      try {
        c.save();
        // 反転: 画像自身の中心で左右/上下ミラー
        if (obj.flipX) { c.translate(2 * obj.x + obj.w, 0); c.scale(-1, 1); }
        if (obj.flipY) { c.translate(0, 2 * obj.y + obj.h); c.scale(1, -1); }
        if (obj.sw > 0 && obj.sh > 0) {
          c.drawImage(obj.img, obj.sx, obj.sy, obj.sw, obj.sh, obj.x, obj.y, obj.w, obj.h);
        } else {
          c.drawImage(obj.img, obj.x, obj.y, obj.w, obj.h);
        }
        c.restore();
      } catch (_) {}
    } else if (obj.dataUrl) {
      if (!obj._loading) {
        obj._loading = true;
        const newImg = new Image();
        newImg.onload = () => {
          obj.img = newImg;
          obj._loading = false;
          render();
        };
        newImg.src = obj.dataUrl;
      }
    }
  }

  // --- Darkened Crop Mask ---
  function drawCropMask(c, crop, screenW, screenH) {
    const x = Math.min(crop.x, crop.x + crop.w);
    const y = Math.min(crop.y, crop.y + crop.h);
    const w = Math.abs(crop.w);
    const h = Math.abs(crop.h);

    // Darken outside the crop frame (clean cut-out, NO dashed/solid frame)
    c.fillStyle = 'rgba(0, 0, 0, 0.62)';

    // Top
    c.fillRect(0, 0, screenW, y);
    // Bottom
    c.fillRect(0, y + h, screenW, screenH - (y + h));
    // Left
    c.fillRect(0, y, x, h);
    // Right
    c.fillRect(x + w, y, screenW - (x + w), h);
  }

  function drawCircleHandle(c, x, y, radius = 7) {
    c.save();
    c.shadowColor = 'rgba(0, 0, 0, 0.45)';
    c.shadowBlur = 5;
    c.shadowOffsetX = 0;
    c.shadowOffsetY = 1;

    c.fillStyle = '#ffffff';
    c.beginPath();
    c.arc(x, y, radius, 0, Math.PI * 2);
    c.fill();

    c.shadowColor = 'transparent';
    c.strokeStyle = '#2563eb';
    c.lineWidth = 2.2;
    c.stroke();
    c.restore();
  }

  // --- Eyedropper Sampling & Precision Crosshair Target ---
  function rgbToHex(r, g, b) {
    const toHex = (n) => {
      const hex = Math.max(0, Math.min(255, n)).toString(16);
      return hex.length === 1 ? '0' + hex : hex;
    };
    return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
  }

  function getPixelColorAt(x, y) {
    const dpr = window.devicePixelRatio || 1;
    const px = Math.round(x * dpr);
    const py = Math.round(y * dpr);

    if (eyedropperSnapshotCtx) {
      try {
        const pixel = eyedropperSnapshotCtx.getImageData(px, py, 1, 1).data;
        if (pixel[3] > 0) {
          return rgbToHex(pixel[0], pixel[1], pixel[2]);
        }
      } catch (_) {}
    }

    if (ctx) {
      try {
        const pixel = ctx.getImageData(px, py, 1, 1).data;
        if (pixel[3] > 0) {
          return rgbToHex(pixel[0], pixel[1], pixel[2]);
        }
      } catch (_) {}
    }

    const hit = hitTest(x, y);
    if (hit) {
      if (hit.color) return hit.color;
      if (hit.strokeColor) return hit.strokeColor;
      if (hit.fillColor) return hit.fillColor;
    }
    return currentColor;
  }

  async function prepareEyedropperSnapshot() {
    const dpr = window.devicePixelRatio || 1;
    const offscreen = document.createElement('canvas');
    offscreen.width = Math.round(window.innerWidth * dpr);
    offscreen.height = Math.round(window.innerHeight * dpr);
    const octx = offscreen.getContext('2d', { willReadFrequently: true });
    octx.scale(dpr, dpr);

    if (typeof chrome !== 'undefined' && chrome.runtime?.sendMessage) {
      try {
        if (hostEl) hostEl.style.visibility = 'hidden';
        await new Promise(r => setTimeout(r, 40));
        const response = await chrome.runtime.sendMessage({ type: 'CHROME_DOODLE_CAPTURE_TAB' });
        if (hostEl) hostEl.style.visibility = 'visible';

        if (response && response.success && response.dataUrl) {
          const bgImg = await loadImage(response.dataUrl);
          octx.drawImage(bgImg, 0, 0, window.innerWidth, window.innerHeight);
        }
      } catch (_) {
        if (hostEl) hostEl.style.visibility = 'visible';
      }
    }

    for (const obj of objects) {
      drawObject(octx, obj);
    }

    eyedropperSnapshotCanvas = offscreen;
    eyedropperSnapshotCtx = octx;
    render();
  }

  function drawEyedropperTarget(c, x, y) {
    const color = getPixelColorAt(x, y) || currentColor;

    c.save();
    // 1. Crosshair lines (high contrast dual-stroke)
    const len = 14;
    const gap = 3;

    // Outer white halo
    c.strokeStyle = '#ffffff';
    c.lineWidth = 3.5;
    c.beginPath();
    c.moveTo(x, y - gap); c.lineTo(x, y - gap - len);
    c.moveTo(x, y + gap); c.lineTo(x, y + gap + len);
    c.moveTo(x - gap, y); c.lineTo(x - gap - len, y);
    c.moveTo(x + gap, y); c.lineTo(x + gap + len, y);
    c.stroke();

    // Inner dark hairline
    c.strokeStyle = '#0f172a';
    c.lineWidth = 1.2;
    c.stroke();

    // Center precision dot
    c.fillStyle = '#0f172a';
    c.beginPath();
    c.arc(x, y, 1, 0, Math.PI * 2);
    c.fill();

    // 2. Color Loupe & Badge offset from cursor
    let badgeX = x + 24;
    let badgeY = y - 24;
    const badgeR = 15;

    // Boundary check so badge stays on-screen
    if (badgeX + 50 > window.innerWidth) badgeX = x - 50;
    if (badgeY - badgeR < 10) badgeY = y + 36;

    // Outer shadow
    c.shadowColor = 'rgba(0, 0, 0, 0.45)';
    c.shadowBlur = 8;
    c.shadowOffsetX = 0;
    c.shadowOffsetY = 2;

    // Loupe color circle
    c.beginPath();
    c.arc(badgeX, badgeY, badgeR, 0, Math.PI * 2);
    c.fillStyle = color;
    c.fill();

    // Loupe white border
    c.shadowColor = 'transparent';
    c.strokeStyle = '#ffffff';
    c.lineWidth = 2.5;
    c.stroke();

    // Hex text label
    const text = color.toUpperCase();
    c.font = 'bold 11px monospace';
    const tw = c.measureText(text).width;
    const tx = Math.round(badgeX - tw / 2);
    const ty = Math.round(badgeY + badgeR + 15);

    c.fillStyle = 'rgba(15, 23, 42, 0.88)';
    c.beginPath();
    if (c.roundRect) {
      c.roundRect(tx - 6, ty - 11, tw + 12, 16, 4);
    } else {
      c.rect(tx - 6, ty - 11, tw + 12, 16);
    }
    c.fill();

    c.fillStyle = '#f8fafc';
    c.textBaseline = 'middle';
    c.textAlign = 'left';
    c.fillText(text, tx, ty - 2);

    c.restore();
  }

  // --- 貼付画像フレーミング補助 ---
  // 元画像全体(隠れ部分含む)の画面上の矩形
  function getImageFullRect(obj) {
    if (!obj || obj.type !== 'image' || !obj.img || !(obj.img.naturalWidth > 0) || !(obj.sw > 0) || !(obj.sh > 0)) return null;
    const kx = obj.w / obj.sw;
    const ky = obj.h / obj.sh;
    const w = obj.img.naturalWidth * kx;
    const h = obj.img.naturalHeight * ky;
    let x = obj.x - obj.sx * kx;
    let y = obj.y - obj.sy * ky;
    if (obj.flipX) x = 2 * obj.x + obj.w - (x + w);
    if (obj.flipY) y = 2 * obj.y + obj.h - (y + h);
    return { x, y, w, h };
  }

  // ドラッグ中矩形を元画像全体の範囲内に正規化
  function getFramingRectNormalized() {
    const obj = selectedObject;
    const fr = getImageFullRect(obj);
    if (!framingRect || !fr) return null;
    const x1 = Math.max(fr.x, Math.min(framingRect.x0, framingRect.x1));
    const x2 = Math.min(fr.x + fr.w, Math.max(framingRect.x0, framingRect.x1));
    const y1 = Math.max(fr.y, Math.min(framingRect.y0, framingRect.y1));
    const y2 = Math.min(fr.y + fr.h, Math.max(framingRect.y0, framingRect.y1));
    return { x: x1, y: y1, w: x2 - x1, h: y2 - y1 };
  }

  // ドラッグした矩形を新しいフレーム(表示範囲)として確定
  function applyFramingRect() {
    const obj = selectedObject;
    const r = getFramingRectNormalized();
    framingRect = null;
    if (!obj || obj.type !== 'image' || !r || r.w < 8 || r.h < 8) {
      render();
      return;
    }
    const kx = obj.w / obj.sw;
    const ky = obj.h / obj.sh;
    const toSrcX = (X) => obj.flipX ? obj.sx + obj.sw - (X - obj.x) / kx : obj.sx + (X - obj.x) / kx;
    const toSrcY = (Y) => obj.flipY ? obj.sy + obj.sh - (Y - obj.y) / ky : obj.sy + (Y - obj.y) / ky;
    const sxa = toSrcX(r.x), sxb = toSrcX(r.x + r.w);
    const sya = toSrcY(r.y), syb = toSrcY(r.y + r.h);

    const before = cloneDoodleObject(obj);
    obj.sx = Math.min(sxa, sxb);
    obj.sw = Math.abs(sxb - sxa);
    obj.sy = Math.min(sya, syb);
    obj.sh = Math.abs(syb - sya);
    obj.x = r.x;
    obj.y = r.y;
    obj.w = r.w;
    obj.h = r.h;
    pushAction({
      type: 'MODIFY',
      objectId: obj.id,
      before,
      after: cloneDoodleObject(obj)
    });
  }

  // --- Selection Bounding Box & Handles ---
  function drawSelectionBox(c, obj) {
    if (!obj) return;

    // 1. Line & Arrow: 2 Endpoint Handles (Start & End) - NO dashed border
    if (obj.type === 'line' || obj.type === 'arrow') {
      drawCircleHandle(c, obj.x1, obj.y1, 7.5);
      drawCircleHandle(c, obj.x2, obj.y2, 7.5);
      return;
    }

    // 2. Image: 8 Handles - NO dashed border
    if (obj.type === 'image') {
      // フレーミングモード(Ctrl押下中/ドラッグ中)のみ、フレーム外の元画像を半透明で表示 (画面表示専用でエクスポートには含まれない)
      if ((framingMode || isFraming || framingRect) && obj.img &&obj.img.naturalWidth > 0 && obj.sw > 0 && obj.sh > 0 &&
          (obj.sw < obj.img.naturalWidth - 0.5 || obj.sh < obj.img.naturalHeight - 0.5)) {
        const kx = obj.w / obj.sw;
        const ky = obj.h / obj.sh;
        c.save();
        c.beginPath();
        c.rect(-1e5, -1e5, 2e5, 2e5);
        c.rect(obj.x, obj.y, obj.w, obj.h);
        c.clip('evenodd');
        c.globalAlpha = 0.3;
        if (obj.flipX) { c.translate(2 * obj.x + obj.w, 0); c.scale(-1, 1); }
        if (obj.flipY) { c.translate(0, 2 * obj.y + obj.h); c.scale(1, -1); }
        c.drawImage(obj.img, obj.x - obj.sx * kx, obj.y - obj.sy * ky, obj.img.naturalWidth * kx, obj.img.naturalHeight * ky);
        c.restore();
      }
      // ドラッグ中の新フレーム矩形
      const fRect = getFramingRectNormalized();
      if (fRect && fRect.w > 0 && fRect.h > 0) {
        c.save();
        c.strokeStyle = '#6366f1';
        c.lineWidth = 1.5;
        c.setLineDash([5, 4]);
        c.strokeRect(fRect.x, fRect.y, fRect.w, fRect.h);
        c.restore();
      }

      const handles = getObjectResizeHandles(obj);
      for (const h of handles) {
        drawCircleHandle(c, h.x, h.y, 7.5);
      }

      // 情報ラベル: フレーミング後の元画像pixelサイズ と 元画像比の縦横倍率(%)
      if (obj.img && obj.img.naturalWidth > 0) {
        const dpr = window.devicePixelRatio || 1;
        const srcW = obj.sw > 0 ? obj.sw : obj.img.naturalWidth;
        const srcH = obj.sh > 0 ? obj.sh : obj.img.naturalHeight;
        const pctX = Math.round(Math.abs(obj.w) * dpr / srcW * 100);
        const pctY = Math.round(Math.abs(obj.h) * dpr / srcH * 100);
        const text = `${Math.round(srcW)}×${Math.round(srcH)} px (${pctX}%,${pctY}%)`;
        c.save();
        c.font = 'bold 11px monospace';
        const tw = c.measureText(text).width;
        const bx = Math.max(4, Math.min(window.innerWidth - tw - 16, Math.min(obj.x, obj.x + obj.w)));
        let by = Math.min(obj.y, obj.y + obj.h) - 16;
        if (by < 14) by = Math.max(obj.y, obj.y + obj.h) + 22;
        c.fillStyle = 'rgba(15, 23, 42, 0.88)';
        c.beginPath();
        if (c.roundRect) {
          c.roundRect(bx, by - 11, tw + 12, 16, 4);
        } else {
          c.rect(bx, by - 11, tw + 12, 16);
        }
        c.fill();
        c.fillStyle = '#f8fafc';
        c.textBaseline = 'middle';
        c.textAlign = 'left';
        c.fillText(text, bx + 6, by - 2);
        c.restore();
      }
      return;
    }

    // 3. Rect: 8 Handles around rectangle
    if (obj.type === 'rect') {
      const box = getObjectBoundingBox(obj);
      if (box) {
        c.save();
        c.strokeStyle = 'rgba(99, 102, 241, 0.55)';
        c.lineWidth = 1.2;
        c.setLineDash([4, 4]);
        c.strokeRect(box.x, box.y, box.w, box.h);
        c.restore();
      }
      const handles = getObjectResizeHandles(obj);
      for (const h of handles) {
        drawCircleHandle(c, h.x, h.y, 7.5);
      }
      return;
    }

    // 4. Ellipse: 8 Handles around bounding box
    if (obj.type === 'ellipse') {
      const box = getObjectBoundingBox(obj);
      if (box) {
        c.save();
        c.strokeStyle = 'rgba(99, 102, 241, 0.55)';
        c.lineWidth = 1.2;
        c.setLineDash([4, 4]);
        c.strokeRect(box.x, box.y, box.w, box.h);
        c.restore();
      }
      const handles = getObjectResizeHandles(obj);
      for (const h of handles) {
        drawCircleHandle(c, h.x, h.y, 7.5);
      }
      return;
    }

    // 5. Other objects (text, stroke)
    const box = getObjectBoundingBox(obj);
    if (!box) return;

    c.strokeStyle = '#6366f1';
    c.lineWidth = 1.5;
    c.setLineDash([4, 4]);
    c.strokeRect(box.x - 4, box.y - 4, box.w + 8, box.h + 8);
    c.setLineDash([]);
  }

  function getObjectResizeHandles(obj) {
    if (!obj) return [];

    if (obj.type === 'line' || obj.type === 'arrow') {
      return [
        { id: 'start', x: obj.x1, y: obj.y1, cursor: 'crosshair' },
        { id: 'end',   x: obj.x2, y: obj.y2, cursor: 'crosshair' }
      ];
    }

    if (obj.type === 'image' || obj.type === 'rect') {
      const x1 = Math.min(obj.x, obj.x + obj.w);
      const y1 = Math.min(obj.y, obj.y + obj.h);
      const x2 = Math.max(obj.x, obj.x + obj.w);
      const y2 = Math.max(obj.y, obj.y + obj.h);
      const mx = (x1 + x2) / 2;
      const my = (y1 + y2) / 2;
      return [
        { id: 'nw', x: x1, y: y1, cursor: 'nwse-resize' },
        { id: 'ne', x: x2, y: y1, cursor: 'nesw-resize' },
        { id: 'se', x: x2, y: y2, cursor: 'nwse-resize' },
        { id: 'sw', x: x1, y: y2, cursor: 'nesw-resize' },
        { id: 'n',  x: mx, y: y1, cursor: 'ns-resize' },
        { id: 's',  x: mx, y: y2, cursor: 'ns-resize' },
        { id: 'w',  x: x1, y: my, cursor: 'ew-resize' },
        { id: 'e',  x: x2, y: my, cursor: 'ew-resize' }
      ];
    }

    if (obj.type === 'ellipse') {
      const rx = Math.abs(obj.rx);
      const ry = Math.abs(obj.ry);
      const x1 = obj.cx - rx;
      const y1 = obj.cy - ry;
      const x2 = obj.cx + rx;
      const y2 = obj.cy + ry;
      const mx = obj.cx;
      const my = obj.cy;
      return [
        { id: 'nw', x: x1, y: y1, cursor: 'nwse-resize' },
        { id: 'ne', x: x2, y: y1, cursor: 'nesw-resize' },
        { id: 'se', x: x2, y: y2, cursor: 'nwse-resize' },
        { id: 'sw', x: x1, y: y2, cursor: 'nesw-resize' },
        { id: 'n',  x: mx, y: y1, cursor: 'ns-resize' },
        { id: 's',  x: mx, y: y2, cursor: 'ns-resize' },
        { id: 'w',  x: x1, y: my, cursor: 'ew-resize' },
        { id: 'e',  x: x2, y: my, cursor: 'ew-resize' }
      ];
    }

    return [];
  }

  function getObjectResizeHandle(obj, x, y) {
    if (!obj) return null;
    const handles = getObjectResizeHandles(obj);
    const pad = 16; // clickable handle radius
    for (const h of handles) {
      if (Math.hypot(x - h.x, y - h.y) <= pad) {
        return h;
      }
    }
    return null;
  }

  // --- Bounding Box & Hit Testing ---
  function getObjectBoundingBox(obj) {
    switch (obj.type) {
      case 'stroke': {
        if (!obj.points || obj.points.length === 0) return null;
        let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        for (const p of obj.points) {
          minX = Math.min(minX, p.x);
          minY = Math.min(minY, p.y);
          maxX = Math.max(maxX, p.x);
          maxY = Math.max(maxY, p.y);
        }
        const pad = (obj.width || 4) / 2;
        return { x: minX - pad, y: minY - pad, w: maxX - minX + pad * 2, h: maxY - minY + pad * 2 };
      }
      case 'line':
      case 'arrow': {
        const minX = Math.min(obj.x1, obj.x2);
        const minY = Math.min(obj.y1, obj.y2);
        const maxX = Math.max(obj.x1, obj.x2);
        const maxY = Math.max(obj.y1, obj.y2);
        const pad = Math.max(obj.width * 2, 8);
        return { x: minX - pad, y: minY - pad, w: maxX - minX + pad * 2, h: maxY - minY + pad * 2 };
      }
      case 'rect': {
        const x = Math.min(obj.x, obj.x + obj.w);
        const y = Math.min(obj.y, obj.y + obj.h);
        return { x, y, w: Math.abs(obj.w), h: Math.abs(obj.h) };
      }
      case 'ellipse': {
        return {
          x: obj.cx - Math.abs(obj.rx),
          y: obj.cy - Math.abs(obj.ry),
          w: Math.abs(obj.rx) * 2,
          h: Math.abs(obj.ry) * 2
        };
      }
      case 'text': {
        measureTextObject(obj);
        return { x: obj.x, y: obj.y, w: obj.w, h: obj.h };
      }
      case 'image': {
        return { x: obj.x, y: obj.y, w: obj.w, h: obj.h };
      }
      default:
        return null;
    }
  }

  function measureTextObject(obj) {
    if (!ctx) return;
    const fontSize = obj.fontSize || 24;
    ctx.save();
    ctx.font = `bold ${fontSize}px sans-serif`;
    const lines = (obj.text || '').split('\n');
    let maxWidth = 20;
    for (const l of lines) {
      const metrics = ctx.measureText(l);
      if (metrics.width > maxWidth) maxWidth = metrics.width;
    }
    ctx.restore();
    obj.w = maxWidth + 8;
    obj.h = lines.length * (fontSize * 1.25);
  }

  function distToSegment(px, py, x1, y1, x2, y2) {
    const l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
    if (l2 === 0) return Math.hypot(px - x1, py - y1);
    let t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;
    t = Math.max(0, Math.min(1, t));
    return Math.hypot(px - (x1 + t * (x2 - x1)), py - (y1 + t * (y2 - y1)));
  }

  function distToStroke(px, py, points, threshold) {
    if (!points || points.length === 0) return false;
    for (let i = 0; i < points.length - 1; i++) {
      if (distToSegment(px, py, points[i].x, points[i].y, points[i + 1].x, points[i + 1].y) <= threshold) {
        return true;
      }
    }
    return false;
  }

  function hitTest(x, y) {
    // Test in reverse order (top-most first)
    for (let i = objects.length - 1; i >= 0; i--) {
      const obj = objects[i];
      const box = getObjectBoundingBox(obj);
      if (!box) continue;

      if (x < box.x || x > box.x + box.w || y < box.y || y > box.y + box.h) {
        continue;
      }

      if (obj.type === 'line' || obj.type === 'arrow') {
        const threshold = Math.max((obj.width || 4) * 2, 12);
        if (distToSegment(x, y, obj.x1, obj.y1, obj.x2, obj.y2) <= threshold) {
          return obj;
        }
      } else if (obj.type === 'stroke') {
        const threshold = Math.max((obj.width || 4) * 2, 12);
        if (distToStroke(x, y, obj.points, threshold)) {
          return obj;
        }
      } else {
        return obj;
      }
    }
    return null;
  }

  // --- HTML Edit Mode Block Hover Highlight ---
  const HTML_BLOCK_DISPLAYS = new Set([
    'block', 'flex', 'grid', 'list-item', 'flow-root',
    'table', 'table-row', 'table-cell', 'table-caption',
    'table-row-group', 'table-header-group', 'table-footer-group',
    'inline-block', 'inline-flex', 'inline-grid', 'inline-table'
  ]);
  const HTML_REPLACED_TAGS = new Set(['IMG', 'CANVAS', 'VIDEO', 'IFRAME']);

  function findHtmlBlockElement(target) {
    let el = target;
    if (el && el.nodeType === 3) el = el.parentElement;
    if (!(el instanceof Element)) return null;
    if (el === hostEl || hostEl?.contains(el)) return null;
    if (el === document.body || el === document.documentElement) return null;

    // Walk out of SVG child primitives (<path>, <g>, etc.) up to <svg>
    while (el && el instanceof SVGElement && el.tagName.toLowerCase() !== 'svg') {
      el = el.parentElement;
    }
    if (!el || el === document.body || el === document.documentElement) return null;

    const startEl = el;
    while (el && el !== document.body && el !== document.documentElement) {
      if (el === hostEl || hostEl?.contains(el)) return null;
      const tag = el.tagName ? el.tagName.toUpperCase() : '';
      const disp = getComputedStyle(el).display;
      if (HTML_BLOCK_DISPLAYS.has(disp) || HTML_REPLACED_TAGS.has(tag)) {
        const r = el.getBoundingClientRect();
        if (r.width > 0 && r.height > 0) return el;
      }
      el = el.parentElement;
    }

    if (startEl && startEl !== document.body && startEl !== document.documentElement) {
      const r = startEl.getBoundingClientRect();
      if (r.width > 0 && r.height > 0) return startEl;
    }
    return null;
  }

  function renderHtmlHoverHighlight() {
    if (!htmlHoverBoxEl) return;
    if (!isActive || currentTool !== 'edit' || !hoveredHtmlEl || !hoveredHtmlEl.isConnected) {
      htmlHoverBoxEl.style.display = 'none';
      return;
    }
    const rect = hoveredHtmlEl.getBoundingClientRect();
    if (rect.width <= 0 || rect.height <= 0) {
      htmlHoverBoxEl.style.display = 'none';
      return;
    }
    htmlHoverBoxEl.style.display = 'block';
    htmlHoverBoxEl.style.left = `${rect.left}px`;
    htmlHoverBoxEl.style.top = `${rect.top}px`;
    htmlHoverBoxEl.style.width = `${rect.width}px`;
    htmlHoverBoxEl.style.height = `${rect.height}px`;
  }

  function clearHtmlHoverHighlight() {
    hoveredHtmlEl = null;
    lastHtmlMousePos = null;
    if (htmlHoverBoxEl) {
      htmlHoverBoxEl.style.display = 'none';
    }
  }

  function setupHtmlEditHighlightEvents() {
    document.addEventListener('mouseleave', () => {
      if (currentTool === 'edit') clearHtmlHoverHighlight();
    });
    window.addEventListener('blur', () => {
      if (currentTool === 'edit') clearHtmlHoverHighlight();
    });
    window.addEventListener('scroll', () => {
      if (!isActive || currentTool !== 'edit') return;
      if (lastHtmlMousePos) {
        const el = document.elementFromPoint(lastHtmlMousePos.x, lastHtmlMousePos.y);
        hoveredHtmlEl = findHtmlBlockElement(el);
      }
      renderHtmlHoverHighlight();
    }, true);
    document.addEventListener('input', () => {
      if (isActive && currentTool === 'edit' && hoveredHtmlEl) {
        renderHtmlHoverHighlight();
      }
    }, true);
  }

  // --- Canvas Mouse / Touch Events ---
  function setupCanvasEvents() {
    canvas.addEventListener('mousedown', onMouseDown);
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    canvas.addEventListener('dblclick', onDoubleClick);
  }

  function getMousePos(e) {
    return { x: e.clientX, y: e.clientY };
  }

  function onMouseDown(e) {
    if (!isActive || e.button !== 0) return;
    const pos = getMousePos(e);

    // If text editor is open, close/commit it
    if (textEditorEl && textEditorEl.style.display === 'block') {
      commitTextEditor();
    }

    if (currentTool === 'select') {
      // Check if clicking on an object resize/endpoint handle first
      if (selectedObject) {
        const resizeH = getObjectResizeHandle(selectedObject, pos.x, pos.y);
        if (resizeH) {
          isResizingSelected = true;
          activeResizeHandle = resizeH.id;
          isFraming = (e.ctrlKey || e.metaKey) && selectedObject.type === 'image';
          dragStartPos = pos;
          selectedObjectInitialState = cloneDoodleObject(selectedObject);
          return;
        }

        // フレーミングモード(Ctrl): 画像(隠れ部分含む)上のドラッグ範囲が新しいフレームになる
        if ((e.ctrlKey || e.metaKey) && selectedObject.type === 'image') {
          const fr = getImageFullRect(selectedObject);
          if (fr && pos.x >= fr.x && pos.x <= fr.x + fr.w && pos.y >= fr.y && pos.y <= fr.y + fr.h) {
            framingRect = { x0: pos.x, y0: pos.y, x1: pos.x, y1: pos.y };
            dragStartPos = pos;
            render();
            return;
          }
        }
      }

      const hit = hitTest(pos.x, pos.y);
      selectedObject = hit;
      if (hit) {
        const resizeH = getObjectResizeHandle(hit, pos.x, pos.y);
        if (resizeH) {
          isResizingSelected = true;
          activeResizeHandle = resizeH.id;
          isFraming = (e.ctrlKey || e.metaKey) && hit.type === 'image';
          dragStartPos = pos;
          selectedObjectInitialState = cloneDoodleObject(hit);
          render();
          return;
        }
        isDraggingSelected = true;
        dragStartPos = pos;
        selectedObjectInitialState = cloneDoodleObject(hit);
      }
      render();
      return;
    }

    if (currentTool === 'eyedropper') {
      const picked = getPixelColorAt(pos.x, pos.y);
      if (picked) {
        currentColor = picked;
        updateColorSelection();
        if (selectedObject) {
          updateSelectedObjectStyle();
        }
        showToast(`💧 色を抽出しました: ${currentColor}`, 'success');
      }
      setTool(previousTool || 'freehand');
      return;
    }

    if (currentTool === 'crop') {
      handleCropMouseDown(pos);
      return;
    }

    if (currentTool === 'text') {
      // Check if clicking on an existing text object -> Edit it directly!
      const hit = hitTest(pos.x, pos.y);
      if (hit && hit.type === 'text') {
        openTextEditorForObject(hit);
        return;
      }
      // Otherwise open editor to create new text
      openTextEditorAt(pos.x, pos.y);
      return;
    }

    if (currentTool === 'stamp' && currentStamp) {
      const newObj = {
        id: 'text_' + Date.now() + '_' + Math.random().toString(36).substr(2, 5),
        type: 'text',
        text: currentStamp,
        x: Math.round(pos.x),
        y: Math.round(pos.y),
        color: currentColor,
        fontSize: currentFontSize
      };
      measureTextObject(newObj);
      objects.push(newObj);
      pushAction({ type: 'ADD', object: newObj });
      render();
      return;
    }

    // Drawing tools
    isDrawing = true;
    const id = 'obj_' + Date.now() + '_' + Math.random().toString(36).substr(2, 5);

    if (currentTool === 'freehand') {
      draftObject = {
        id,
        type: 'stroke',
        color: currentColor,
        width: currentStrokeWidth,
        points: [pos]
      };
    } else if (currentTool === 'line') {
      draftObject = {
        id,
        type: 'line',
        color: currentColor,
        width: currentStrokeWidth,
        x1: pos.x,
        y1: pos.y,
        x2: pos.x,
        y2: pos.y
      };
    } else if (currentTool === 'arrow') {
      draftObject = {
        id,
        type: 'arrow',
        color: currentColor,
        width: currentStrokeWidth,
        x1: pos.x,
        y1: pos.y,
        x2: pos.x,
        y2: pos.y
      };
    } else if (currentTool === 'rect') {
      draftObject = {
        id,
        type: 'rect',
        strokeColor: currentColor,
        fillColor: getFillColor(currentColor, currentFillMode),
        width: currentStrokeWidth,
        x: pos.x,
        y: pos.y,
        w: 0,
        h: 0
      };
    } else if (currentTool === 'ellipse') {
      draftObject = {
        id,
        type: 'ellipse',
        strokeColor: currentColor,
        fillColor: getFillColor(currentColor, currentFillMode),
        width: currentStrokeWidth,
        cx: pos.x,
        cy: pos.y,
        rx: 0,
        ry: 0,
        startX: pos.x,
        startY: pos.y
      };
    }

    render();
  }

  function onMouseMove(e) {
    if (!isActive) return;
    const pos = getMousePos(e);

    if (currentTool === 'edit') {
      if (toolbarEl?.classList.contains('dragging')) {
        clearHtmlHoverHighlight();
        return;
      }
      lastHtmlMousePos = pos;
      hoveredHtmlEl = findHtmlBlockElement(e.target);
      renderHtmlHoverHighlight();
      return;
    }

    if (currentTool === 'eyedropper') {
      eyedropperHoverPos = pos;
      canvas.style.cursor = 'crosshair';
      render();
      return;
    }

    if (currentTool === 'select') {
      // 0. Framing rectangle drag
      if (framingRect) {
        framingRect.x1 = pos.x;
        framingRect.y1 = pos.y;
        render();
        return;
      }
      // 1. Resizing or Endpoint Dragging
      if (isResizingSelected && selectedObject) {
        const init = selectedObjectInitialState;
        const dx = pos.x - dragStartPos.x;
        const dy = pos.y - dragStartPos.y;

        // --- Line & Arrow Endpoints ---
        if (selectedObject.type === 'line' || selectedObject.type === 'arrow') {
          if (activeResizeHandle === 'start') {
            selectedObject.x1 = Math.round(init.x1 + dx);
            selectedObject.y1 = Math.round(init.y1 + dy);
          } else if (activeResizeHandle === 'end') {
            selectedObject.x2 = Math.round(init.x2 + dx);
            selectedObject.y2 = Math.round(init.y2 + dy);
          }
          render();
          return;
        }

        // --- Ellipse 8 Handles ---
        if (selectedObject.type === 'ellipse') {
          const initRx = Math.abs(init.rx);
          const initRy = Math.abs(init.ry);
          const x1 = init.cx - initRx;
          const y1 = init.cy - initRy;
          const x2 = init.cx + initRx;
          const y2 = init.cy + initRy;
          const initW = initRx * 2;
          const initH = initRy * 2;
          const ratio = Math.max(0.01, initW / initH);

          let newCx = init.cx;
          let newCy = init.cy;
          let newRx = initRx;
          let newRy = initRy;

          if (activeResizeHandle === 'se') {
            const newW = Math.max(16, initW + dx);
            const newH = Math.max(16, newW / ratio);
            newRx = newW / 2;
            newRy = newH / 2;
            newCx = x1 + newRx;
            newCy = y1 + newRy;
          } else if (activeResizeHandle === 'sw') {
            const newW = Math.max(16, initW - dx);
            const newH = Math.max(16, newW / ratio);
            newRx = newW / 2;
            newRy = newH / 2;
            newCx = x2 - newRx;
            newCy = y1 + newRy;
          } else if (activeResizeHandle === 'ne') {
            const newW = Math.max(16, initW + dx);
            const newH = Math.max(16, newW / ratio);
            newRx = newW / 2;
            newRy = newH / 2;
            newCx = x1 + newRx;
            newCy = y2 - newRy;
          } else if (activeResizeHandle === 'nw') {
            const newW = Math.max(16, initW - dx);
            const newH = Math.max(16, newW / ratio);
            newRx = newW / 2;
            newRy = newH / 2;
            newCx = x2 - newRx;
            newCy = y2 - newRy;
          } else if (activeResizeHandle === 's') {
            // Change height only
            const newH = Math.max(16, initH + dy);
            newRy = newH / 2;
            newCy = y1 + newRy;
          } else if (activeResizeHandle === 'n') {
            // Change height only
            const newH = Math.max(16, initH - dy);
            newRy = newH / 2;
            newCy = y2 - newRy;
          } else if (activeResizeHandle === 'e') {
            // Change width only
            const newW = Math.max(16, initW + dx);
            newRx = newW / 2;
            newCx = x1 + newRx;
          } else if (activeResizeHandle === 'w') {
            // Change width only
            const newW = Math.max(16, initW - dx);
            newRx = newW / 2;
            newCx = x2 - newRx;
          }

          selectedObject.cx = Math.round(newCx);
          selectedObject.cy = Math.round(newCy);
          selectedObject.rx = Math.round(newRx);
          selectedObject.ry = Math.round(newRy);
          render();
          return;
        }

        // --- Image 8 Handles ---
        if (selectedObject.type === 'image' && isFraming && init.sw > 0 && init.sh > 0) {
          // Framing: 辺/角の移動分だけ元画像の表示範囲(sx,sy,sw,sh)を増減。画像自体は拡縮しない
          // 反転(flipX/flipY)中は画面上の左右/上下と元画像の左右/上下が入れ替わるため補正する
          const fx = !!init.flipX, fy = !!init.flipY;
          const kx = init.sw / init.w;
          const ky = init.sh / init.h;
          const natW = init.img?.naturalWidth || init.sx + init.sw;
          const natH = init.img?.naturalHeight || init.sy + init.sh;
          const minS = 8;
          let sL = init.sx, sR = init.sx + init.sw, sT = init.sy, sB = init.sy + init.sh;
          const hid = activeResizeHandle;
          let hx = hid.includes('w') ? 'w' : hid.includes('e') ? 'e' : '';
          let hy = hid.includes('n') ? 'n' : hid.includes('s') ? 's' : '';
          if (fx && hx) hx = hx === 'w' ? 'e' : 'w';
          if (fy && hy) hy = hy === 'n' ? 's' : 'n';
          const ddx = fx ? -dx : dx;
          const ddy = fy ? -dy : dy;
          if (hx === 'w') sL = Math.min(sR - minS, Math.max(0, init.sx + ddx * kx));
          if (hx === 'e') sR = Math.max(sL + minS, Math.min(natW, init.sx + init.sw + ddx * kx));
          if (hy === 'n') sT = Math.min(sB - minS, Math.max(0, init.sy + ddy * ky));
          if (hy === 's') sB = Math.max(sT + minS, Math.min(natH, init.sy + init.sh + ddy * ky));
          selectedObject.sx = sL;
          selectedObject.sy = sT;
          selectedObject.sw = sR - sL;
          selectedObject.sh = sB - sT;
          selectedObject.x = fx ? init.x + ((init.sx + init.sw) - sR) / kx : init.x + (sL - init.sx) / kx;
          selectedObject.y = fy ? init.y + ((init.sy + init.sh) - sB) / ky : init.y + (sT - init.sy) / ky;
          selectedObject.w = (sR - sL) / kx;
          selectedObject.h = (sB - sT) / ky;
          render();
          return;
        }
        if (selectedObject.type === 'image') {
          // 反対側の辺(原点)を超えてドラッグすると画像を反転(flipX/flipY)
          const hid = activeResizeHandle;
          const hasW = hid.includes('w'), hasE = hid.includes('e');
          const hasN = hid.includes('n'), hasS = hid.includes('s');
          const corner = (hasW || hasE) && (hasN || hasS);
          const ratio = Math.max(0.01, init.w / init.h);
          const minSize = corner ? 24 : 20;
          let rawW = init.w, rawH = init.h;
          if (hasE) rawW = init.w + dx; else if (hasW) rawW = init.w - dx;
          if (hasS) rawH = init.h + dy; else if (hasN) rawH = init.h - dy;
          const absW = Math.max(minSize, Math.abs(rawW));
          const absH = corner ? Math.max(minSize, absW / ratio) : Math.max(minSize, Math.abs(rawH));

          let newX = init.x, newY = init.y;
          if (hasW) newX = rawW >= 0 ? init.x + init.w - absW : init.x + init.w;
          else if (hasE) newX = rawW >= 0 ? init.x : init.x - absW;
          if (hasN) newY = rawH >= 0 ? init.y + init.h - absH : init.y + init.h;
          else if (hasS) newY = rawH >= 0 ? init.y : init.y - absH;

          selectedObject.flipX = (hasW || hasE) ? (!!init.flipX !== (rawW < 0)) : !!init.flipX;
          selectedObject.flipY = (hasN || hasS) ? (!!init.flipY !== (rawH < 0)) : !!init.flipY;
          selectedObject.x = Math.round(newX);
          selectedObject.y = Math.round(newY);
          selectedObject.w = Math.round(((hasW || hasE) ? absW : init.w));
          selectedObject.h = Math.round(((hasN || hasS) ? absH : init.h));
          render();
          return;
        }

        // --- Rect 8 Handles ---
        if (selectedObject.type === 'rect') {
          const initX1 = Math.min(init.x, init.x + init.w);
          const initY1 = Math.min(init.y, init.y + init.h);
          const initX2 = Math.max(init.x, init.x + init.w);
          const initY2 = Math.max(init.y, init.y + init.h);
          const initW = Math.max(1, initX2 - initX1);
          const initH = Math.max(1, initY2 - initY1);
          const ratio = Math.max(0.01, initW / initH);
          const freeRatio = !!e.shiftKey;

          let newX = initX1;
          let newY = initY1;
          let newW = initW;
          let newH = initH;

          if (activeResizeHandle === 'se') {
            newW = Math.max(8, initW + dx);
            newH = freeRatio ? Math.max(8, initH + dy) : Math.max(8, newW / ratio);
          } else if (activeResizeHandle === 'sw') {
            newW = Math.max(8, initW - dx);
            newH = freeRatio ? Math.max(8, initH + dy) : Math.max(8, newW / ratio);
            newX = initX2 - newW;
          } else if (activeResizeHandle === 'ne') {
            newW = Math.max(8, initW + dx);
            newH = freeRatio ? Math.max(8, initH - dy) : Math.max(8, newW / ratio);
            newY = initY2 - newH;
          } else if (activeResizeHandle === 'nw') {
            newW = Math.max(8, initW - dx);
            newH = freeRatio ? Math.max(8, initH - dy) : Math.max(8, newW / ratio);
            newX = initX2 - newW;
            newY = initY2 - newH;
          } else if (activeResizeHandle === 's') {
            // Bottom midpoint: change height only (changes aspect ratio)
            newH = Math.max(8, initH + dy);
          } else if (activeResizeHandle === 'n') {
            // Top midpoint: change height only (changes aspect ratio)
            newH = Math.max(8, initH - dy);
            newY = initY2 - newH;
          } else if (activeResizeHandle === 'e') {
            // Right midpoint: change width only (changes aspect ratio)
            newW = Math.max(8, initW + dx);
          } else if (activeResizeHandle === 'w') {
            // Left midpoint: change width only (changes aspect ratio)
            newW = Math.max(8, initW - dx);
            newX = initX2 - newW;
          }

          selectedObject.x = Math.round(newX);
          selectedObject.y = Math.round(newY);
          selectedObject.w = Math.round(newW);
          selectedObject.h = Math.round(newH);
          render();
          return;
        }
      }

      // 2. Moving selected object
      if (isDraggingSelected && selectedObject) {
        const dx = pos.x - dragStartPos.x;
        const dy = pos.y - dragStartPos.y;
        moveObject(selectedObject, selectedObjectInitialState, dx, dy);
        render();
        return;
      }

      // 3. Hover cursor update
      if (!isDrawing && !isCropping) {
        // フレーミングモード: クロップと同じ十字カーソル
        if (framingMode && selectedObject?.type === 'image') {
          canvas.style.cursor = 'crosshair';
          return;
        }
        if (selectedObject) {
          const resizeH = getObjectResizeHandle(selectedObject, pos.x, pos.y);
          if (resizeH) {
            canvas.style.cursor = resizeH.cursor;
            return;
          }
        }
        const hoverHit = hitTest(pos.x, pos.y);
        if (hoverHit) {
          canvas.style.cursor = 'move';
        } else {
          canvas.style.cursor = 'default';
        }
      }
    }

    if (currentTool === 'crop') {
      if (isCropping) {
        handleCropMouseMove(pos);
        canvas.style.cursor = getCropCursor(cropDragHandle);
      } else {
        canvas.style.cursor = getCropCursor(getCropHandleAt(pos.x, pos.y));
      }
      return;
    }

    if (!isDrawing || !draftObject) return;

    if (draftObject.type === 'stroke') {
      draftObject.points.push(pos);
    } else if (draftObject.type === 'line' || draftObject.type === 'arrow') {
      draftObject.x2 = pos.x;
      draftObject.y2 = pos.y;
    } else if (draftObject.type === 'rect') {
      draftObject.w = pos.x - draftObject.x;
      draftObject.h = pos.y - draftObject.y;
    } else if (draftObject.type === 'ellipse') {
      draftObject.cx = (draftObject.startX + pos.x) / 2;
      draftObject.cy = (draftObject.startY + pos.y) / 2;
      draftObject.rx = Math.abs(pos.x - draftObject.startX) / 2;
      draftObject.ry = Math.abs(pos.y - draftObject.startY) / 2;
    }

    render();
  }

  function onMouseUp(e) {
    if (!isActive) return;

    if (currentTool === 'select') {
      if (framingRect) {
        applyFramingRect();
        return;
      }
      if (isResizingSelected && selectedObject) {
        isResizingSelected = false;
        activeResizeHandle = null;
        isFraming = false;
        const afterState = cloneDoodleObject(selectedObject);
        if (JSON.stringify(selectedObjectInitialState) !== JSON.stringify(afterState)) {
          pushAction({
            type: 'MODIFY',
            objectId: selectedObject.id,
            before: selectedObjectInitialState,
            after: afterState
          });
        }
        return;
      }

      if (isDraggingSelected && selectedObject) {
        isDraggingSelected = false;
        const afterState = JSON.parse(JSON.stringify(selectedObject));
        if (JSON.stringify(selectedObjectInitialState) !== JSON.stringify(afterState)) {
          pushAction({
            type: 'MODIFY',
            objectId: selectedObject.id,
            before: selectedObjectInitialState,
            after: afterState
          });
        }
        return;
      }
    }

    if (currentTool === 'crop' && isCropping) {
      handleCropMouseUp(getMousePos(e));
      return;
    }

    if (!isDrawing || !draftObject) return;
    isDrawing = false;

    // Validate if object has meaningful dimensions
    let isValid = true;
    if (draftObject.type === 'stroke' && draftObject.points.length <= 1) isValid = false;
    if ((draftObject.type === 'line' || draftObject.type === 'arrow') &&
        Math.hypot(draftObject.x2 - draftObject.x1, draftObject.y2 - draftObject.y1) < 4) {
      isValid = false;
    }
    if (draftObject.type === 'rect' && (Math.abs(draftObject.w) < 4 || Math.abs(draftObject.h) < 4)) {
      isValid = false;
    }
    if (draftObject.type === 'ellipse' && (draftObject.rx < 3 || draftObject.ry < 3)) {
      isValid = false;
    }

    if (isValid) {
      if (draftObject.type === 'rect') {
        const rx = Math.min(draftObject.x, draftObject.x + draftObject.w);
        const ry = Math.min(draftObject.y, draftObject.y + draftObject.h);
        draftObject.w = Math.abs(draftObject.w);
        draftObject.h = Math.abs(draftObject.h);
        draftObject.x = rx;
        draftObject.y = ry;
      }
      objects.push(draftObject);
      pushAction({
        type: 'ADD',
        object: draftObject
      });
    }

    draftObject = null;
    render();
  }

  function onDoubleClick(e) {
    if (!isActive) return;
    const pos = getMousePos(e);
    const hit = hitTest(pos.x, pos.y);
    if (hit && hit.type === 'text') {
      openTextEditorForObject(hit);
    }
  }

  function moveObject(obj, initial, dx, dy) {
    if (initial.type === 'stroke') {
      obj.points = initial.points.map(p => ({ x: p.x + dx, y: p.y + dy }));
    } else if (initial.type === 'line' || initial.type === 'arrow') {
      obj.x1 = initial.x1 + dx;
      obj.y1 = initial.y1 + dy;
      obj.x2 = initial.x2 + dx;
      obj.y2 = initial.y2 + dy;
    } else if (initial.type === 'rect' || initial.type === 'text' || initial.type === 'image') {
      obj.x = initial.x + dx;
      obj.y = initial.y + dy;
    } else if (initial.type === 'ellipse') {
      obj.cx = initial.cx + dx;
      obj.cy = initial.cy + dy;
    }
  }

  function getFillColor(color, mode) {
    if (mode === 'none') return null;
    if (mode === 'solid') return color;
    // Semi-transparent
    return hexToRgba(color, 0.25);
  }

  function hexToRgba(hex, alpha) {
    const c = hex.replace('#', '');
    const num = parseInt(c.length === 3 ? c.split('').map(x => x + x).join('') : c, 16);
    const r = (num >> 16) & 255;
    const g = (num >> 8) & 255;
    const b = num & 255;
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }

  // --- Text Tool & In-place Text Editing ---
  let editingTextObject = null;

  function openTextEditorAt(x, y) {
    editingTextObject = null;
    textEditorEl.value = '';
    textEditorEl.style.left = `${Math.min(x, window.innerWidth - 260)}px`;
    textEditorEl.style.top = `${Math.min(y, window.innerHeight - 100)}px`;
    textEditorEl.style.fontSize = `${currentFontSize}px`;
    textEditorEl.style.color = currentColor;
    textEditorEl.style.display = 'block';

    setTimeout(() => {
      textEditorEl.focus();
    }, 10);
  }

  function openTextEditorForObject(textObj) {
    editingTextObject = textObj;
    textEditorEl.value = textObj.text || '';
    textEditorEl.style.left = `${textObj.x}px`;
    textEditorEl.style.top = `${textObj.y}px`;
    textEditorEl.style.fontSize = `${textObj.fontSize || currentFontSize}px`;
    textEditorEl.style.color = textObj.color || currentColor;
    textEditorEl.style.display = 'block';

    // Temporarily hide from rendering while editing in-place
    render();

    setTimeout(() => {
      textEditorEl.focus();
      textEditorEl.select();
    }, 10);
  }

  function commitTextEditor() {
    if (textEditorEl.style.display !== 'block') return;
    const text = textEditorEl.value.trim();

    if (editingTextObject) {
      // Modifying existing text
      const beforeState = JSON.parse(JSON.stringify(editingTextObject));
      if (text === '') {
        // Delete empty text
        const idx = objects.findIndex(o => o.id === editingTextObject.id);
        if (idx !== -1) {
          objects.splice(idx, 1);
          pushAction({
            type: 'REMOVE',
            object: editingTextObject,
            index: idx
          });
        }
      } else {
        editingTextObject.text = text;
        editingTextObject.color = currentColor;
        editingTextObject.fontSize = currentFontSize;
        measureTextObject(editingTextObject);
        const afterState = JSON.parse(JSON.stringify(editingTextObject));
        pushAction({
          type: 'MODIFY',
          objectId: editingTextObject.id,
          before: beforeState,
          after: afterState
        });
      }
    } else if (text !== '') {
      // Adding new text
      const newObj = {
        id: 'text_' + Date.now(),
        type: 'text',
        text: text,
        x: parseInt(textEditorEl.style.left, 10),
        y: parseInt(textEditorEl.style.top, 10),
        color: currentColor,
        fontSize: currentFontSize
      };
      measureTextObject(newObj);
      objects.push(newObj);
      pushAction({
        type: 'ADD',
        object: newObj
      });
    }

    closeTextEditor();
    render();
  }

  function closeTextEditor() {
    if (!textEditorEl) return;
    textEditorEl.style.display = 'none';
    textEditorEl.value = '';
    editingTextObject = null;
  }

  // --- Crop Tool Implementation ---
  function getCropCursor(handle) {
    switch (handle) {
      case 'nw':
      case 'se':
        return 'nwse-resize';
      case 'ne':
      case 'sw':
        return 'nesw-resize';
      case 'n':
      case 's':
        return 'ns-resize';
      case 'e':
      case 'w':
        return 'ew-resize';
      case 'move':
        return 'move';
      default:
        return 'crosshair';
    }
  }

  function handleCropMouseDown(pos) {
    if (!cropRect) {
      isCropping = true;
      cropDragHandle = 'create';
      cropStartPos = pos;
      cropRect = { x: pos.x, y: pos.y, w: 0, h: 0 };
    } else {
      const handle = getCropHandleAt(pos.x, pos.y);
      if (handle) {
        isCropping = true;
        cropDragHandle = handle;
        cropStartPos = pos;
        cropInitialRect = { ...cropRect };
      } else {
        // Reset crop if clicked outside handles
        isCropping = true;
        cropDragHandle = 'create';
        cropStartPos = pos;
        cropRect = { x: pos.x, y: pos.y, w: 0, h: 0 };
      }
    }
    if (canvas) {
      canvas.style.cursor = getCropCursor(cropDragHandle);
    }
    render();
  }

  function handleCropMouseMove(pos) {
    if (cropDragHandle === 'create') {
      cropRect.w = pos.x - cropStartPos.x;
      cropRect.h = pos.y - cropStartPos.y;
    } else if (cropDragHandle === 'move' && cropInitialRect) {
      const dx = pos.x - cropStartPos.x;
      const dy = pos.y - cropStartPos.y;
      cropRect.x = cropInitialRect.x + dx;
      cropRect.y = cropInitialRect.y + dy;
    } else if (cropInitialRect) {
      const dx = pos.x - cropStartPos.x;
      const dy = pos.y - cropStartPos.y;
      if (cropDragHandle.includes('e')) cropRect.w = cropInitialRect.w + dx;
      if (cropDragHandle.includes('s')) cropRect.h = cropInitialRect.h + dy;
      if (cropDragHandle.includes('w')) {
        cropRect.x = cropInitialRect.x + dx;
        cropRect.w = cropInitialRect.w - dx;
      }
      if (cropDragHandle.includes('n')) {
        cropRect.y = cropInitialRect.y + dy;
        cropRect.h = cropInitialRect.h - dy;
      }
    }
    render();
  }

  function handleCropMouseUp(pos) {
    isCropping = false;
    cropDragHandle = null;

    if (cropRect) {
      // Normalize crop rect (w and h positive)
      const nx = Math.min(cropRect.x, cropRect.x + cropRect.w);
      const ny = Math.min(cropRect.y, cropRect.y + cropRect.h);
      const nw = Math.abs(cropRect.w);
      const nh = Math.abs(cropRect.h);

      if (nw < 20 || nh < 20) {
        cropRect = null;
      } else {
        cropRect = { x: nx, y: ny, w: nw, h: nh };
      }
    }
    if (canvas && currentTool === 'crop') {
      canvas.style.cursor = pos ? getCropCursor(getCropHandleAt(pos.x, pos.y)) : 'crosshair';
    }
    render();
  }

  function getCropHandleAt(x, y) {
    if (!cropRect) return null;
    const x1 = Math.min(cropRect.x, cropRect.x + cropRect.w);
    const y1 = Math.min(cropRect.y, cropRect.y + cropRect.h);
    const x2 = Math.max(cropRect.x, cropRect.x + cropRect.w);
    const y2 = Math.max(cropRect.y, cropRect.y + cropRect.h);
    const inv = 1 / (currentPageZoom || 1);
    const minDim = Math.max(20, Math.min(x2 - x1, y2 - y1));
    const pad = Math.min(minDim * 0.3, Math.max(8, 12 * inv));
    const edgePad = Math.min(minDim * 0.2, Math.max(5, 8 * inv));

    // Check corners first
    if (Math.hypot(x - x1, y - y1) <= pad) return 'nw';
    if (Math.hypot(x - x2, y - y1) <= pad) return 'ne';
    if (Math.hypot(x - x2, y - y2) <= pad) return 'se';
    if (Math.hypot(x - x1, y - y2) <= pad) return 'sw';

    // Check edges
    if (Math.abs(y - y1) <= edgePad && x >= x1 - edgePad && x <= x2 + edgePad) return 'n';
    if (Math.abs(y - y2) <= edgePad && x >= x1 - edgePad && x <= x2 + edgePad) return 's';
    if (Math.abs(x - x1) <= edgePad && y >= y1 - edgePad && y <= y2 + edgePad) return 'w';
    if (Math.abs(x - x2) <= edgePad && y >= y1 - edgePad && y <= y2 + edgePad) return 'e';

    // Check inside to move
    if (x >= x1 && x <= x2 && y >= y1 && y <= y2) return 'move';

    return null;
  }

  function updateCropOverlay() {
    if (!cropOverlayEl) return;

    if (currentTool !== 'crop' || !cropRect) {
      cropOverlayEl.style.display = 'none';
      cropOverlayEl.innerHTML = '';
      return;
    }

    cropOverlayEl.style.display = 'block';
    const r = cropRect;
    const inv = 1 / (currentPageZoom || 1);
    const pad = 10 * inv;

    // Floating action bar near the crop rectangle (icon-only)
    const topPos = Math.max(pad, r.y + r.h + 12 * inv < window.innerHeight - 48 * inv ? r.y + r.h + 12 * inv : r.y - 44 * inv);
    const leftPos = Math.max(pad, Math.min(window.innerWidth - 140 * inv, r.x));

    cropOverlayEl.innerHTML = `
      <div class="cd-crop-info" style="top: ${Math.max(pad, r.y - 28 * inv)}px; left: ${r.x}px;">
        ${Math.round(r.w)} × ${Math.round(r.h)} px
      </div>
      <div class="cd-crop-actions" style="top: ${topPos}px; left: ${leftPos}px;">
        <button id="cd-crop-copy-btn" class="cd-action-icon-btn" title="領域コピー (Ctrl+C)" aria-label="領域コピー">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
        </button>
        <button id="cd-crop-save-btn" class="cd-action-icon-btn" title="PNG保存 (Ctrl+S)" aria-label="PNG保存">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
        </button>
        <button id="cd-crop-cancel-btn" class="cd-action-icon-btn close-btn" title="クロップ解除 (Esc)" aria-label="クロップ解除">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
    `;

    shadow.getElementById('cd-crop-copy-btn')?.addEventListener('click', () => {
      copyCropAreaToClipboard();
    });
    shadow.getElementById('cd-crop-save-btn')?.addEventListener('click', () => {
      saveCropAreaAsImage();
    });
    shadow.getElementById('cd-crop-cancel-btn')?.addEventListener('click', () => {
      cropRect = null;
      render();
    });
  }

  // --- Clipboard Integration: Copy Area to Clipboard ---
  async function getCompositeBlob(area) {
    const dpr = window.devicePixelRatio || 1;
    const offscreen = document.createElement('canvas');
    const targetW = area ? area.w : window.innerWidth;
    const targetH = area ? area.h : window.innerHeight;

    offscreen.width = Math.round(targetW * dpr);
    offscreen.height = Math.round(targetH * dpr);
    const octx = offscreen.getContext('2d');
    octx.scale(dpr, dpr);

    // 1. Try capturing background webpage via background service worker
    let bgCaptured = false;
    if (typeof chrome !== 'undefined' && chrome.runtime?.sendMessage) {
      try {
        // Temporarily hide our entire UI/canvas so the crop border/toolbar are NEVER captured
        if (hostEl) hostEl.style.visibility = 'hidden';
        await new Promise(r => setTimeout(r, 40));

        const response = await chrome.runtime.sendMessage({ type: 'CHROME_DOODLE_CAPTURE_TAB' });

        if (hostEl) hostEl.style.visibility = 'visible';

        if (response && response.success && response.dataUrl) {
          const bgImg = await loadImage(response.dataUrl);
          if (area) {
            // Slice cropped region from clean background screenshot
            octx.drawImage(
              bgImg,
              area.x * dpr, area.y * dpr, area.w * dpr, area.h * dpr,
              0, 0, area.w, area.h
            );
          } else {
            octx.drawImage(bgImg, 0, 0, targetW, targetH);
          }
          bgCaptured = true;
        }
      } catch (e) {
        if (hostEl) hostEl.style.visibility = 'visible';
        console.warn('[ChromeDoodle] Unable to capture visible tab background:', e);
      }
    }

    if (!bgCaptured) {
      // Fallback: draw white or transparent background
      octx.fillStyle = '#ffffff';
      octx.fillRect(0, 0, targetW, targetH);
    }

    // 2. Draw doodles on top
    octx.save();
    if (area) {
      octx.translate(-area.x, -area.y);
    }
    for (const obj of objects) {
      drawObject(octx, obj);
    }
    octx.restore();

    return new Promise(resolve => {
      offscreen.toBlob(blob => resolve(blob), 'image/png');
    });
  }

  function fallbackCopyTextToClipboard(text) {
    try {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.style.cssText = 'position:fixed;left:-9999px;top:-9999px;opacity:0;';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      ta.remove();
    } catch (_) {}
  }

  function writeTextToClipboard(text) {
    if (navigator.clipboard && typeof navigator.clipboard.writeText === 'function') {
      return navigator.clipboard.writeText(text).catch(() => fallbackCopyTextToClipboard(text));
    }
    fallbackCopyTextToClipboard(text);
    return Promise.resolve();
  }

  async function writeImageBlobToClipboard(blob) {
    if (
      navigator.clipboard &&
      typeof navigator.clipboard.write === 'function' &&
      typeof ClipboardItem !== 'undefined'
    ) {
      try {
        await navigator.clipboard.write([
          new ClipboardItem({ 'image/png': blob })
        ]);
        return;
      } catch (directErr) {
        console.warn('[ChromeDoodle] Direct clipboard.write failed, trying extension helper:', directErr);
      }
    }

    const dataUrl = await readBlobAsDataURL(blob);
    const res = await chrome.runtime.sendMessage({
      type: 'CHROME_DOODLE_CLIPBOARD_WRITE_IMAGE',
      dataUrl
    });
    if (!res || !res.success) {
      throw new Error(res?.cancelled ? 'クリップボードコピーがキャンセルされました' : (res?.error || 'Clipboard write failed'));
    }
  }

  async function copyCropAreaToClipboard() {
    if (!cropRect) {
      showToast('⚠️ クロップ領域が指定されていません', 'warning');
      return;
    }

    showToast('⏳ キャプチャ中...', 'info');

    try {
      const blob = await getCompositeBlob(cropRect);
      if (!blob) throw new Error('Blob creation failed');

      await tryCopyBlobOrShowModal(blob, 'クロップ領域', cropRect.w, cropRect.h);
    } catch (err) {
      console.warn('[ChromeDoodle] Clipboard copy error:', err);
      showToast('❌ クリップボードへの書き込みに失敗しました', 'error');
    }
  }

  async function copyFullViewToClipboard() {
    showToast('⏳ 画面キャプチャ中...', 'info');
    try {
      const blob = await getCompositeBlob(null);
      if (!blob) throw new Error('Blob creation failed');

      await tryCopyBlobOrShowModal(blob, '画面全体', window.innerWidth, window.innerHeight);
    } catch (err) {
      console.warn('[ChromeDoodle] Clipboard copy error:', err);
      showToast('❌ コピーに失敗しました', 'error');
    }
  }

  async function saveCropAreaAsImage() {
    const area = cropRect || { x: 0, y: 0, w: window.innerWidth, h: window.innerHeight };
    showToast('⏳ 画像生成中...', 'info');

    try {
      const blob = await getCompositeBlob(area);
      if (!blob) throw new Error('Blob creation failed');

      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `doodle_${Date.now()}.png`;
      a.click();
      URL.revokeObjectURL(url);
      showToast('💾 画像を保存しました！', 'success');
    } catch (err) {
      console.error('[ChromeDoodle] Save error:', err);
      showToast('❌ 保存に失敗しました', 'error');
    }
  }

  // --- Capture Helpers & Result Modal ---
  const cdSleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

  async function captureTabWithThrottle() {
    for (let attempt = 0; attempt < 4; attempt++) {
      const waitMs = Math.max(0, 560 - (Date.now() - lastTabCaptureTs));
      if (waitMs > 0) await cdSleep(waitMs);
      lastTabCaptureTs = Date.now();
      const response = await chrome.runtime.sendMessage({ type: 'CHROME_DOODLE_CAPTURE_TAB' });
      if (response && response.success && response.dataUrl) {
        return response.dataUrl;
      }
      const errMsg = response?.error || '';
      if (/quota|MAX_CAPTURE_VISIBLE_TAB/i.test(errMsg)) {
        await cdSleep(650);
        continue;
      }
      throw new Error(errMsg || 'Tab capture failed');
    }
    throw new Error('キャプチャ回数上限に達しました。少し待ってから再試行してください');
  }

  function closeCaptureResultModal() {
    if (pendingCaptureObjectUrl) {
      URL.revokeObjectURL(pendingCaptureObjectUrl);
      pendingCaptureObjectUrl = null;
    }
    pendingCaptureBlob = null;
    if (captureModalEl) {
      captureModalEl.style.display = 'none';
      captureModalEl.innerHTML = '';
    }
  }

  function showCaptureResultModal(blob, title, width, height, note = '') {
    if (!captureModalEl || !blob) return;
    closeCaptureResultModal();

    pendingCaptureBlob = blob;
    pendingCaptureObjectUrl = URL.createObjectURL(blob);

    captureModalEl.innerHTML = `
      <div class="cd-modal-card">
        <div class="cd-modal-header">
          <span>${title} (${Math.round(width)} × ${Math.round(height)} px)</span>
          <button id="cd-modal-close" class="cd-modal-close-btn" title="閉じる">✕</button>
        </div>
        ${note ? `<div class="cd-modal-note">${note}</div>` : ''}
        <div class="cd-modal-preview">
          <img src="${pendingCaptureObjectUrl}" alt="Captured Preview" />
        </div>
        <div class="cd-modal-actions">
          <button id="cd-modal-copy" class="cd-action-btn primary">📋 クリップボードにコピー</button>
          <button id="cd-modal-paste-canvas" class="cd-action-btn">🎨 キャンバスに貼付</button>
          <button id="cd-modal-save" class="cd-action-btn">💾 PNG保存</button>
        </div>
      </div>
    `;
    captureModalEl.style.display = 'flex';

    shadow.getElementById('cd-modal-close')?.addEventListener('click', closeCaptureResultModal);
    captureModalEl.addEventListener('click', (e) => {
      if (e.target === captureModalEl) closeCaptureResultModal();
    }, { once: true });

    shadow.getElementById('cd-modal-copy')?.addEventListener('click', async () => {
      if (!pendingCaptureBlob) return;
      try {
        await writeImageBlobToClipboard(pendingCaptureBlob);
        showToast('✅ クリップボードにコピーしました！', 'success');
        closeCaptureResultModal();
      } catch (err) {
        console.warn('[ChromeDoodle] Modal copy error:', err);
        showToast('❌ コピーに失敗しました。PNG保存をお試しください', 'error');
      }
    });

    shadow.getElementById('cd-modal-paste-canvas')?.addEventListener('click', async () => {
      if (!pendingCaptureBlob) return;
      const b = pendingCaptureBlob;
      closeCaptureResultModal();
      await addImageBlobToDoodle(b);
    });

    shadow.getElementById('cd-modal-save')?.addEventListener('click', () => {
      if (!pendingCaptureBlob) return;
      const url = URL.createObjectURL(pendingCaptureBlob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `doodle_capture_${Date.now()}.png`;
      a.click();
      URL.revokeObjectURL(url);
      showToast('💾 PNG画像を保存しました！', 'success');
    });
  }

  async function tryCopyBlobOrShowModal(blob, title, width, height, forceModal = false) {
    if (!blob) throw new Error('Blob creation failed');
    if (forceModal) {
      showCaptureResultModal(blob, title, width, height, 'プレビューを確認してコピー・キャンバス配置・保存ができます');
      return;
    }
    try {
      await writeImageBlobToClipboard(blob);
      showToast(`✅ ${title} (${Math.round(width)}×${Math.round(height)}) をクリップボードにコピーしました！`, 'success');
    } catch (err) {
      console.warn('[ChromeDoodle] Auto clipboard write failed, showing fallback modal:', err);
      showCaptureResultModal(
        blob,
        title,
        width,
        height,
        'ブラウザの制限により自動コピーができなかったため、下のボタンからコピーまたはPNG保存ができます。'
      );
    }
  }

  // 貼付画像を現在のフレーム(sx/sy/sw/sh・反転・表示サイズ)のまま PNG としてクリップボードへ
  async function copyImageObjectAsFramed(obj) {
    try {
      const dpr = window.devicePixelRatio || 1;
      const w = Math.max(1, Math.round(Math.abs(obj.w) * dpr));
      const h = Math.max(1, Math.round(Math.abs(obj.h) * dpr));
      const off = document.createElement('canvas');
      off.width = w;
      off.height = h;
      const octx = off.getContext('2d');
      octx.scale(w / Math.abs(obj.w), h / Math.abs(obj.h));
      octx.translate(-obj.x, -obj.y);
      drawImageObject(octx, obj);
      const blob = await new Promise(resolve => off.toBlob(b => resolve(b), 'image/png'));
      await tryCopyBlobOrShowModal(blob, 'フレーム内の画像', w, h);
    } catch (err) {
      console.error('[ChromeDoodle] Copy framed image error:', err);
      showToast('❌ 画像のコピーに失敗しました', 'error');
    }
  }

  // --- Full Page Scroll Capture (コンテンツ全範囲スクロールコピー) ---
  function findScrollTarget() {
    const root = document.scrollingElement || document.documentElement;
    const rootH = Math.max(root.scrollHeight || 0, document.body?.scrollHeight || 0, window.innerHeight);
    if (rootH > window.innerHeight + 8) {
      return {
        kind: 'window',
        el: root,
        viewW: window.innerWidth,
        viewH: window.innerHeight,
        totalH: rootH,
        rect: { left: 0, top: 0, width: window.innerWidth, height: window.innerHeight }
      };
    }

    // Fallback for SPAs with an inner scrollable container
    let bestEl = null;
    let bestScore = 0;
    const candidates = document.querySelectorAll('main, article, section, div');
    const limit = Math.min(candidates.length, 3000);
    for (let i = 0; i < limit; i++) {
      const el = candidates[i];
      if (el === hostEl || hostEl?.contains(el)) continue;
      if (el.scrollHeight <= el.clientHeight + 24) continue;
      const st = getComputedStyle(el);
      if (st.overflowY !== 'auto' && st.overflowY !== 'scroll' && st.overflowY !== 'overlay') continue;
      const rect = el.getBoundingClientRect();
      if (rect.width < 200 || rect.height < 200) continue;
      const score = rect.width * rect.height;
      if (score > bestScore) {
        bestScore = score;
        bestEl = el;
      }
    }

    if (bestEl) {
      const rect = bestEl.getBoundingClientRect();
      return {
        kind: 'element',
        el: bestEl,
        viewW: bestEl.clientWidth,
        viewH: bestEl.clientHeight,
        totalH: bestEl.scrollHeight,
        rect
      };
    }

    return {
      kind: 'window',
      el: root,
      viewW: window.innerWidth,
      viewH: window.innerHeight,
      totalH: window.innerHeight,
      rect: { left: 0, top: 0, width: window.innerWidth, height: window.innerHeight }
    };
  }

  function collectFixedElements(scrollTarget) {
    const fixedList = [];
    const all = document.querySelectorAll('*');
    const limit = Math.min(all.length, 6000);
    const viewW = window.innerWidth;
    const viewH = window.innerHeight;

    for (let i = 0; i < limit; i++) {
      const el = all[i];
      if (el === hostEl || hostEl?.contains(el)) continue;
      if (scrollTarget.kind === 'element' && (el === scrollTarget.el || el.contains(scrollTarget.el))) continue;
      const pos = getComputedStyle(el).position;
      if (pos !== 'fixed' && pos !== 'sticky') continue;
      const r = el.getBoundingClientRect();
      if (r.width === 0 || r.height === 0) continue;
      if (r.width >= viewW * 0.92 && r.height >= viewH * 0.6) continue;
      fixedList.push({ el, prevVisibility: el.style.visibility });
    }
    return fixedList;
  }

  async function copyFullPageScrollToClipboard(forceModal = false) {
    if (isScrollCapturing) return;
    if (typeof chrome === 'undefined' || !chrome.runtime?.sendMessage) {
      showToast('⚠️ 全範囲スクロール撮影は拡張機能として実行時に利用できます', 'warning');
      return;
    }

    isScrollCapturing = true;
    scrollCaptureCancelled = false;

    const target = findScrollTarget();
    const origScrollX = target.kind === 'window' ? window.scrollX : target.el.scrollLeft;
    const origScrollY = target.kind === 'window' ? window.scrollY : target.el.scrollTop;

    const scrollToY = (y) => {
      if (target.kind === 'window') {
        window.scrollTo({ left: origScrollX, top: y, behavior: 'instant' });
      } else {
        target.el.scrollTop = y;
      }
    };
    const getScrollY = () => (target.kind === 'window' ? window.scrollY : target.el.scrollTop);

    // Override smooth scroll temporarily
    const overrideStyle = document.createElement('style');
    overrideStyle.id = 'cd-scroll-capture-override';
    overrideStyle.textContent = 'html, body, * { scroll-behavior: auto !important; }';
    document.head?.appendChild(overrideStyle);

    const fixedElements = collectFixedElements(target);
    let fixedHidden = false;
    const hideFixed = () => {
      if (fixedHidden) return;
      fixedHidden = true;
      for (const item of fixedElements) {
        item.el.style.visibility = 'hidden';
      }
    };
    const restoreFixed = () => {
      if (!fixedHidden) return;
      fixedHidden = false;
      for (const item of fixedElements) {
        item.el.style.visibility = item.prevVisibility || '';
      }
    };

    try {
      const viewW = target.viewW;
      const viewH = target.viewH;
      const totalH = target.totalH;
      const maxScroll = Math.max(0, totalH - viewH);

      // Compute scale keeping within Chrome canvas size limits
      const baseDpr = window.devicePixelRatio || 1;
      const MAX_DIM = 30000;
      const MAX_AREA = 180000000;
      let scale = baseDpr;
      if (totalH * scale > MAX_DIM) scale = MAX_DIM / totalH;
      if (viewW * scale > MAX_DIM) scale = Math.min(scale, MAX_DIM / viewW);
      if (viewW * totalH * scale * scale > MAX_AREA) {
        scale = Math.min(scale, Math.sqrt(MAX_AREA / (viewW * totalH)));
      }

      const offscreen = document.createElement('canvas');
      offscreen.width = Math.max(1, Math.round(viewW * scale));
      offscreen.height = Math.max(1, Math.round(totalH * scale));
      const octx = offscreen.getContext('2d');
      octx.scale(scale, scale);

      // Build list of Y scroll positions
      const positions = [0];
      let nextY = viewH;
      const maxSteps = 60;
      while (nextY < maxScroll && positions.length < maxSteps - 1) {
        positions.push(nextY);
        nextY += viewH;
      }
      if (maxScroll > 0 && positions[positions.length - 1] < maxScroll) {
        positions.push(maxScroll);
      }

      let drawnBottom = 0;

      for (let i = 0; i < positions.length; i++) {
        if (scrollCaptureCancelled) {
          showToast('⏹️ 全範囲撮影をキャンセルしました', 'info');
          return;
        }

        const targetY = positions[i];
        scrollToY(targetY);

        if (i === 1) {
          hideFixed();
        }

        showToast(`⏳ 全範囲スクロール撮影中 (${i + 1}/${positions.length})... [Escで中断]`, 'info');
        await cdSleep(160);

        if (scrollCaptureCancelled) {
          showToast('⏹️ 全範囲撮影をキャンセルしました', 'info');
          return;
        }

        // Hide UI only during capture
        if (hostEl) hostEl.style.visibility = 'hidden';
        await cdSleep(50);

        const dataUrl = await captureTabWithThrottle();

        if (hostEl) hostEl.style.visibility = 'visible';

        const bgImg = await loadImage(dataUrl);
        const imgScaleX = bgImg.naturalWidth / window.innerWidth;
        const imgScaleY = bgImg.naturalHeight / window.innerHeight;
        const actualY = getScrollY();

        // Avoid overwriting already-drawn upper portion on the final overlapping slice
        const clipTop = Math.max(0, drawnBottom - actualY);
        const drawH = Math.min(viewH - clipTop, totalH - (actualY + clipTop));
        if (drawH > 0) {
          const sx = (target.kind === 'window' ? 0 : target.rect.left) * imgScaleX;
          const sy = ((target.kind === 'window' ? 0 : target.rect.top) + clipTop) * imgScaleY;
          const sw = viewW * imgScaleX;
          const sh = drawH * imgScaleY;

          octx.drawImage(bgImg, sx, sy, sw, sh, 0, actualY + clipTop, viewW, drawH);
          drawnBottom = Math.max(drawnBottom, actualY + clipTop + drawH);
        }
      }

      // Restore scroll before drawing annotations
      restoreFixed();
      scrollToY(origScrollY);

      // Composite doodles at their original scroll position
      if (objects.length > 0) {
        octx.save();
        if (target.kind === 'window') {
          octx.translate(0, origScrollY);
        } else {
          octx.translate(-target.rect.left, origScrollY - target.rect.top);
        }
        for (const obj of objects) {
          drawObject(octx, obj);
        }
        octx.restore();
      }

      const blob = await new Promise(resolve => offscreen.toBlob(b => resolve(b), 'image/png'));
      await tryCopyBlobOrShowModal(blob, 'ページ全範囲スクショ', viewW, totalH, forceModal);
    } catch (err) {
      console.error('[ChromeDoodle] Scroll capture error:', err);
      showToast(`❌ 全範囲撮影に失敗しました: ${err?.message || err}`, 'error');
    } finally {
      overrideStyle.remove();
      restoreFixed();
      scrollToY(origScrollY);
      if (hostEl) hostEl.style.visibility = 'visible';
      isScrollCapturing = false;
      scrollCaptureCancelled = false;
    }
  }

  // --- Desktop / Window Capture ---
  async function handleDesktopCapturedDataUrl(dataUrl, forceModal = false) {
    try {
      const img = await loadImage(dataUrl);
      const res = await fetch(dataUrl);
      const blob = await res.blob();
      await tryCopyBlobOrShowModal(blob, 'Desktop / Window 画面', img.naturalWidth, img.naturalHeight, forceModal);
    } catch (err) {
      console.error('[ChromeDoodle] Desktop capture result error:', err);
      showToast('❌ 画面採取の処理に失敗しました', 'error');
    }
  }

  async function captureDesktopOrWindow(forceModal = false) {
    // Prefer direct getDisplayMedia when available in a secure context
    if (window.isSecureContext && navigator.mediaDevices?.getDisplayMedia) {
      const startedAt = Date.now();
      let stream = null;
      try {
        showToast('🖥️ 採取する画面（Desktop / Window / タブ）を選択してください', 'info');
        stream = await navigator.mediaDevices.getDisplayMedia({
          video: { cursor: 'always' },
          audio: false
        });

        const video = document.createElement('video');
        video.muted = true;
        video.playsInline = true;
        video.srcObject = stream;
        await video.play();

        await new Promise((resolve) => {
          const check = () => {
            if (video.readyState >= 2 && video.videoWidth > 0 && video.videoHeight > 0) {
              resolve();
            } else {
              requestAnimationFrame(check);
            }
          };
          check();
        });

        if (hostEl) hostEl.style.visibility = 'hidden';
        await cdSleep(120);

        const offscreen = document.createElement('canvas');
        offscreen.width = video.videoWidth;
        offscreen.height = video.videoHeight;
        const octx = offscreen.getContext('2d');
        octx.drawImage(video, 0, 0, offscreen.width, offscreen.height);

        if (hostEl) hostEl.style.visibility = 'visible';
        stream.getTracks().forEach(t => t.stop());
        stream = null;

        const blob = await new Promise(resolve => offscreen.toBlob(b => resolve(b), 'image/png'));
        await tryCopyBlobOrShowModal(blob, 'Desktop / Window 画面', offscreen.width, offscreen.height, forceModal);
        return;
      } catch (err) {
        if (hostEl) hostEl.style.visibility = 'visible';
        if (stream) {
          stream.getTracks().forEach(t => t.stop());
          stream = null;
        }
        const elapsed = Date.now() - startedAt;
        // If rejected immediately (< 350ms), likely blocked by Permissions-Policy -> fallback to extension window
        if (err.name === 'NotAllowedError' && elapsed < 350 && typeof chrome !== 'undefined' && chrome.runtime?.sendMessage) {
          await openDesktopCapturePopup();
          return;
        }
        if (err.name === 'AbortError' || err.name === 'NotAllowedError') {
          showToast('⏹️ 画面採取をキャンセルしました', 'info');
          return;
        }
        if (typeof chrome !== 'undefined' && chrome.runtime?.sendMessage) {
          await openDesktopCapturePopup();
          return;
        }
        showToast(`❌ 画面採取エラー: ${err.message || err}`, 'error');
        return;
      }
    }

    // Fallback for non-HTTPS (insecure) pages via extension popup
    if (typeof chrome !== 'undefined' && chrome.runtime?.sendMessage) {
      await openDesktopCapturePopup();
    } else {
      showToast('⚠️ この環境では Desktop / Window 採取がサポートされていません', 'warning');
    }
  }

  async function openDesktopCapturePopup() {
    try {
      showToast('🖥️ キャプチャ用ウィンドウを開きます...', 'info');
      const res = await chrome.runtime.sendMessage({ type: 'CHROME_DOODLE_OPEN_DESKTOP_CAPTURE' });
      if (!res?.ok) {
        showToast(`❌ キャプチャウィンドウを開けませんでした: ${res?.error || ''}`, 'error');
      }
    } catch (err) {
      console.error('[ChromeDoodle] Open desktop capture popup failed:', err);
      showToast('❌ Desktop / Window 採取の起動に失敗しました', 'error');
    }
  }

  // --- Clipboard Integration: Paste Image Overlay ---
  function setupPasteHandler() {
    window.addEventListener('paste', async (e) => {
      if (!isActive) return;
      // If typing inside an input or textarea that is not our canvas
      if (e.target.tagName === 'INPUT' || (e.target.tagName === 'TEXTAREA' && e.target !== textEditorEl)) {
        return;
      }

      // 1. Check if clipboard has our doodle object JSON
      let doodleJson = null;
      try {
        const text = e.clipboardData?.getData('text/plain');
        if (text && text.includes('__chromedoodle')) {
          const parsed = JSON.parse(text);
          if (parsed && parsed.__chromedoodle && parsed.obj) {
            doodleJson = parsed.obj;
          }
        }
      } catch (_) {}

      if (doodleJson) {
        e.preventDefault();
        pasteDoodleObject(doodleJson);
        return;
      }

      // 2. Check if clipboard contains an image file
      const items = e.clipboardData?.items;
      if (items) {
        for (let i = 0; i < items.length; i++) {
          if (items[i].type.startsWith('image/')) {
            e.preventDefault();
            const file = items[i].getAsFile();
            if (file) {
              await addImageBlobToDoodle(file);
              return;
            }
          }
        }
      }

      // 3. Fallback to in-memory copied doodle object
      if (copiedDoodleObject) {
        e.preventDefault();
        pasteDoodleObject(copiedDoodleObject);
        return;
      }
    });
  }

  function pasteDoodleObject(source) {
    if (!source) return;
    const newObj = cloneDoodleObject(source);
    const type = source.type || 'obj';
    newObj.id = `${type}_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`;

    // Slightly offset the position by 24px so the duplicate is clearly visible
    const offset = 24;
    if (newObj.points) {
      newObj.points = newObj.points.map(p => ({ x: p.x + offset, y: p.y + offset }));
    }
    if (typeof newObj.x1 === 'number') {
      newObj.x1 += offset;
      newObj.y1 += offset;
      newObj.x2 += offset;
      newObj.y2 += offset;
    }
    if (typeof newObj.x === 'number') {
      newObj.x += offset;
      newObj.y += offset;
    }
    if (typeof newObj.cx === 'number') {
      newObj.cx += offset;
      newObj.cy += offset;
    }

    objects.push(newObj);
    selectedObject = newObj;
    setTool('select');
    pushAction({
      type: 'ADD',
      object: newObj
    });
    render();

    const nameMap = {
      stroke: '線',
      line: '直線',
      arrow: '矢印',
      rect: '矩形',
      ellipse: '楕円',
      text: 'テキスト',
      image: '画像'
    };
    showToast(`📋 ${nameMap[type] || 'オブジェクト'}を貼り付けました`, 'success');
  }

  async function pasteImageFromClipboard() {
    if (!navigator.clipboard || typeof navigator.clipboard.read !== 'function') {
      if (copiedDoodleObject) {
        pasteDoodleObject(copiedDoodleObject);
        return;
      }
      showToast('⚠️ このページではボタンからの貼付が制限されています。Ctrl+V (Cmd+V) をお試しください', 'info');
      return;
    }
    try {
      const items = await navigator.clipboard.read();
      for (const item of items) {
        const imageType = item.types.find(t => t.startsWith('image/'));
        if (imageType) {
          const blob = await item.getType(imageType);
          await addImageBlobToDoodle(blob);
          return;
        }
      }
      if (copiedDoodleObject) {
        pasteDoodleObject(copiedDoodleObject);
        return;
      }
      showToast('⚠️ クリップボードに画像が見つかりません', 'warning');
    } catch (err) {
      if (copiedDoodleObject) {
        pasteDoodleObject(copiedDoodleObject);
        return;
      }
      showToast('⚠️ クリップボード貼付は Ctrl+V (Cmd+V) をお試しください', 'info');
    }
  }

  async function addImageBlobToDoodle(blob) {
    try {
      const dataUrl = await readBlobAsDataURL(blob);
      const img = await loadImage(dataUrl);

      const dpr = window.devicePixelRatio || 1;
      let w = Math.round((img.naturalWidth || 300) / dpr);
      let h = Math.round((img.naturalHeight || 200) / dpr);

      // Only shrink if larger than viewport
      const maxW = window.innerWidth - 40;
      const maxH = window.innerHeight - 40;
      if (w > maxW || h > maxH) {
        const ratio = Math.min(maxW / w, maxH / h);
        w = Math.round(w * ratio);
        h = Math.round(h * ratio);
      }

      // Center on screen
      const x = Math.round((window.innerWidth - w) / 2);
      const y = Math.round((window.innerHeight - h) / 2);

      const imgCount = objects.filter(o => o.type === 'image').length;
      const offsetX = (imgCount * 20) % 120;
      const offsetY = (imgCount * 20) % 120;

      const imgObj = {
        id: 'img_' + Date.now() + '_' + Math.random().toString(36).substr(2, 7),
        type: 'image',
        img: img,
        dataUrl: dataUrl,
        x: Math.min(window.innerWidth - w - 20, Math.max(20, x + offsetX)),
        y: Math.min(window.innerHeight - h - 20, Math.max(20, y + offsetY)),
        w: w,
        h: h,
        // 元画像の表示範囲(フレーミング用)。初期は全体
        sx: 0,
        sy: 0,
        sw: img.naturalWidth || w,
        sh: img.naturalHeight || h,
        // 反転状態 (Undo 復元用に明示)
        flipX: false,
        flipY: false
      };

      objects.push(imgObj);
      selectedObject = imgObj;
      setTool('select');

      pushAction({
        type: 'ADD',
        object: imgObj
      });
    } catch (err) {
      console.error('[ChromeDoodle] Add image error:', err);
      showToast('❌ 画像の貼り付けに失敗しました', 'error');
    }
  }

  function readBlobAsDataURL(blob) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  }

  function loadImage(src) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => resolve(img);
      img.onerror = reject;
      img.src = src;
    });
  }

  // --- Keyboard Shortcuts ---
  function setupKeyboardEvents() {
    function isTypingInput(e) {
      if (e.isComposing) return true;
      if (currentTool === 'edit' || document.designMode === 'on') return true;
      if (textEditorEl && textEditorEl.style.display === 'block') return true;
      if (shadow && shadow.activeElement === textEditorEl) return true;

      const target = e.target;
      if (target) {
        if (target === textEditorEl) return true;
        const tag = target.tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA' || target.isContentEditable) return true;
      }

      const active = document.activeElement;
      if (active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA' || active.isContentEditable)) {
        return true;
      }
      return false;
    }

    // In edit mode: prevent clicking links from navigating away
    window.addEventListener('click', (e) => {
      if (!isActive || currentTool !== 'edit') return;
      const link = e.target?.closest?.('a');
      if (link) {
        e.preventDefault();
      }
    }, true);

    // Ctrl(Cmd) 押下中 = 貼付画像のフレーミングモード (隠れ部分を半透明表示)
    const setFramingMode = (on) => {
      if (framingMode === on) return;
      framingMode = on;
      if (isActive && selectedObject?.type === 'image') {
        if (canvas) canvas.style.cursor = on ? 'crosshair' : 'default';
        render();
      }
    };
    window.addEventListener('keyup', (e) => {
      if (e.key === 'Control' || e.key === 'Meta') setFramingMode(false);
    });
    window.addEventListener('blur', () => setFramingMode(false));

    window.addEventListener('keydown', (e) => {
      if (!isActive) return;
      if (e.key === 'Control' || e.key === 'Meta') setFramingMode(true);

      const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0;
      const mod = isMac ? e.metaKey : e.ctrlKey;
      const isTyping = isTypingInput(e);

      // Escape key: cancel scroll capture, close modal, exit text editor, exit edit mode, cancel crop, or close
      if (e.key === 'Escape') {
        if (isScrollCapturing) {
          scrollCaptureCancelled = true;
          return;
        }
        if (captureModalEl && captureModalEl.style.display === 'flex') {
          closeCaptureResultModal();
          return;
        }
        if (textEditorEl && textEditorEl.style.display === 'block') {
          commitTextEditor();
        } else if (currentTool === 'edit') {
          setTool('select');
        } else if (currentTool === 'eyedropper') {
          setTool(previousTool || 'freehand');
        } else if (cropRect) {
          cropRect = null;
          render();
        } else {
          setActive(false);
        }
        return;
      }

      // Undo: Ctrl+Z / Cmd+Z (only if not typing in text editor)
      if (!isTyping && mod && !e.shiftKey && e.key.toLowerCase() === 'z') {
        e.preventDefault();
        undo();
        return;
      }

      // Redo: Ctrl+Y / Cmd+Shift+Z / Ctrl+Shift+Z (only if not typing in text editor)
      if (!isTyping && ((mod && e.key.toLowerCase() === 'y') || (mod && e.shiftKey && e.key.toLowerCase() === 'z'))) {
        e.preventDefault();
        redo();
        return;
      }

      // Copy: Ctrl+C / Cmd+C (Copies selected object if one is selected, else crop area or full screen)
      if (!isTyping && mod && !e.shiftKey && e.key.toLowerCase() === 'c') {
        e.preventDefault();
        if (selectedObject && currentTool === 'select') {
          copiedDoodleObject = cloneDoodleObject(selectedObject);
          if (selectedObject.type === 'image') {
            // 貼付画像: 現在のフレーム(表示範囲・反転込み)の画像をクリップボードへ
            copyImageObjectAsFramed(selectedObject);
            return;
          }
          writeTextToClipboard(JSON.stringify({ __chromedoodle: true, obj: copiedDoodleObject }));
          const nameMap = { stroke: '線', line: '直線', arrow: '矢印', rect: '矩形', ellipse: '楕円', text: 'テキスト', image: '画像' };
          showToast(`📋 ${nameMap[selectedObject.type] || 'オブジェクト'}をコピーしました (Ctrl+Vで貼付)`, 'info');
        } else if (cropRect) {
          copyCropAreaToClipboard();
        } else {
          copyFullViewToClipboard();
        }
        return;
      }

      // Duplicate: Ctrl+D / Cmd+D (Quick duplicate selected object)
      if (!isTyping && mod && !e.shiftKey && e.key.toLowerCase() === 'd') {
        if (selectedObject && currentTool === 'select') {
          e.preventDefault();
          pasteDoodleObject(selectedObject);
          return;
        }
      }

      // Save PNG: Ctrl+S / Cmd+S
      if (!isTyping && mod && !e.shiftKey && e.key.toLowerCase() === 's') {
        e.preventDefault();
        saveCropAreaAsImage();
        return;
      }

      // If user is currently typing text, DO NOT trigger any single-key tool shortcuts or object deletion
      if (isTyping) {
        return;
      }

      // Arrow keys: 選択オブジェクトを移動 (1px / Shift+10px)
      if (!mod && !e.altKey && selectedObject && currentTool === 'select' &&
          (e.key === 'ArrowLeft' || e.key === 'ArrowRight' || e.key === 'ArrowUp' || e.key === 'ArrowDown')) {
        e.preventDefault();
        const step = e.shiftKey ? 10 : 1;
        const dx = e.key === 'ArrowLeft' ? -step : e.key === 'ArrowRight' ? step : 0;
        const dy = e.key === 'ArrowUp' ? -step : e.key === 'ArrowDown' ? step : 0;
        const before = cloneDoodleObject(selectedObject);
        moveObject(selectedObject, before, dx, dy);
        pushAction({
          type: 'MODIFY',
          objectId: selectedObject.id,
          before,
          after: cloneDoodleObject(selectedObject)
        });
        return;
      }

      // Delete / Backspace / Ctrl+X: delete selected object
      const isCutKey = mod && !e.shiftKey && e.key.toLowerCase() === 'x';
      if ((e.key === 'Delete' || e.key === 'Backspace' || isCutKey) && selectedObject && currentTool === 'select') {
        e.preventDefault();
        if (isCutKey) {
          // Ctrl+X: Ctrl+C と同じ内部クリップボードへ保存してから削除 (Ctrl+V で貼付)
          copiedDoodleObject = cloneDoodleObject(selectedObject);
          if (selectedObject.type === 'image') {
            // 画像は Ctrl+C 同様、現在のフレームの画像(PNG)をクリップボードへ
            copyImageObjectAsFramed(selectedObject);
          } else {
            writeTextToClipboard(JSON.stringify({ __chromedoodle: true, obj: copiedDoodleObject }));
          }
        }
        const idx = objects.findIndex(o => o.id === selectedObject.id);
        if (idx !== -1) {
          const removed = objects.splice(idx, 1)[0];
          pushAction({
            type: 'REMOVE',
            object: removed,
            index: idx
          });
          selectedObject = null;
          render();
          showToast('🗑️ オブジェクトを削除しました', 'info');
        }
        return;
      }

      // Tool shortcuts (only when not typing and no modifier keys pressed)
      if (!mod) {
        const key = e.key.toLowerCase();
        if (key === 'm') setTool('select');
        else if (key === 'w') setTool('edit');
        else if (key === 'p') setTool('freehand');
        else if (key === 'l') setTool('line');
        else if (key === 'a') setTool('arrow');
        else if (key === 'r') setTool('rect');
        else if (key === 'e') setTool('ellipse');
        else if (key === 't') setTool('text');
        else if (key === 'c') setTool('crop');
        else if (key === 'i') setTool('eyedropper');
      }
    });

    // In text editor: Enter commits text if Shift is not pressed
    textEditorEl?.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        commitTextEditor();
      }
    });
  }

  // --- Toolbar Setup & Interactions ---
  function setTool(tool) {
    const prevTool = currentTool;
    currentTool = tool;

    if (tool !== 'select') {
      selectedObject = null;
    }
    if (tool !== 'crop') {
      cropRect = null;
    }
    if (tool !== 'text') {
      closeTextEditor();
    }
    if (tool !== 'stamp') {
      const stSel = shadow?.getElementById('cd-stamp-select');
      if (stSel) { stSel.value = ''; stSel.style.borderColor = ''; }
    }

    // Toggle HTML edit mode (document.designMode + block hover highlight)
    if (tool === 'edit') {
      document.designMode = 'on';
      if (canvasWrapEl) canvasWrapEl.style.pointerEvents = 'none';
      if (canvas) canvas.style.pointerEvents = 'none';
      showToast('</> HTML編集モード (ブロックをハイライト・クリックで直接編集・Escで完了)', 'info');
    } else {
      clearHtmlHoverHighlight();
      if (prevTool === 'edit' && document.designMode === 'on') {
        document.designMode = 'off';
      }
      if (canvasWrapEl) canvasWrapEl.style.pointerEvents = 'auto';
      if (canvas) canvas.style.pointerEvents = 'auto';
    }

    // Handle eyedropper mode
    if (tool === 'eyedropper') {
      if (prevTool !== 'eyedropper') {
        previousTool = prevTool;
      }
      prepareEyedropperSnapshot();
      showToast('💧 スポイト: 抽出したい場所を十字カーソルでクリックしてください (Escでキャンセル)', 'info');
    } else {
      eyedropperHoverPos = null;
    }

    // Update active class on toolbar buttons
    shadow.querySelectorAll('.cd-tool-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tool === tool);
    });
    shadow.getElementById('cd-btn-eyedropper')?.classList.toggle('active', tool === 'eyedropper');

    // Change canvas cursor
    if (canvas) {
      if (tool === 'select') canvas.style.cursor = 'default';
      else if (tool === 'text' || tool === 'edit') canvas.style.cursor = 'text';
      else if (tool === 'crop' || tool === 'eyedropper') canvas.style.cursor = 'crosshair';
      else canvas.style.cursor = 'crosshair';
    }

    render();
  }

  function setupToolbarEvents() {
    // Tool buttons
    shadow.querySelectorAll('.cd-tool-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        setTool(btn.dataset.tool);
      });
    });

    // Color buttons
    shadow.querySelectorAll('.cd-color-dot').forEach(dot => {
      dot.addEventListener('click', () => {
        currentColor = dot.dataset.color;
        updateColorSelection();
        if (selectedObject) {
          updateSelectedObjectStyle();
        }
      });
    });

    // Custom color picker
    const customPicker = shadow.getElementById('cd-custom-color');
    if (customPicker) {
      customPicker.addEventListener('input', (e) => {
        currentColor = e.target.value;
        updateColorSelection();
        if (selectedObject) {
          updateSelectedObjectStyle();
        }
      });
    }

    // Stroke width buttons
    shadow.querySelectorAll('.cd-width-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        currentStrokeWidth = parseInt(btn.dataset.width, 10);
        shadow.querySelectorAll('.cd-width-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        if (selectedObject) {
          updateSelectedObjectStyle();
        }
      });
    });

    // Fill mode buttons
    shadow.querySelectorAll('.cd-fill-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        currentFillMode = btn.dataset.fill;
        shadow.querySelectorAll('.cd-fill-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        if (selectedObject && (selectedObject.type === 'rect' || selectedObject.type === 'ellipse')) {
          selectedObject.fillColor = getFillColor(selectedObject.strokeColor || currentColor, currentFillMode);
          render();
        }
      });
    });

    // Font size selector
    const fontSelect = shadow.getElementById('cd-font-select');
    if (fontSelect) {
      fontSelect.addEventListener('change', (e) => {
        currentFontSize = parseInt(e.target.value, 10);
        if (selectedObject && selectedObject.type === 'text') {
          selectedObject.fontSize = currentFontSize;
          measureTextObject(selectedObject);
          render();
        }
      });
    }

    // Text stamp selector
    const stampSelect = shadow.getElementById('cd-stamp-select');
    if (stampSelect) {
      stampSelect.addEventListener('change', (e) => {
        let v = e.target.value;
        if (v === '__add__') {
          const custom = prompt('追加するスタンプ文字列:');
          if (!custom || !custom.trim()) { stampSelect.value = currentTool === 'stamp' ? currentStamp : ''; return; }
          v = custom.trim();
          const list = loadStamps();
          if (!list.includes(v)) {
            list.push(v);
            saveStamps(list);
            const opt = document.createElement('option');
            opt.value = v; opt.textContent = v;
            stampSelect.insertBefore(opt, stampSelect.querySelector('option[value="__add__"]'));
          }
          stampSelect.value = v;
        }
        if (!v) { setTool('select'); return; }
        currentStamp = v;
        setTool('stamp');
        stampSelect.value = v;
        stampSelect.style.borderColor = '#6366f1';
        if (typeof showToast === 'function') showToast(`スタンプ: ${v} (クリックで配置)`);
      });
    }

    // Eyedropper button in palette row (Shift+Click for native OS EyeDropper)
    shadow.getElementById('cd-btn-eyedropper')?.addEventListener('click', (e) => {
      if (e.shiftKey && typeof window.EyeDropper !== 'undefined') {
        pickColorWithNativeEyeDropper();
      } else {
        setTool('eyedropper');
      }
    });

    // Action buttons
    shadow.getElementById('cd-btn-undo')?.addEventListener('click', undo);
    shadow.getElementById('cd-btn-redo')?.addEventListener('click', redo);
    shadow.getElementById('cd-btn-clear')?.addEventListener('click', clearAll);
    shadow.getElementById('cd-btn-paste')?.addEventListener('click', pasteImageFromClipboard);
    shadow.getElementById('cd-btn-fullcopy')?.addEventListener('click', copyFullViewToClipboard);
    shadow.getElementById('cd-btn-scrollcopy')?.addEventListener('click', (e) => copyFullPageScrollToClipboard(e.shiftKey));
    shadow.getElementById('cd-btn-desktop-capture')?.addEventListener('click', (e) => captureDesktopOrWindow(e.shiftKey));
    shadow.getElementById('cd-btn-download')?.addEventListener('click', saveCropAreaAsImage);
    shadow.getElementById('cd-btn-close')?.addEventListener('click', () => setActive(false));

    // Dragging Toolbar from anywhere on the dialog
    let isDragPending = false;
    let isDraggingToolbar = false;
    let suppressNextToolbarClick = false;
    let tbStartX = 0, tbStartY = 0, tbInitX = 0, tbInitY = 0;

    toolbarEl.addEventListener('mousedown', (e) => {
      if (e.button !== 0) return;
      if (e.target.closest('select, input[type="color"]')) return;
      isDragPending = true;
      isDraggingToolbar = false;
      suppressNextToolbarClick = false;
      tbStartX = e.clientX;
      tbStartY = e.clientY;
      const rect = toolbarEl.getBoundingClientRect();
      tbInitX = rect.left;
      tbInitY = rect.top;
      e.preventDefault();
    });

    window.addEventListener('mousemove', (e) => {
      if (!isDragPending && !isDraggingToolbar) return;
      const dx = e.clientX - tbStartX;
      const dy = e.clientY - tbStartY;
      if (!isDraggingToolbar) {
        if (Math.hypot(dx, dy) < 4) return;
        isDraggingToolbar = true;
        suppressNextToolbarClick = true;
        toolbarEl.classList.add('positioned', 'dragging');
      }
      const inv = 1 / (currentPageZoom || 1);
      const margin = 8 * inv;
      const rect = toolbarEl.getBoundingClientRect();
      const maxX = Math.max(margin, window.innerWidth - rect.width - margin);
      const maxY = Math.max(margin, window.innerHeight - rect.height - margin);
      // 最小化中(左端ハンドル)は上下のみ移動
      const newX = toolbarEl.classList.contains('minimized') ? 0 : Math.max(margin, Math.min(maxX, tbInitX + dx));
      const newY = Math.max(margin, Math.min(maxY, tbInitY + dy));
      toolbarEl.style.right = 'auto';
      toolbarEl.style.left = `${newX}px`;
      toolbarEl.style.top = `${newY}px`;
    });

    window.addEventListener('mouseup', () => {
      if (!isDragPending && !isDraggingToolbar) return;
      isDragPending = false;
      if (isDraggingToolbar) {
        isDraggingToolbar = false;
        toolbarEl.classList.remove('dragging');
        setTimeout(() => { suppressNextToolbarClick = false; }, 50);
      }
    });

    toolbarEl.addEventListener('click', (e) => {
      if (suppressNextToolbarClick) {
        e.preventDefault();
        e.stopPropagation();
        suppressNextToolbarClick = false;
      }
    }, true);

    // Minimize / Expand toggle
    const minBtn = shadow.querySelector('.cd-toolbar-min');
    // 最小化: ハンドルだけ残して画面左端へ移動。再クリックで元の位置に展開
    let savedToolbarPos = null;
    minBtn?.addEventListener('click', () => {
      const rect = toolbarEl.getBoundingClientRect();
      if (!toolbarEl.classList.contains('minimized')) {
        savedToolbarPos = {
          positioned: toolbarEl.classList.contains('positioned'),
          left: rect.left,
          top: rect.top
        };
        toolbarEl.classList.add('minimized', 'positioned');
        toolbarEl.style.right = 'auto';
        toolbarEl.style.left = '0px';
        toolbarEl.style.top = `${Math.max(0, rect.top)}px`;
        minBtn.title = 'クリックで展開 / ドラッグで移動';
      } else {
        toolbarEl.classList.remove('minimized');
        // 左右位置は最小化前に戻す。上下は最小化中に動かした高さを維持
        if (savedToolbarPos) {
          toolbarEl.style.left = `${savedToolbarPos.left}px`;
        }
        minBtn.title = 'クリックで最小化 (画面左端へ) / 展開、ドラッグで移動';
        clampToolbarToViewport();
      }
    });
  }

  async function pickColorWithNativeEyeDropper() {
    if (typeof window.EyeDropper !== 'undefined') {
      try {
        const eyeDropper = new EyeDropper();
        const result = await eyeDropper.open();
        if (result && result.sRGBHex) {
          currentColor = result.sRGBHex;
          updateColorSelection();
          if (selectedObject) {
            updateSelectedObjectStyle();
          }
          showToast(`💧 色を抽出しました: ${currentColor}`, 'success');
        }
      } catch (err) {
        if (err.name !== 'AbortError') {
          console.warn('[ChromeDoodle] EyeDropper error:', err);
        }
      }
    } else {
      showToast('⚠️ このブラウザ環境ではEyeDropper APIに対応していません', 'warning');
    }
  }

  function updateColorSelection() {
    shadow.querySelectorAll('.cd-color-dot').forEach(dot => {
      dot.classList.toggle('active', dot.dataset.color.toLowerCase() === currentColor.toLowerCase());
    });
    const picker = shadow.getElementById('cd-custom-color');
    if (picker) picker.value = currentColor;
  }

  function updateSelectedObjectStyle() {
    if (!selectedObject) return;
    const beforeState = JSON.parse(JSON.stringify(selectedObject));

    if (selectedObject.type === 'stroke' || selectedObject.type === 'line' || selectedObject.type === 'arrow') {
      selectedObject.color = currentColor;
      selectedObject.width = currentStrokeWidth;
    } else if (selectedObject.type === 'rect' || selectedObject.type === 'ellipse') {
      selectedObject.strokeColor = currentColor;
      selectedObject.width = currentStrokeWidth;
      selectedObject.fillColor = getFillColor(currentColor, currentFillMode);
    } else if (selectedObject.type === 'text') {
      selectedObject.color = currentColor;
      selectedObject.fontSize = currentFontSize;
      measureTextObject(selectedObject);
    }

    pushAction({
      type: 'MODIFY',
      objectId: selectedObject.id,
      before: beforeState,
      after: JSON.parse(JSON.stringify(selectedObject))
    });
    render();
  }

  // --- Toast Notification ---
  let toastTimer = null;
  function showToast(msg, type = 'info') {
    if (!toastEl) return;
    toastEl.textContent = msg;
    toastEl.className = `cd-toast ${type} show`;

    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toastEl.classList.remove('show');
    }, 3000);
  }

  // --- Listen to Runtime Messages from Background or Popup ---
  if (typeof chrome !== 'undefined' && chrome.runtime?.onMessage) {
    chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
      if (message.type === 'CHROME_DOODLE_TOGGLE') {
        setActive(!isActive);
        sendResponse({ active: isActive });
        return true;
      }
      if (message.type === 'CHROME_DOODLE_START') {
        setActive(true);
        sendResponse({ active: true });
        return true;
      }
      if (message.type === 'CHROME_DOODLE_STOP') {
        setActive(false);
        sendResponse({ active: false });
        return true;
      }
      if (message.type === 'CHROME_DOODLE_ZOOM_CHANGED') {
        applyPageZoom(message.zoom);
        sendResponse({ ok: true });
        return true;
      }
      if (message.type === 'CHROME_DOODLE_DESKTOP_CAPTURED') {
        if (message.success && message.dataUrl) {
          handleDesktopCapturedDataUrl(message.dataUrl);
        } else if (message.cancelled) {
          showToast('⏹️ 画面採取をキャンセルしました', 'info');
        }
        sendResponse({ ok: true });
        return true;
      }
    });
  }

  // --- HTML Builder for Toolbar ---
  function buildToolbarHTML() {
    return `
      <!-- Left handle: ダイアログ左側 上〜下の縦長ハンドル (クリックで最小化/展開) -->
      <div class="cd-toolbar-min" title="クリックで最小化 (画面左端へ) / 展開、ドラッグで移動">
        <svg width="14" height="22" viewBox="0 0 14 24" fill="currentColor"><circle cx="4" cy="6" r="1.7"/><circle cx="10" cy="6" r="1.7"/><circle cx="4" cy="12" r="1.7"/><circle cx="10" cy="12" r="1.7"/><circle cx="4" cy="18" r="1.7"/><circle cx="10" cy="18" r="1.7"/></svg>
      </div>
      <div class="cd-toolbar-body">
      <!-- Row 1: Drawing & Editing Tools (9) -->
      <div class="cd-section cd-tools-group">
        <button class="cd-tool-btn" data-tool="select" title="選択・移動・オブジェクト編集 (M)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 3l7 18 3-7 7-3L3 3z"/></svg>
        </button>
        <button class="cd-tool-btn" data-tool="edit" title="HTML編集モード (W)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 8 2 12 6 16"/><polyline points="18 8 22 12 18 16"/><line x1="14.5" y1="4" x2="9.5" y2="20"/></svg>
        </button>
        <button class="cd-tool-btn active" data-tool="freehand" title="フリーハンド描画 (P)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/><path d="M2 2l7.586 7.586"/></svg>
        </button>
        <button class="cd-tool-btn" data-tool="line" title="直線 (L)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="4" y1="20" x2="20" y2="4"/></svg>
        </button>
        <button class="cd-tool-btn" data-tool="arrow" title="矢印 (A)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="19" x2="19" y2="5"/><polyline points="9 5 19 5 19 15"/></svg>
        </button>
        <button class="cd-tool-btn" data-tool="rect" title="矩形 (R)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/></svg>
        </button>
        <button class="cd-tool-btn" data-tool="ellipse" title="楕円 (E)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><ellipse cx="12" cy="12" rx="9" ry="7"/></svg>
        </button>
        <button class="cd-tool-btn" data-tool="text" title="テキスト配置・既存テキスト編集 (T)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="4 7 4 4 20 4 20 7"/><line x1="9" y1="20" x2="15" y2="20"/><line x1="12" y1="4" x2="12" y2="20"/></svg>
        </button>
        <button class="cd-tool-btn cd-crop-btn" data-tool="crop" title="暗転クロップエリア指定・領域コピー (C)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2v14a2 2 0 0 0 2 2h14"/><path d="M18 22V8a2 2 0 0 0-2-2H2"/></svg>
        </button>
      </div>

      <!-- Row 2: Color Palette (Eyedropper + 9 presets + custom picker) -->
      <div class="cd-section cd-palette-group">
        <button id="cd-btn-eyedropper" class="cd-eyedropper-btn" title="画面から色を抽出 (スポイト: I / Shift+クリックでOS全体)">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 2a3 3 0 0 1 3 3c0 .8-.3 1.5-.9 2.1l-1.6 1.6-4.2-4.2 1.6-1.6c.6-.6 1.3-.9 2.1-.9z"/>
            <path d="m14.3 4.5 4.2 4.2"/>
            <path d="M14.3 8.7 6.5 16.5H3v-3.5l7.8-7.8"/>
            <path d="m2 22 2-2"/>
            <circle cx="2" cy="22" r="1" fill="currentColor"/>
          </svg>
        </button>
        ${PRESET_COLORS.map(c => `
          <div class="cd-color-dot ${c.toLowerCase() === currentColor.toLowerCase() ? 'active' : ''}" data-color="${c}" style="background-color: ${c};"></div>
        `).join('')}
        <input type="color" id="cd-custom-color" value="${currentColor}" title="カスタム色選択" />
      </div>

      <!-- Row 3: Stroke Width, Fill & Font Size -->
      <div class="cd-section cd-stroke-group">
        ${STROKE_WIDTHS.map(w => `
          <button class="cd-width-btn ${w === currentStrokeWidth ? 'active' : ''}" data-width="${w}" title="線の太さ ${w}px">
            <span style="width:${Math.max(4, w)}px; height:${Math.max(4, w)}px;"></span>
          </button>
        `).join('')}

        <div class="cd-fill-group" title="塗りつぶし設定">
          <button class="cd-fill-btn active" data-fill="none" title="塗りなし (枠線のみ)">∅</button>
          <button class="cd-fill-btn" data-fill="semi" title="半透明塗りつぶし">◩</button>
          <button class="cd-fill-btn" data-fill="solid" title="不透明塗りつぶし">◼</button>
        </div>

        <select id="cd-font-select" class="cd-font-select" title="フォントサイズ">
          ${FONT_SIZES.map(s => `<option value="${s}" ${s === currentFontSize ? 'selected' : ''}>${s}px</option>`).join('')}
        </select>
        <select id="cd-stamp-select" class="cd-font-select" title="文字スタンプ (選択後クリックで連続配置)">
          <option value="">スタンプ…</option>
          ${loadStamps().map(s => `<option value="${s.replace(/"/g, '&quot;')}">${s.replace(/</g, '&lt;')}</option>`).join('')}
          <option value="__add__">+ 追加…</option>
        </select>
      </div>

      <!-- Row 4: Actions -->
      <div class="cd-section cd-actions-group">
        <button id="cd-btn-clear" class="cd-action-icon-btn clear-btn" title="全消去">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        </button>
        <button id="cd-btn-paste" class="cd-action-icon-btn" title="クリップボードから画像を重ね貼り (Ctrl+V)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg>
        </button>
        <button id="cd-btn-undo" class="cd-action-icon-btn disabled" title="元に戻す (Ctrl+Z)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>
        </button>
        <button id="cd-btn-redo" class="cd-action-icon-btn disabled" title="やり直す (Ctrl+Y)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
        </button>
        <button id="cd-btn-fullcopy" class="cd-action-icon-btn" title="表示範囲をクリップボードにコピー (Ctrl+C)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
        </button>
        <button id="cd-btn-scrollcopy" class="cd-action-icon-btn" title="ページ全範囲スクロール撮影してコピー (Shift+クリックでプレビュー)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v18"/><polyline points="8 7 12 3 16 7"/><polyline points="8 17 12 21 16 17"/></svg>
        </button>
        <button id="cd-btn-desktop-capture" class="cd-action-icon-btn" title="Desktop / Window 画面を採取してコピー (Shift+クリックでプレビュー)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
        </button>
        <button id="cd-btn-download" class="cd-action-icon-btn" title="PNG画像として保存 (Ctrl+S)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
        </button>
        <button id="cd-btn-close" class="cd-action-icon-btn close-btn" title="ChromeDoodleを閉じる (Esc)">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      </div>
    `;
  }

  // --- CSS Styles for Shadow Root ---
  function getShadowStyles() {
    return `
      :host {
        all: initial;
        user-select: none;
        -webkit-user-select: none;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      }

      .cd-canvas-wrap {
        position: fixed;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        z-index: 2147483640;
        pointer-events: auto;
      }

      .cd-main-canvas {
        display: block;
        width: 100vw;
        height: 100vh;
        cursor: crosshair;
        touch-action: none;
      }

      /* Fixed 4-Row Floating Toolbar (draggable anywhere, zoom-independent) */
      .cd-toolbar {
        position: fixed;
        top: calc(16px * var(--cd-inv-zoom, 1));
        left: 50%;
        right: auto;
        transform-origin: top center;
        transform: translateX(-50%) scale(var(--cd-inv-zoom, 1));
        z-index: 2147483645;
        width: max-content;
        max-width: calc(100vw / var(--cd-inv-zoom, 1) - 16px);
        background: rgba(24, 24, 27, 0.92);
        backdrop-filter: blur(16px);
        -webkit-backdrop-filter: blur(16px);
        border: 1px solid rgba(255, 255, 255, 0.16);
        border-radius: 14px;
        box-shadow: 0 12px 36px rgba(0, 0, 0, 0.45), 0 2px 8px rgba(0, 0, 0, 0.3);
        display: flex;
        flex-direction: row;
        align-items: stretch;
        padding: 0;
        box-sizing: border-box;
        color: #f4f4f5;
        font-size: 13px;
        cursor: grab;
        overflow: hidden;
      }

      .cd-toolbar-body {
        display: flex;
        flex-direction: column;
        align-items: center;
        row-gap: 4px;
        padding: 7px 10px 7px 6px;
        box-sizing: border-box;
        min-width: 0;
      }

      .cd-toolbar.positioned {
        transform-origin: top left;
        transform: scale(var(--cd-inv-zoom, 1));
      }

      .cd-toolbar.dragging,
      .cd-toolbar.dragging * {
        cursor: grabbing !important;
      }

      .cd-toolbar.minimized {
        border-left: none;
        border-radius: 0 14px 14px 0;
      }
      .cd-toolbar.minimized .cd-toolbar-body {
        display: none !important;
      }
      .cd-toolbar.minimized .cd-toolbar-min {
        min-height: 48px;
        border-radius: 0 14px 14px 0;
        border-right: none;
      }

      .cd-section {
        display: flex;
        align-items: center;
        justify-content: center;
        flex-wrap: nowrap;
        gap: 3px;
        flex-shrink: 0;
      }

      .cd-tool-btn,
      .cd-action-icon-btn {
        background: transparent;
        border: 1px solid transparent;
        color: #d4d4d8;
        border-radius: 8px;
        width: 30px;
        height: 30px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: background 0.15s, color 0.15s, transform 0.1s;
        padding: 0;
      }
      .cd-tool-btn:hover,
      .cd-action-icon-btn:hover {
        background: rgba(255, 255, 255, 0.12);
        color: #ffffff;
      }
      .cd-tool-btn.active {
        background: #6366f1;
        color: #ffffff;
        box-shadow: 0 0 12px rgba(99, 102, 241, 0.5);
      }
      .cd-action-icon-btn.disabled {
        opacity: 0.3;
        cursor: not-allowed;
        pointer-events: none;
      }
      .cd-action-icon-btn.close-btn:hover {
        background: rgba(239, 68, 68, 0.25);
        color: #f87171;
      }
      .cd-action-icon-btn.clear-btn {
        color: #ef4444;
      }
      .cd-action-icon-btn.clear-btn:hover {
        background: rgba(239, 68, 68, 0.25);
        color: #f87171;
      }

      /* Color Palette */
      .cd-palette-group {
        gap: 5px;
      }
      .cd-eyedropper-btn {
        background: rgba(255, 255, 255, 0.12);
        border: 1px solid rgba(255, 255, 255, 0.22);
        color: #f8fafc;
        border-radius: 7px;
        width: 24px;
        height: 24px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        padding: 0;
        margin-right: 2px;
        transition: all 0.15s ease;
      }
      .cd-eyedropper-btn:hover {
        background: rgba(56, 189, 248, 0.25);
        border-color: #38bdf8;
        color: #38bdf8;
        transform: scale(1.08);
      }
      .cd-eyedropper-btn.active {
        background: #6366f1;
        border-color: #6366f1;
        color: #ffffff;
        box-shadow: 0 0 12px rgba(99, 102, 241, 0.5);
      }
      .cd-color-dot {
        width: 17px;
        height: 17px;
        border-radius: 50%;
        cursor: pointer;
        border: 2px solid transparent;
        box-shadow: 0 1px 3px rgba(0,0,0,0.4);
        transition: transform 0.15s, border-color 0.15s;
      }
      .cd-color-dot:hover {
        transform: scale(1.15);
      }
      .cd-color-dot.active {
        border-color: #ffffff;
        transform: scale(1.22);
      }
      #cd-custom-color {
        width: 22px;
        height: 20px;
        border-radius: 5px;
        border: none;
        background: transparent;
        cursor: pointer;
        padding: 0;
      }

      /* Stroke Widths & Fill Mode */
      .cd-width-btn {
        background: transparent;
        border: 1px solid transparent;
        border-radius: 6px;
        width: 26px;
        height: 26px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
      }
      .cd-width-btn span {
        background: #a1a1aa;
        border-radius: 50%;
        display: block;
      }
      .cd-width-btn:hover {
        background: rgba(255, 255, 255, 0.1);
      }
      .cd-width-btn.active {
        background: rgba(255, 255, 255, 0.2);
      }
      .cd-width-btn.active span {
        background: #ffffff;
      }

      .cd-fill-group {
        display: flex;
        background: rgba(255, 255, 255, 0.08);
        border-radius: 6px;
        padding: 2px;
        margin-left: 2px;
      }
      .cd-fill-btn {
        background: transparent;
        border: none;
        color: #a1a1aa;
        font-size: 13px;
        width: 22px;
        height: 22px;
        border-radius: 4px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        padding: 0;
      }
      .cd-fill-btn:hover {
        color: #ffffff;
      }
      .cd-fill-btn.active {
        background: #6366f1;
        color: #ffffff;
      }

      .cd-font-select {
        background: rgba(255, 255, 255, 0.1);
        color: #e4e4e7;
        border: 1px solid rgba(255, 255, 255, 0.15);
        border-radius: 6px;
        padding: 2px 4px;
        font-size: 12px;
        outline: none;
        cursor: pointer;
        margin-left: 2px;
      }
      .cd-font-select option {
        background: #27272a;
        color: #ffffff;
      }

      .cd-toolbar-min {
        cursor: pointer;
        color: #71717a;
        width: 20px;
        flex-shrink: 0;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border-radius: 14px 0 0 14px;
        border-right: 1px solid rgba(255, 255, 255, 0.08);
        font-size: 13px;
        line-height: 1;
      }
      .cd-toolbar-min:hover {
        color: #ffffff;
        background: rgba(255, 255, 255, 0.1);
      }

      /* Text Editor Overlay */
      .cd-text-editor {
        display: none;
        position: fixed;
        z-index: 2147483646;
        background: rgba(0, 0, 0, 0.7);
        backdrop-filter: blur(8px);
        border: 2px dashed #6366f1;
        border-radius: 6px;
        padding: 6px 10px;
        outline: none;
        resize: both;
        min-width: 140px;
        min-height: 48px;
        font-family: sans-serif;
        font-weight: bold;
        line-height: 1.25;
        box-shadow: 0 8px 24px rgba(0,0,0,0.5);
      }

      /* HTML Edit Mode Hover Highlight */
      .cd-html-hover-box {
        display: none;
        position: fixed;
        top: 0;
        left: 0;
        z-index: 2147483643;
        pointer-events: none;
        box-sizing: border-box;
        border: calc(2px * var(--cd-inv-zoom, 1)) solid #6366f1;
        background: rgba(99, 102, 241, 0.06);
        border-radius: 3px;
        box-shadow: 0 0 0 calc(1px * var(--cd-inv-zoom, 1)) rgba(255, 255, 255, 0.65);
      }

      /* Crop Overlay & Actions */
      .cd-crop-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        z-index: 2147483644;
        pointer-events: none;
      }

      .cd-crop-info {
        position: fixed;
        background: rgba(15, 23, 42, 0.85);
        backdrop-filter: blur(8px);
        color: #38bdf8;
        font-size: 12px;
        font-weight: 600;
        padding: 3px 8px;
        border-radius: 4px;
        border: 1px solid rgba(56, 189, 248, 0.3);
        pointer-events: none;
        transform-origin: bottom left;
        transform: scale(var(--cd-inv-zoom, 1));
      }

      .cd-crop-actions {
        position: fixed;
        display: flex;
        align-items: center;
        gap: 3px;
        background: rgba(24, 24, 27, 0.92);
        backdrop-filter: blur(16px);
        -webkit-backdrop-filter: blur(16px);
        padding: 5px 8px;
        border-radius: 14px;
        border: 1px solid rgba(255, 255, 255, 0.16);
        box-shadow: 0 12px 36px rgba(0, 0, 0, 0.45), 0 2px 8px rgba(0, 0, 0, 0.3);
        pointer-events: auto;
        transform-origin: top left;
        transform: scale(var(--cd-inv-zoom, 1));
      }

      .cd-action-btn {
        background: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.15);
        color: #f4f4f5;
        padding: 6px 12px;
        font-size: 12px;
        font-weight: 500;
        border-radius: 6px;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        transition: background 0.15s, transform 0.1s;
      }
      .cd-action-btn:hover {
        background: rgba(255, 255, 255, 0.2);
      }
      .cd-action-btn.primary {
        background: #0284c7;
        border-color: #38bdf8;
        color: #ffffff;
      }
      .cd-action-btn.primary:hover {
        background: #0369a1;
      }
      .cd-action-btn.danger:hover {
        background: rgba(239, 68, 68, 0.25);
        border-color: #f87171;
        color: #f87171;
      }

      /* Capture Result Modal */
      .cd-capture-modal {
        display: none;
        position: fixed;
        inset: 0;
        z-index: 2147483647;
        background: rgba(0, 0, 0, 0.65);
        backdrop-filter: blur(6px);
        align-items: center;
        justify-content: center;
        padding: 20px;
      }
      .cd-modal-card {
        background: #18181b;
        border: 1px solid rgba(255, 255, 255, 0.18);
        border-radius: 14px;
        box-shadow: 0 20px 50px rgba(0, 0, 0, 0.65);
        max-width: min(760px, calc(92vw / var(--cd-inv-zoom, 1)));
        max-height: calc(88vh / var(--cd-inv-zoom, 1));
        display: flex;
        flex-direction: column;
        gap: 10px;
        padding: 14px 16px;
        color: #f4f4f5;
        transform-origin: center center;
        transform: scale(var(--cd-inv-zoom, 1));
      }
      .cd-modal-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 13px;
        font-weight: 600;
      }
      .cd-modal-close-btn {
        background: rgba(255, 255, 255, 0.1);
        border: none;
        color: #d4d4d8;
        border-radius: 6px;
        width: 26px;
        height: 26px;
        cursor: pointer;
      }
      .cd-modal-close-btn:hover {
        background: rgba(239, 68, 68, 0.3);
        color: #ffffff;
      }
      .cd-modal-note {
        font-size: 12px;
        color: #fbbf24;
        background: rgba(234, 179, 8, 0.12);
        border: 1px solid rgba(234, 179, 8, 0.3);
        border-radius: 8px;
        padding: 6px 10px;
      }
      .cd-modal-preview {
        overflow: auto;
        max-height: 60vh;
        background: #09090b;
        border-radius: 8px;
        border: 1px solid rgba(255, 255, 255, 0.1);
        display: flex;
        justify-content: center;
        align-items: flex-start;
        padding: 8px;
      }
      .cd-modal-preview img {
        max-width: 100%;
        height: auto;
        display: block;
      }
      .cd-modal-actions {
        display: flex;
        justify-content: flex-end;
        flex-wrap: wrap;
        gap: 8px;
      }

      /* Toast Notification */
      .cd-toast {
        position: fixed;
        bottom: calc(30px * var(--cd-inv-zoom, 1));
        left: 50%;
        transform-origin: bottom center;
        transform: translateX(-50%) translateY(calc(30px * var(--cd-inv-zoom, 1))) scale(var(--cd-inv-zoom, 1));
        background: rgba(24, 24, 27, 0.95);
        backdrop-filter: blur(12px);
        color: #ffffff;
        font-size: 14px;
        padding: 10px 20px;
        border-radius: 10px;
        border: 1px solid rgba(255, 255, 255, 0.18);
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.45);
        z-index: 2147483647;
        opacity: 0;
        pointer-events: none;
        transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.25s ease;
      }
      .cd-toast.show {
        opacity: 1;
        transform: translateX(-50%) translateY(0) scale(var(--cd-inv-zoom, 1));
      }
      .cd-toast.success { border-color: #22c55e; color: #86efac; }
      .cd-toast.warning { border-color: #eab308; color: #fde047; }
      .cd-toast.error { border-color: #ef4444; color: #fca5a5; }
    `;
  }

  // Mark instance initialized (ready to be toggled on-demand)
  window.__CHROME_DOODLE_INSTANCE__ = {
    setActive,
    toggle: () => setActive(!isActive)
  };
})();
