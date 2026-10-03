#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';

// --- ANSI Escape Sequences ---
const ESC = '\x1b';
const CSI = `${ESC}[`;
const HIDE_CURSOR = `${CSI}?25l`;
const SHOW_CURSOR = `${CSI}?25h`;
const ALT_SCREEN_ENTER = `${CSI}?1049h`;
const ALT_SCREEN_LEAVE = `${CSI}?1049l`;

// SGR Extended mouse mode (1006) + button tracking (1002) + any-event motion tracking (1003)
const MOUSE_ENABLE = `${CSI}?1000h${CSI}?1002h${CSI}?1003h${CSI}?1006h`;
const MOUSE_DISABLE = `${CSI}?1000l${CSI}?1002l${CSI}?1003l${CSI}?1006l`;

// Colors & Styles
const RESET = `${CSI}0m`;
const BOLD = `${CSI}1m`;
const DIM = `${CSI}2m`;
const UNDERLINE = `${CSI}4m`;
const INVERSE = `${CSI}7m`;

const FG_BLACK = `${CSI}30m`;
const FG_WHITE = `${CSI}97m`;
const FG_CYAN = `${CSI}96m`;
const FG_YELLOW = `${CSI}93m`;
const FG_GREEN = `${CSI}92m`;
const FG_RED = `${CSI}91m`;
const FG_BLUE = `${CSI}94m`;
const FG_MAGENTA = `${CSI}95m`;
const FG_GRAY = `${CSI}90m`;
const FG_DARK_GRAY = `${CSI}90m`;

const BG_BLUE = `${CSI}44m`;
const BG_DARK_GRAY = `${CSI}100m`;
const BG_CYAN = `${CSI}46m`;
const BG_YELLOW = `${CSI}43m`;
const BG_RED = `${CSI}41m`;
const BG_GREEN = `${CSI}42m`;
const BG_BLACK = `${CSI}40m`;

// Helper string width calculation for East Asian & Emoji characters
function charWidth(code) {
  if (!code) return 0;
  // Zero-width modifiers (Variation Selectors, ZWJ, Combining marks)
  if (code === 0xfe0f || code === 0xfe0e || code === 0x200d || (code >= 0x0300 && code <= 0x036f)) {
    return 0;
  }
  // Fullwidth / CJK / Emojis (Width = 2)
  if (
    (code >= 0x1100 && code <= 0x115f) ||
    (code >= 0x231a && code <= 0x231b) ||
    (code >= 0x23e9 && code <= 0x23f3) ||
    (code >= 0x23f8 && code <= 0x23fa) ||
    (code >= 0x25a0 && code <= 0x25ff) || // Geometric Shapes (▶, ■, ▲, ▼, etc.)
    (code >= 0x2600 && code <= 0x27bf) ||
    (code >= 0x2e80 && code <= 0xa4cf && code !== 0x303f) ||
    (code >= 0xac00 && code <= 0xd7a3) ||
    (code >= 0xf900 && code <= 0xfaff) ||
    (code >= 0xfe10 && code <= 0xfe19) ||
    (code >= 0xfe30 && code <= 0xfe6f) ||
    (code >= 0xff00 && code <= 0xff60) ||
    (code >= 0xffe0 && code <= 0xffe6) ||
    (code >= 0x1f000 && code <= 0x1faff) ||
    (code >= 0x20000 && code <= 0x3ffff)
  ) {
    return 2;
  }
  return 1;
}

function stringWidth(str) {
  let w = 0;
  for (const ch of str) {
    w += charWidth(ch.codePointAt(0));
  }
  return w;
}

function truncateString(str, maxWidth) {
  let w = 0;
  let out = '';
  for (const ch of str) {
    const cw = charWidth(ch.codePointAt(0));
    if (w + cw > maxWidth) break;
    w += cw;
    out += ch;
  }
  return out;
}

function padRight(str, targetWidth) {
  const curW = stringWidth(str);
  if (curW >= targetWidth) return truncateString(str, targetWidth);
  return str + ' '.repeat(targetWidth - curW);
}

function formatSize(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'K', 'M', 'G', 'T'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i] || 'P'}`;
}

function formatDate(date, full = true) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  if (full) {
    return `${y}-${m}-${d} ${h}:${min}`;
  }
  return `${m}-${d} ${h}:${min}`;
}

// --- Pane Class ---
class Pane {
  constructor(id, initialDir = process.cwd()) {
    this.id = id;
    this.currentDir = path.resolve(initialDir);
    this.items = [];
    this.cursorIndex = 0;
    this.scrollOffset = 0;
    this.sortBy = 'name';
    this.sortAsc = true;
    this.filter = '';
    this.refresh();
  }

  refresh() {
    try {
      const entries = fs.readdirSync(this.currentDir, { withFileTypes: true });
      const list = [];

      const parentDir = path.dirname(this.currentDir);
      if (parentDir !== this.currentDir) {
        list.push({
          name: '..',
          isDirectory: true,
          size: 0,
          mtime: new Date(),
          isSelected: false,
          fullPath: parentDir,
          isParent: true,
        });
      }

      for (const entry of entries) {
        if (this.filter && !entry.name.toLowerCase().includes(this.filter.toLowerCase())) {
          continue;
        }
        const fullPath = path.join(this.currentDir, entry.name);
        let size = 0;
        let mtime = new Date();
        let isDir = entry.isDirectory();
        try {
          const stats = fs.statSync(fullPath);
          size = stats.size;
          mtime = stats.mtime;
          isDir = stats.isDirectory();
        } catch {}
        list.push({
          name: entry.name,
          isDirectory: isDir,
          size,
          mtime,
          isSelected: false,
          fullPath,
          isParent: false,
        });
      }

      list.sort((a, b) => {
        if (a.isParent) return -1;
        if (b.isParent) return 1;
        if (a.isDirectory && !b.isDirectory) return -1;
        if (!a.isDirectory && b.isDirectory) return 1;

        let res = 0;
        if (this.sortBy === 'name') {
          res = a.name.localeCompare(b.name, undefined, { sensitivity: 'base' });
        } else if (this.sortBy === 'size') {
          res = a.size - b.size;
        } else if (this.sortBy === 'mtime') {
          res = a.mtime.getTime() - b.mtime.getTime();
        }
        return this.sortAsc ? res : -res;
      });

      this.items = list;
      if (this.cursorIndex >= this.items.length) {
        this.cursorIndex = Math.max(0, this.items.length - 1);
      }
    } catch (err) {
      this.items = [];
      this.cursorIndex = 0;
      this.scrollOffset = 0;
    }
  }

  getSelectedItems() {
    const selected = this.items.filter((item) => item.isSelected && !item.isParent);
    if (selected.length > 0) return selected;
    const current = this.items[this.cursorIndex];
    if (current && !current.isParent) return [current];
    return [];
  }

  selectAll(val = true) {
    for (const it of this.items) {
      if (!it.isParent) it.isSelected = val;
    }
  }

  navigate(targetPath) {
    try {
      const resolved = path.resolve(targetPath);
      if (fs.existsSync(resolved) && fs.statSync(resolved).isDirectory()) {
        this.currentDir = resolved;
        this.cursorIndex = 0;
        this.scrollOffset = 0;
        this.refresh();
        return true;
      }
    } catch {}
    return false;
  }
}

// --- Main App Class ---
class FileTuiApp {
  constructor() {
    this.leftPane = new Pane('left', process.cwd());
    this.rightPane = new Pane('right', process.cwd());
    this.activePane = this.leftPane;
    this.modal = null;
    this.contextMenu = null;
    this.statusMessage = 'マウス操作対応（クリック・ホバー・D&D・ホイール）';
    this.statusIsError = false;
    this.lastClickTime = 0;
    this.lastClickRow = -1;
    this.dragItem = null;
    this.running = true;
    this.keepAliveTimer = null;

    this.hoverX = -1;
    this.hoverY = -1;
    this.hoveredItemDesc = '';

    this.cols = process.stdout.columns || 80;
    this.rows = process.stdout.rows || 24;
    this.clickableRegions = []; // { x1, y1, x2, y2, desc, onClick(btn, isDbl, x, y) }
  }

  start() {
    process.stdout.write(ALT_SCREEN_ENTER + HIDE_CURSOR + MOUSE_ENABLE);

    if (process.stdin.isTTY && typeof process.stdin.setRawMode === 'function') {
      process.stdin.setRawMode(true);
    }
    process.stdin.resume();
    process.stdin.setEncoding('utf8');

    process.stdin.on('data', (chunk) => this.handleInput(chunk));
    process.stdout.on('resize', () => {
      this.cols = process.stdout.columns || 80;
      this.rows = process.stdout.rows || 24;
      this.render();
    });

    this.keepAliveTimer = setInterval(() => {}, 1000);

    const cleanup = () => {
      if (!this.running) return;
      this.running = false;
      if (this.keepAliveTimer) clearInterval(this.keepAliveTimer);
      process.stdout.write(MOUSE_DISABLE + SHOW_CURSOR + ALT_SCREEN_LEAVE);
    };

    process.on('SIGINT', () => {
      cleanup();
      process.exit(0);
    });
    process.on('SIGTERM', () => {
      cleanup();
      process.exit(0);
    });
    process.on('exit', () => {
      cleanup();
    });

    this.render();
  }

  setStatus(msg, isError = false) {
    this.statusMessage = msg;
    this.statusIsError = isError;
    this.render();
  }

  isHover(x1, y1, x2, y2) {
    return this.hoverX >= x1 && this.hoverX <= x2 && this.hoverY >= y1 && this.hoverY <= y2;
  }

  handleInput(chunk) {
    const sgrRegex = /\x1b\[<(\d+);(\d+);(\d+)([Mm])/g;
    let match;
    let handledMouse = false;

    while ((match = sgrRegex.exec(chunk)) !== null) {
      handledMouse = true;
      const buttonCode = parseInt(match[1], 10);
      const x = parseInt(match[2], 10);
      const y = parseInt(match[3], 10);
      const isRelease = match[4] === 'm';
      this.handleMouseEvent(buttonCode, x, y, isRelease);
    }

    if (handledMouse) return;

    if (this.contextMenu && (chunk === '\x1b' || chunk === '\x03')) {
      this.contextMenu = null;
      this.render();
      return;
    }

    if (this.modal) {
      this.handleModalKey(chunk);
      return;
    }

    if (chunk === '\x03' || chunk === 'q' || chunk === '\x1b[21~') {
      this.exit();
    } else if (chunk === '\t') {
      this.toggleActivePane();
    } else if (chunk === '\x1b[A' || chunk === 'k') {
      this.moveCursor(-1);
    } else if (chunk === '\x1b[B' || chunk === 'j') {
      this.moveCursor(1);
    } else if (chunk === '\r' || chunk === '\n') {
      this.activateCurrentItem();
    } else if (chunk === ' ') {
      this.toggleSelection();
    } else if (chunk === '\x1b[11~' || chunk === '?') {
      this.showHelpModal();
    } else if (chunk === '\x1b[12~' || chunk === 'r') {
      this.actionRename();
    } else if (chunk === '\x1b[13~' || chunk === 'v') {
      this.actionView();
    } else if (chunk === '\x1b[14~' || chunk === 'o') {
      this.openDirectoryPicker(this.activePane);
    } else if (chunk === '\x1b[15~' || chunk === 'c') {
      this.actionCopy();
    } else if (chunk === '\x1b[17~' || chunk === 'm') {
      this.actionMove();
    } else if (chunk === '\x1b[18~' || chunk === 'n') {
      this.actionMkdir();
    } else if (chunk === '\x1b[19~' || chunk === 'd' || chunk === '\x7f') {
      this.actionDelete();
    } else if (chunk === 's') {
      this.toggleSort();
    }
  }

  handleModalKey(chunk) {
    if (chunk === '\x1b' || chunk === '\x03') {
      this.modal = null;
      this.render();
      return;
    }

    if (this.modal.type === 'input') {
      if (chunk === '\r' || chunk === '\n') {
        const cb = this.modal.callback;
        const val = this.modal.value;
        this.modal = null;
        if (cb) cb(val);
      } else if (chunk === '\x7f' || chunk === '\b') {
        this.modal.value = this.modal.value.slice(0, -1);
        this.render();
      } else if (chunk.length === 1 && chunk >= ' ') {
        this.modal.value += chunk;
        this.render();
      }
    } else if (this.modal.type === 'confirm') {
      if (chunk === 'y' || chunk === 'Y' || chunk === '\r') {
        const cb = this.modal.callback;
        this.modal = null;
        if (cb) cb(true);
      } else if (chunk === 'n' || chunk === 'N' || chunk === '\x1b') {
        this.modal = null;
        this.render();
      }
    } else if (this.modal.type === 'view' || this.modal.type === 'help') {
      if (chunk === '\x1b[A' || chunk === 'k') {
        this.modal.scrollOffset = Math.max(0, (this.modal.scrollOffset || 0) - 1);
        this.render();
      } else if (chunk === '\x1b[B' || chunk === 'j') {
        this.modal.scrollOffset = (this.modal.scrollOffset || 0) + 1;
        this.render();
      } else if (chunk === '\r' || chunk === ' ') {
        this.modal = null;
        this.render();
      }
    }
  }

  handleMouseEvent(buttonCode, x, y, isRelease) {
    const prevHoverX = this.hoverX;
    const prevHoverY = this.hoverY;
    this.hoverX = x;
    this.hoverY = y;

    // Detect hovered item description for status bar
    let curHoverDesc = '';
    for (const region of this.clickableRegions) {
      if (x >= region.x1 && x <= region.x2 && y >= region.y1 && y <= region.y2) {
        curHoverDesc = region.desc || '';
        break;
      }
    }
    this.hoveredItemDesc = curHoverDesc;

    // Mouse movement / hover event (no button click)
    if (buttonCode === 35 || (buttonCode === 32 && !this.dragItem)) {
      if (prevHoverX !== x || prevHoverY !== y) {
        this.render();
      }
      return;
    }

    if (buttonCode === 64) {
      if (this.modal && (this.modal.type === 'view' || this.modal.type === 'help')) {
        this.modal.scrollOffset = Math.max(0, (this.modal.scrollOffset || 0) - 3);
        this.render();
        return;
      }
      const pane = x <= Math.floor(this.cols / 2) ? this.leftPane : this.rightPane;
      pane.scrollOffset = Math.max(0, pane.scrollOffset - 3);
      this.render();
      return;
    }
    if (buttonCode === 65) {
      if (this.modal && (this.modal.type === 'view' || this.modal.type === 'help')) {
        this.modal.scrollOffset = (this.modal.scrollOffset || 0) + 3;
        this.render();
        return;
      }
      const pane = x <= Math.floor(this.cols / 2) ? this.leftPane : this.rightPane;
      pane.scrollOffset = Math.min(Math.max(0, pane.items.length - 5), pane.scrollOffset + 3);
      this.render();
      return;
    }

    if (isRelease) {
      if (this.dragItem) {
        const targetPane = x <= Math.floor(this.cols / 2) ? this.leftPane : this.rightPane;
        if (targetPane !== this.dragItem.sourcePane) {
          this.promptTransfer(this.dragItem.items, this.dragItem.sourcePane, targetPane);
        }
        this.dragItem = null;
        this.render();
      }
      return;
    }

    if (this.contextMenu) {
      for (const btn of this.contextMenu.items) {
        if (y === btn.y && x >= btn.x1 && x <= btn.x2) {
          this.contextMenu = null;
          btn.action();
          return;
        }
      }
      this.contextMenu = null;
      this.render();
      return;
    }

    const now = Date.now();
    const isDoubleClick = buttonCode === 0 && now - this.lastClickTime < 350 && this.lastClickRow === y;
    this.lastClickTime = now;
    this.lastClickRow = y;

    if (this.modal) {
      for (const btn of this.modal.buttons || []) {
        if (y === btn.y && x >= btn.x1 && x <= btn.x2) {
          btn.action();
          return;
        }
      }
      if (this.modal.type === 'view' || this.modal.type === 'help') {
        this.modal = null;
        this.render();
      }
      return;
    }

    for (const region of this.clickableRegions) {
      if (x >= region.x1 && x <= region.x2 && y >= region.y1 && y <= region.y2) {
        region.onClick(buttonCode, isDoubleClick, x, y);
        return;
      }
    }
  }

  openContextMenu(x, y, pane, item) {
    this.activePane = pane;
    const menuItems = [
      {
        label: item.isDirectory ? '[開く] フォルダ遷移' : '[表示] プレビュー',
        action: () => this.activateCurrentItem(),
      },
      {
        label: '[リネーム] 名前の変更',
        action: () => this.actionRename(),
      },
      {
        label: '[コピー] 逆ペインへ転送',
        action: () => this.actionCopy(),
      },
      {
        label: '[移動] 逆ペインへ移動',
        action: () => this.actionMove(),
      },
      {
        label: '[新規] フォルダ作成',
        action: () => this.actionMkdir(),
      },
      {
        label: '[削除] アイテム削除',
        action: () => this.actionDelete(),
      },
      {
        label: item.isSelected ? '[選択解除] 選択を外す' : '[選択] 複数選択に追加',
        action: () => this.toggleSelection(),
      },
      {
        label: '[閉じる] メニュー終了',
        action: () => {
          this.contextMenu = null;
          this.render();
        },
      },
    ];

    const menuW = 28;
    const menuH = menuItems.length + 2;
    let menuX = Math.min(x, this.cols - menuW - 1);
    let menuY = Math.min(y, this.rows - menuH - 1);
    if (menuX < 1) menuX = 1;
    if (menuY < 1) menuY = 1;

    for (let i = 0; i < menuItems.length; i++) {
      menuItems[i].x1 = menuX + 1;
      menuItems[i].x2 = menuX + menuW - 2;
      menuItems[i].y = menuY + 1 + i;
    }

    this.contextMenu = {
      title: '  メニュー',
      x: menuX,
      y: menuY,
      width: menuW,
      height: menuH,
      items: menuItems,
    };
    this.render();
  }

  openSubdirDropdown(clickX, clickY, pane, dirPath, dirName) {
    this.activePane = pane;
    try {
      const entries = fs.readdirSync(dirPath, { withFileTypes: true });
      const subdirs = entries
        .filter((e) => e.isDirectory())
        .map((e) => ({
          name: e.name,
          fullPath: path.join(dirPath, e.name),
        }));

      if (subdirs.length === 0) {
        this.setStatus(`[${dirName}] にサブディレクトリはありません`);
        return;
      }

      subdirs.sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));

      const menuItems = subdirs.map((sub) => ({
        label: truncateString(`[DIR] ${sub.name}`, 26),
        action: () => {
          pane.navigate(sub.fullPath);
          this.contextMenu = null;
          this.render();
        },
      }));

      menuItems.push({
        label: '[閉じる]',
        action: () => {
          this.contextMenu = null;
          this.render();
        },
      });

      const maxLabelW = Math.max(...menuItems.map((m) => stringWidth(m.label)), stringWidth(` ${dirName} のサブフォルダ `));
      const menuW = Math.min(this.cols - 4, Math.max(28, maxLabelW + 4));
      const menuH = Math.min(this.rows - 4, menuItems.length + 2);
      let menuX = Math.min(clickX, this.cols - menuW - 1);
      let menuY = Math.min(clickY + 1, this.rows - menuH - 1);
      if (menuX < 1) menuX = 1;
      if (menuY < 1) menuY = 1;

      for (let i = 0; i < menuItems.length; i++) {
        menuItems[i].x1 = menuX + 1;
        menuItems[i].x2 = menuX + menuW - 2;
        menuItems[i].y = menuY + 1 + i;
      }

      this.contextMenu = {
        title: ` ${truncateString(dirName, 18)} のサブフォルダ `,
        x: menuX,
        y: menuY,
        width: menuW,
        height: menuH,
        items: menuItems,
      };
      this.render();
    } catch (err) {
      this.setStatus(`サブフォルダ読込エラー: ${err.message}`, true);
    }
  }

  openDirectoryPicker(pane) {
    this.activePane = pane;
    const homedir = process.env.USERPROFILE || process.env.HOME || 'C:\\';
    const parentOfCurrent = path.dirname(pane.currentDir);

    const drives = [];
    for (const d of ['C', 'D', 'E', 'F', 'G', 'Z']) {
      try {
        if (fs.existsSync(`${d}:\\`)) {
          drives.push(`${d}:\\`);
        }
      } catch {}
    }

    const quickDirs = [
      { label: `ホーム (~): ${truncateString(homedir, 30)}`, target: homedir },
      { label: `親階層: ${truncateString(parentOfCurrent, 35)}`, target: parentOfCurrent },
      { label: `初期パス: ${truncateString(process.cwd(), 35)}`, target: process.cwd() },
      ...drives.map((drv) => ({ label: `ドライブ ${drv}`, target: drv })),
    ];

    const modalW = Math.min(70, this.cols - 4);
    const modalH = Math.min(quickDirs.length + 6, this.rows - 4);
    const startX = Math.floor((this.cols - modalW) / 2);
    const startY = Math.floor((this.rows - modalH) / 2);

    const buttons = quickDirs.map((item, idx) => ({
      label: padRight(`[ ${item.label} ]`, modalW - 8),
      x1: startX + 4,
      x2: startX + modalW - 4,
      y: startY + 2 + idx,
      desc: item.label,
      action: () => {
        pane.navigate(item.target);
        this.modal = null;
        this.render();
      },
    }));

    buttons.push({
      label: '[ キャンセル (Esc) ]',
      x1: startX + Math.floor(modalW / 2) - 10,
      x2: startX + Math.floor(modalW / 2) + 10,
      y: startY + modalH - 2,
      desc: 'キャンセル',
      action: () => {
        this.modal = null;
        this.render();
      },
    });

    this.modal = {
      type: 'choice',
      title: 'ディレクトリ切替 / ドライブ選択',
      message: '移動したい場所をマウスで直接クリックしてください:',
      buttons,
    };
    this.render();
  }

  toggleActivePane() {
    this.activePane = this.activePane === this.leftPane ? this.rightPane : this.leftPane;
    this.render();
  }

  moveCursor(delta) {
    const pane = this.activePane;
    const newIdx = pane.cursorIndex + delta;
    if (newIdx >= 0 && newIdx < pane.items.length) {
      pane.cursorIndex = newIdx;
      this.adjustScroll(pane);
      this.render();
    }
  }

  adjustScroll(pane) {
    const visibleLines = this.rows - 6;
    if (pane.cursorIndex < pane.scrollOffset) {
      pane.scrollOffset = pane.cursorIndex;
    } else if (pane.cursorIndex >= pane.scrollOffset + visibleLines) {
      pane.scrollOffset = pane.cursorIndex - visibleLines + 1;
    }
  }

  toggleSelection() {
    const pane = this.activePane;
    const item = pane.items[pane.cursorIndex];
    if (item && !item.isParent) {
      item.isSelected = !item.isSelected;
      this.moveCursor(1);
    }
  }

  toggleSort(targetSort = null) {
    const p = this.activePane;
    if (targetSort) {
      if (p.sortBy === targetSort) {
        p.sortAsc = !p.sortAsc;
      } else {
        p.sortBy = targetSort;
        p.sortAsc = true;
      }
    } else {
      if (p.sortBy === 'name') p.sortBy = 'size';
      else if (p.sortBy === 'size') p.sortBy = 'mtime';
      else p.sortBy = 'name';
    }
    p.refresh();
    this.setStatus(`並び順: ${p.sortBy} (${p.sortAsc ? '昇順' : '降順'})`);
  }

  activateCurrentItem() {
    const pane = this.activePane;
    const item = pane.items[pane.cursorIndex];
    if (!item) return;

    if (item.isDirectory) {
      pane.navigate(item.fullPath);
      this.render();
    } else {
      this.actionView(item);
    }
  }

  actionView(targetItem = null) {
    const item = targetItem || this.activePane.items[this.activePane.cursorIndex];
    if (!item || item.isDirectory) return;

    try {
      const content = fs.readFileSync(item.fullPath, 'utf8');
      const lines = content.split(/\r?\n/);
      const modalW = Math.min(76, this.cols - 4);
      const modalH = Math.min(22, this.rows - 6);
      const startX = Math.floor((this.cols - modalW) / 2);
      const startY = Math.floor((this.rows - modalH) / 2);

      this.modal = {
        type: 'view',
        title: `プレビュー: ${item.name} (${formatSize(item.size)})`,
        lines,
        scrollOffset: 0,
        buttons: [
          {
            label: '[ ▲ 上スクロール ]',
            x1: startX + 4,
            x2: startX + 22,
            y: startY + modalH - 2,
            desc: '上スクロール',
            action: () => {
              this.modal.scrollOffset = Math.max(0, (this.modal.scrollOffset || 0) - 5);
              this.render();
            },
          },
          {
            label: '[ ▼ 下スクロール ]',
            x1: startX + 25,
            x2: startX + 43,
            y: startY + modalH - 2,
            desc: '下スクロール',
            action: () => {
              this.modal.scrollOffset = (this.modal.scrollOffset || 0) + 5;
              this.render();
            },
          },
          {
            label: '[ 閉じる (Esc) ]',
            x1: startX + modalW - 18,
            x2: startX + modalW - 3,
            desc: '閉じる',
            action: () => {
              this.modal = null;
              this.render();
            },
          },
        ],
      };
      this.render();
    } catch (err) {
      this.setStatus(`プレビュー失敗: ${err.message}`, true);
    }
  }

  actionRename() {
    const item = this.activePane.items[this.activePane.cursorIndex];
    if (!item || item.isParent) return;

    const modalW = Math.min(60, this.cols - 4);
    const startX = Math.floor((this.cols - modalW) / 2);
    const startY = Math.floor((this.rows - 10) / 2);

    this.modal = {
      type: 'input',
      title: `名前の変更: ${item.name}`,
      value: item.name,
      callback: (newName) => {
        if (newName && newName !== item.name) {
          try {
            const dest = path.join(this.activePane.currentDir, newName);
            fs.renameSync(item.fullPath, dest);
            this.leftPane.refresh();
            this.rightPane.refresh();
            this.setStatus(`変更完了: ${item.name} -> ${newName}`);
          } catch (err) {
            this.setStatus(`変更エラー: ${err.message}`, true);
          }
        }
        this.render();
      },
      buttons: [
        {
          label: '[ 決定 (OK) ]',
          x1: startX + 4,
          x2: startX + 18,
          y: startY + 6,
          desc: '決定',
          action: () => {
            const cb = this.modal.callback;
            const val = this.modal.value;
            this.modal = null;
            if (cb) cb(val);
          },
        },
        {
          label: '[ クリア ]',
          x1: startX + 21,
          x2: startX + 32,
          y: startY + 6,
          desc: 'クリア',
          action: () => {
            this.modal.value = '';
            this.render();
          },
        },
        {
          label: '[ キャンセル ]',
          x1: startX + 35,
          x2: startX + 50,
          y: startY + 6,
          desc: 'キャンセル',
          action: () => {
            this.modal = null;
            this.render();
          },
        },
      ],
    };
    this.render();
  }

  actionMkdir() {
    const modalW = Math.min(60, this.cols - 4);
    const startX = Math.floor((this.cols - modalW) / 2);
    const startY = Math.floor((this.rows - 10) / 2);

    this.modal = {
      type: 'input',
      title: '新規フォルダ作成',
      value: 'new_folder',
      callback: (folderName) => {
        if (folderName) {
          try {
            const dest = path.join(this.activePane.currentDir, folderName);
            fs.mkdirSync(dest, { recursive: true });
            this.leftPane.refresh();
            this.rightPane.refresh();
            this.setStatus(`フォルダ作成完了: ${folderName}`);
          } catch (err) {
            this.setStatus(`フォルダ作成エラー: ${err.message}`, true);
          }
        }
        this.render();
      },
      buttons: [
        {
          label: '[ 作成 (OK) ]',
          x1: startX + 4,
          x2: startX + 18,
          y: startY + 6,
          desc: '作成',
          action: () => {
            const cb = this.modal.callback;
            const val = this.modal.value;
            this.modal = null;
            if (cb) cb(val);
          },
        },
        {
          label: '[ クリア ]',
          x1: startX + 21,
          x2: startX + 32,
          y: startY + 6,
          desc: 'クリア',
          action: () => {
            this.modal.value = '';
            this.render();
          },
        },
        {
          label: '[ キャンセル ]',
          x1: startX + 35,
          x2: startX + 50,
          y: startY + 6,
          desc: 'キャンセル',
          action: () => {
            this.modal = null;
            this.render();
          },
        },
      ],
    };
    this.render();
  }

  actionDelete() {
    const selected = this.activePane.getSelectedItems();
    if (selected.length === 0) return;

    const names = selected.map((i) => i.name).join(', ');
    const modalW = Math.min(60, this.cols - 4);
    const startX = Math.floor((this.cols - modalW) / 2);
    const startY = Math.floor((this.rows - 10) / 2);

    this.modal = {
      type: 'confirm',
      title: '削除確認',
      message: `${selected.length} 件のアイテムを削除しますか？ (${truncateString(names, 35)})`,
      callback: (confirmed) => {
        if (confirmed) {
          let count = 0;
          for (const item of selected) {
            try {
              fs.rmSync(item.fullPath, { recursive: true, force: true });
              count++;
            } catch (err) {
              this.setStatus(`削除エラー (${item.name}): ${err.message}`, true);
            }
          }
          this.leftPane.refresh();
          this.rightPane.refresh();
          this.setStatus(`${count} 件削除完了`);
        }
        this.render();
      },
      buttons: [
        {
          label: '[ 削除実行 (はい) ]',
          x1: startX + 4,
          x2: startX + 24,
          y: startY + 5,
          desc: '削除実行',
          action: () => {
            const cb = this.modal.callback;
            this.modal = null;
            if (cb) cb(true);
          },
        },
        {
          label: '[ キャンセル (いいえ) ]',
          x1: startX + 28,
          x2: startX + 52,
          y: startY + 5,
          desc: 'キャンセル',
          action: () => {
            this.modal = null;
            this.render();
          },
        },
      ],
    };
    this.render();
  }

  actionCopy() {
    const targetPane = this.activePane === this.leftPane ? this.rightPane : this.leftPane;
    const selected = this.activePane.getSelectedItems();
    if (selected.length === 0) return;
    this.promptTransfer(selected, this.activePane, targetPane, 'copy');
  }

  actionMove() {
    const targetPane = this.activePane === this.leftPane ? this.rightPane : this.leftPane;
    const selected = this.activePane.getSelectedItems();
    if (selected.length === 0) return;
    this.promptTransfer(selected, this.activePane, targetPane, 'move');
  }

  promptTransfer(items, srcPane, dstPane, defaultMode = null) {
    if (srcPane.currentDir === dstPane.currentDir) {
      this.setStatus('コピー/移動元と先が同一ディレクトリです', true);
      return;
    }

    const doTransfer = (mode) => {
      let count = 0;
      for (const item of items) {
        try {
          const dest = path.join(dstPane.currentDir, item.name);
          if (mode === 'copy') {
            fs.cpSync(item.fullPath, dest, { recursive: true });
          } else {
            fs.renameSync(item.fullPath, dest);
          }
          count++;
        } catch (err) {
          this.setStatus(`転送エラー (${item.name}): ${err.message}`, true);
        }
      }
      this.leftPane.refresh();
      this.rightPane.refresh();
      this.setStatus(`${count} 件の${mode === 'copy' ? 'コピー' : '移動'}が完了しました`);
      this.render();
    };

    const modalW = Math.min(60, this.cols - 4);
    const startX = Math.floor((this.cols - modalW) / 2);
    const startY = Math.floor((this.rows - 10) / 2);

    if (defaultMode) {
      this.modal = {
        type: 'confirm',
        title: `${defaultMode === 'copy' ? 'コピー' : '移動'}確認`,
        message: `${items.length} 件を [${truncateString(dstPane.currentDir, 35)}] へ${defaultMode === 'copy' ? 'コピー' : '移動'}しますか？`,
        callback: (confirmed) => {
          if (confirmed) doTransfer(defaultMode);
          this.render();
        },
        buttons: [
          {
            label: '[ 実行 (はい) ]',
            x1: startX + 6,
            x2: startX + 24,
            y: startY + 5,
            desc: '実行',
            action: () => {
              this.modal = null;
              doTransfer(defaultMode);
            },
          },
          {
            label: '[ キャンセル ]',
            x1: startX + 28,
            x2: startX + 46,
            y: startY + 5,
            desc: 'キャンセル',
            action: () => {
              this.modal = null;
              this.render();
            },
          },
        ],
      };
      this.render();
      return;
    }

    this.modal = {
      type: 'choice',
      title: '転送操作を選択',
      message: `${items.length} 件 -> ${truncateString(dstPane.currentDir, 35)}`,
      buttons: [
        {
          label: '[ コピー (C) ]',
          x1: startX + 4,
          x2: startX + 18,
          y: startY + 5,
          desc: 'コピー',
          action: () => {
            this.modal = null;
            doTransfer('copy');
          },
        },
        {
          label: '[ 移動 (M) ]',
          x1: startX + 21,
          x2: startX + 35,
          y: startY + 5,
          desc: '移動',
          action: () => {
            this.modal = null;
            doTransfer('move');
          },
        },
        {
          label: '[ 中止 ]',
          x1: startX + 38,
          x2: startX + 52,
          desc: '中止',
          action: () => {
            this.modal = null;
            this.render();
          },
        },
      ],
    };
    this.render();
  }

  showHelpModal() {
    const helpLines = [
      '【マウスによるすべての操作】',
      '・パンくずボタン          : 各階層 [C:\\] [works] [2026] などを直接クリックでジャンプ',
      '・左クリック              : ファイル選択 / ボタン実行 / タブ切替',
      '・ダブルクリック          : フォルダ展開 / ファイルプレビュー',
      '・右クリック              : コンテキストメニュー表示 (リネーム/コピー/移動/削除)',
      '・ドラッグ＆ドロップ      : 左右ペイン間でのファイルコピー・移動',
      '・マウスホイール (上/下)  : リスト / プレビュー画面のスクロール',
      '・ヘッダ [切替] ボタン    : ドライブ / ホーム / 親階層一覧からジャンプ',
      '・ヘッダ [上へ] ボタン    : 親ディレクトリへ移動',
      '・ヘッダ [更新] ボタン    : ディレクトリ再読み込み',
      '・ヘッダ [名前/サイズ/日時]: クリックで並び替え (昇順/降順)',
      '・ヘッダ [全] ボタン      : アイテム一括選択 / 解除',
      '・ホバー表示              : マウス位置のボタン・項目が黄色反転で強調',
      '',
      '【キーボードショートカット (併用可)】',
      '・Tab: ペイン切替 | ↑↓ / jk: カーソル移動 | Enter: 実行 | Space: 選択',
      '・F1: ヘルプ | F2: リネーム | F3: 表示 | F4: 切替 | F5: コピー | F6: 移動 | F7: 新規 | F8: 削除 | F10: 終了',
    ];

    const modalW = Math.min(76, this.cols - 4);
    const modalH = Math.min(22, this.rows - 6);
    const startX = Math.floor((this.cols - modalW) / 2);
    const startY = Math.floor((this.rows - modalH) / 2);

    this.modal = {
      type: 'help',
      title: 'TUI マウスファイルマネージャ ヘルプ',
      lines: helpLines,
      scrollOffset: 0,
      buttons: [
        {
          label: '[ 閉じる (Esc) ]',
          x1: startX + Math.floor(modalW / 2) - 10,
          x2: startX + Math.floor(modalW / 2) + 10,
          y: startY + modalH - 2,
          desc: '閉じる',
          action: () => {
            this.modal = null;
            this.render();
          },
        },
      ],
    };
    this.render();
  }

  exit() {
    this.running = false;
    if (this.keepAliveTimer) clearInterval(this.keepAliveTimer);
    process.stdout.write(MOUSE_DISABLE + SHOW_CURSOR + ALT_SCREEN_LEAVE);
    process.exit(0);
  }

  // --- Rendering ---
  render() {
    this.clickableRegions = [];
    const buf = [];
    const cols = this.cols;
    const rows = this.rows;

    const paneWidth = Math.floor(cols / 2);
    const rightPaneWidth = cols - paneWidth;
    const contentHeight = rows - 5;

    // 1. App Top Header
    buf.push(`${CSI}1;1H${BG_BLUE}${FG_WHITE}${BOLD}`);
    const title = ' FileTui - マウス対応 ファイルマネージャ ';
    const helpBtn = ' [ヘルプ] ';
    const quitBtn = ' [終了] ';
    const topBar = padRight(title, cols - stringWidth(helpBtn) - stringWidth(quitBtn));
    buf.push(topBar);

    // Help Button
    const helpX1 = cols - stringWidth(helpBtn) - stringWidth(quitBtn) + 1;
    const helpX2 = cols - stringWidth(quitBtn);
    const isHelpHover = this.isHover(helpX1, 1, helpX2, 1);
    buf.push(`${isHelpHover ? BG_YELLOW + FG_BLACK + BOLD : BG_BLUE + FG_WHITE}${helpBtn}${RESET}`);
    this.clickableRegions.push({
      x1: helpX1,
      y1: 1,
      x2: helpX2,
      y2: 1,
      desc: 'ヘルプ',
      onClick: () => this.showHelpModal(),
    });

    // Quit Button
    const quitX1 = cols - stringWidth(quitBtn) + 1;
    const quitX2 = cols;
    const isQuitHover = this.isHover(quitX1, 1, quitX2, 1);
    buf.push(`${isQuitHover ? BG_RED + FG_WHITE + BOLD : BG_BLUE + FG_WHITE}${quitBtn}${RESET}`);
    this.clickableRegions.push({
      x1: quitX1,
      y1: 1,
      x2: quitX2,
      y2: 1,
      desc: '終了',
      onClick: () => this.exit(),
    });

    // 2. Render Both Panes
    this.renderPane(buf, this.leftPane, 1, paneWidth, contentHeight, this.activePane === this.leftPane);
    this.renderPane(buf, this.rightPane, paneWidth + 1, rightPaneWidth, contentHeight, this.activePane === this.rightPane);

    // 3. Bottom Status Bar (Row = rows - 1)
    buf.push(`${CSI}${rows - 1};1H`);
    const statusBg = this.statusIsError ? BG_RED : BG_DARK_GRAY;
    const coordInfo = this.hoverX > 0 ? `[X:${String(this.hoverX).padStart(2)} Y:${String(this.hoverY).padStart(2)}${this.hoveredItemDesc ? ` | ${this.hoveredItemDesc}` : ''}] ` : '';
    const statusTxt = padRight(` ${coordInfo}${this.statusMessage}`, cols);
    buf.push(`${statusBg}${FG_WHITE}${statusTxt}${RESET}`);

    // 4. Bottom Toolbar (Row = rows)
    buf.push(`${CSI}${rows};1H${BG_BLACK}`);
    const buttons = [
      { key: 'F1', label: 'ヘルプ', action: () => this.showHelpModal() },
      { key: 'F2', label: 'リネーム', action: () => this.actionRename() },
      { key: 'F3', label: '表示', action: () => this.actionView() },
      { key: 'F4', label: '切替', action: () => this.openDirectoryPicker(this.activePane) },
      { key: 'F5', label: 'コピー', action: () => this.actionCopy() },
      { key: 'F6', label: '移動', action: () => this.actionMove() },
      { key: 'F7', label: '新規', action: () => this.actionMkdir() },
      { key: 'F8', label: '削除', action: () => this.actionDelete() },
      { key: 'Tab', label: '切替', action: () => this.toggleActivePane() },
      { key: 'F10', label: '終了', action: () => this.exit() },
    ];

    let curX = 1;
    for (const b of buttons) {
      const btnText = `[${b.key}:${b.label}]`;
      const btnW = stringWidth(btnText);
      const isHovered = this.isHover(curX, rows, curX + btnW - 1, rows);

      if (isHovered) {
        buf.push(`${BG_YELLOW}${FG_BLACK}${BOLD}${btnText}${RESET} `);
      } else {
        buf.push(`${FG_CYAN}[${FG_YELLOW}${b.key}${FG_WHITE}:${b.label}${FG_CYAN}]${RESET} `);
      }

      this.clickableRegions.push({
        x1: curX,
        y1: rows,
        x2: curX + btnW - 1,
        y2: rows,
        desc: `${b.key}:${b.label}`,
        onClick: b.action,
      });
      curX += btnW + 1;
    }
    if (curX <= cols) {
      buf.push(' '.repeat(cols - curX + 1));
    }
    buf.push(RESET);

    // 5. Context Menu
    if (this.contextMenu) {
      this.renderContextMenu(buf);
    }

    // 6. Modal
    if (this.modal) {
      this.renderModal(buf);
    }

    process.stdout.write(buf.join(''));
  }

  renderPane(buf, pane, startCol, width, height, isActive) {
    const headerBg = isActive ? BG_CYAN + FG_BLACK : BG_DARK_GRAY + FG_WHITE;

    // --- Row 2: Header Buttons & Breadcrumbs ---
    buf.push(`${CSI}2;${startCol}H${headerBg}`);

    const pickBtnText = ' [切替] ';
    const upBtnText = ' [上へ] ';
    const refBtnText = ' [更新] ';
    const rightControlsW = stringWidth(pickBtnText) + stringWidth(upBtnText) + stringWidth(refBtnText);
    const breadcrumbAreaW = width - rightControlsW;

    // Parse Breadcrumbs
    const normPath = path.normalize(pane.currentDir);
    const parts = normPath.split(path.sep).filter(Boolean);
    const breadcrumbs = [];
    let accPath = '';
    if (normPath.startsWith('\\\\')) {
      accPath = '\\\\';
    }

    for (let i = 0; i < parts.length; i++) {
      const p = parts[i];
      if (i === 0 && p.endsWith(':')) {
        accPath = p + path.sep;
      } else {
        accPath = path.join(accPath || path.sep, p);
      }
      breadcrumbs.push({ label: p, targetPath: accPath });
    }

    // Prepare breadcrumb buttons and ▶ separators
    let curX = startCol;
    const sepTag = '▶';
    const sepW = stringWidth(sepTag); // 2

    const bcRenderItems = [];
    for (let i = 0; i < breadcrumbs.length; i++) {
      const b = breadcrumbs[i];
      const tag = `[${b.label}]`;
      const tagW = stringWidth(tag);
      bcRenderItems.push({ ...b, tag, tagW });
    }

    // Fit visible breadcrumbs from right to left
    let totalBcW = 0;
    const visibleBc = [];
    for (let i = bcRenderItems.length - 1; i >= 0; i--) {
      const it = bcRenderItems[i];
      const itemTotalW = it.tagW + sepW;
      if (totalBcW + itemTotalW <= breadcrumbAreaW - 1) {
        visibleBc.unshift(it);
        totalBcW += itemTotalW;
      } else {
        if (visibleBc.length === 0) {
          visibleBc.push(it);
          totalBcW += itemTotalW;
        }
        break;
      }
    }

    buf.push(' ');
    curX += 1;

    for (let i = 0; i < visibleBc.length; i++) {
      const b = visibleBc[i];

      // 1. Folder Button [label]
      const isTagHovered = this.isHover(curX, 2, curX + b.tagW - 1, 2);
      if (isTagHovered) {
        buf.push(`${BG_YELLOW}${FG_BLACK}${BOLD}${b.tag}${RESET}${headerBg}`);
      } else {
        buf.push(`${BOLD}${FG_WHITE}${b.tag}${RESET}${headerBg}`);
      }

      this.clickableRegions.push({
        x1: curX,
        y1: 2,
        x2: curX + b.tagW - 1,
        y2: 2,
        desc: `階層:${b.label}`,
        onClick: () => {
          this.activePane = pane;
          pane.navigate(b.targetPath);
          this.render();
        },
      });
      curX += b.tagW;

      // 2. Clickable Separator ▶
      const isSepHovered = this.isHover(curX, 2, curX + sepW - 1, 2);
      if (isSepHovered) {
        buf.push(`${BG_YELLOW}${FG_BLACK}${BOLD}${sepTag}${RESET}${headerBg}`);
      } else {
        buf.push(`${DIM}${FG_GRAY}${sepTag}${RESET}${headerBg}`);
      }

      this.clickableRegions.push({
        x1: curX,
        y1: 2,
        x2: curX + sepW - 1,
        y2: 2,
        desc: `${b.label}のサブフォルダ一覧`,
        onClick: (btn, isDbl, clickX, clickY) => {
          this.openSubdirDropdown(clickX, clickY, pane, b.targetPath, b.label);
        },
      });
      curX += sepW;
    }

    // Fill remaining space before right controls
    const fillSpaces = Math.max(0, startCol + breadcrumbAreaW - curX);
    if (fillSpaces > 0) {
      buf.push(' '.repeat(fillSpaces));
      curX += fillSpaces;
    }

    // Header Action Buttons
    // [切替]
    const pickW = stringWidth(pickBtnText);
    const isPickHover = this.isHover(curX, 2, curX + pickW - 1, 2);
    buf.push(`${isPickHover ? BG_YELLOW + FG_BLACK + BOLD : headerBg}${pickBtnText}${RESET}${headerBg}`);
    this.clickableRegions.push({
      x1: curX,
      y1: 2,
      x2: curX + pickW - 1,
      y2: 2,
      desc: '切替',
      onClick: () => this.openDirectoryPicker(pane),
    });
    curX += pickW;

    // [上へ]
    const upW = stringWidth(upBtnText);
    const isUpHover = this.isHover(curX, 2, curX + upW - 1, 2);
    buf.push(`${isUpHover ? BG_YELLOW + FG_BLACK + BOLD : headerBg}${upBtnText}${RESET}${headerBg}`);
    this.clickableRegions.push({
      x1: curX,
      y1: 2,
      x2: curX + upW - 1,
      y2: 2,
      desc: '上へ',
      onClick: () => {
        this.activePane = pane;
        pane.navigate(path.dirname(pane.currentDir));
        this.render();
      },
    });
    curX += upW;

    // [更新]
    const refW = stringWidth(refBtnText);
    const isRefHover = this.isHover(curX, 2, curX + refW - 1, 2);
    buf.push(`${isRefHover ? BG_YELLOW + FG_BLACK + BOLD : headerBg}${refBtnText}${RESET}`);
    this.clickableRegions.push({
      x1: curX,
      y1: 2,
      x2: curX + refW - 1,
      y2: 2,
      desc: '更新',
      onClick: () => {
        this.activePane = pane;
        pane.refresh();
        this.setStatus('ディレクトリを再読み込みしました');
      },
    });

    // --- Row 3: Column Header & Sort Buttons ---
    const isWide = width >= 54;
    const dateW = isWide ? 16 : 11;
    const sizeW = 8;
    const scrollW = 1;
    const fixedRightW = sizeW + 1 + dateW + 1 + scrollW;
    const nameW = Math.max(10, width - fixedRightW - 1);

    buf.push(`${CSI}3;${startCol}H${DIM}${BG_BLACK}${FG_GRAY}`);
    let hdrX = startCol;

    // [全]
    const selAllBtn = '[全]';
    const selAllW = stringWidth(selAllBtn);
    const isSelHover = this.isHover(hdrX, 3, hdrX + selAllW - 1, 3);
    buf.push(`${isSelHover ? BG_YELLOW + FG_BLACK + BOLD : BG_BLACK + FG_YELLOW}${selAllBtn}${RESET}${DIM}${BG_BLACK}${FG_GRAY} `);
    this.clickableRegions.push({
      x1: hdrX,
      y1: 3,
      x2: hdrX + selAllW - 1,
      y2: 3,
      desc: '全選択/解除',
      onClick: () => {
        this.activePane = pane;
        const allSel = pane.items.every((it) => it.isParent || it.isSelected);
        pane.selectAll(!allSel);
        this.render();
      },
    });
    hdrX += selAllW + 1;

    // Sort Name
    const sortName = `名前${pane.sortBy === 'name' ? (pane.sortAsc ? '↑' : '↓') : ''}`;
    const sortNameFieldW = nameW - selAllW - 1;
    const isNameHover = this.isHover(hdrX, 3, hdrX + sortNameFieldW - 1, 3);
    buf.push(`${isNameHover ? BG_YELLOW + FG_BLACK + BOLD : DIM}${padRight(sortName, sortNameFieldW)}${RESET}${DIM}${BG_BLACK}${FG_GRAY} `);
    this.clickableRegions.push({
      x1: hdrX,
      y1: 3,
      x2: hdrX + sortNameFieldW - 1,
      y2: 3,
      desc: '名前ソート',
      onClick: () => {
        this.activePane = pane;
        this.toggleSort('name');
      },
    });
    hdrX += sortNameFieldW + 1;

    // Sort Size
    const sortSize = `サイズ${pane.sortBy === 'size' ? (pane.sortAsc ? '↑' : '↓') : ''}`;
    const isSizeHover = this.isHover(hdrX, 3, hdrX + sizeW - 1, 3);
    buf.push(`${isSizeHover ? BG_YELLOW + FG_BLACK + BOLD : DIM}${padRight(sortSize, sizeW)}${RESET}${DIM}${BG_BLACK}${FG_GRAY} `);
    this.clickableRegions.push({
      x1: hdrX,
      y1: 3,
      x2: hdrX + sizeW - 1,
      y2: 3,
      desc: 'サイズソート',
      onClick: () => {
        this.activePane = pane;
        this.toggleSort('size');
      },
    });
    hdrX += sizeW + 1;

    // Sort Date
    const sortDate = `更新日時${pane.sortBy === 'mtime' ? (pane.sortAsc ? '↑' : '↓') : ''}`;
    const isDateHover = this.isHover(hdrX, 3, startCol + width - 1, 3);
    buf.push(`${isDateHover ? BG_YELLOW + FG_BLACK + BOLD : DIM}${padRight(sortDate, dateW)}${RESET} `);
    this.clickableRegions.push({
      x1: hdrX,
      y1: 3,
      x2: startCol + width - 1,
      y2: 3,
      desc: '更新日時ソート',
      onClick: () => {
        this.activePane = pane;
        this.toggleSort('mtime');
      },
    });

    // --- Row 4+: Item Rows ---
    const visibleLines = height - 1;
    for (let i = 0; i < visibleLines; i++) {
      const row = 4 + i;
      const itemIdx = pane.scrollOffset + i;
      buf.push(`${CSI}${row};${startCol}H`);

      if (itemIdx < pane.items.length) {
        const item = pane.items[itemIdx];
        const isCursor = isActive && itemIdx === pane.cursorIndex;
        const isRowHover = this.isHover(startCol, row, startCol + width - 1, row);

        let lineBg = isCursor ? BG_BLUE + FG_WHITE : isRowHover ? BG_DARK_GRAY + FG_WHITE : BG_BLACK;
        if (item.isSelected) {
          lineBg = isCursor ? BG_CYAN + FG_BLACK : BG_DARK_GRAY + FG_YELLOW;
        }

        let typeTag = item.isDirectory ? '[DIR] ' : '[FILE]';
        let nameColor = item.isDirectory ? FG_CYAN + BOLD : FG_WHITE;
        if (item.isParent) {
          typeTag = '[..]  ';
          nameColor = FG_YELLOW + BOLD;
        }

        const prefix = item.isSelected ? '[✓]' : isCursor ? '► ' : isRowHover ? '👉' : '  ';
        const nameStr = padRight(`${prefix} ${typeTag} ${item.name}`, nameW);
        const sizeStr = item.isDirectory ? '<DIR>   ' : padRight(formatSize(item.size), sizeW);
        const dateStr = padRight(formatDate(item.mtime, isWide), dateW);

        let scrollChar = '│';
        if (pane.items.length > visibleLines) {
          const thumbPos = Math.floor((pane.scrollOffset / (pane.items.length - visibleLines)) * (visibleLines - 1));
          if (i === thumbPos) scrollChar = '█';
        }

        const line = `${lineBg}${nameColor}${nameStr}${RESET}${lineBg}${FG_GRAY} ${sizeStr} ${dateStr} ${FG_DARK_GRAY}${scrollChar}${RESET}`;
        buf.push(line);

        this.clickableRegions.push({
          x1: startCol,
          y1: row,
          x2: startCol + width - 1,
          y2: row,
          desc: item.name,
          onClick: (btn, isDoubleClick, clickX, clickY) => {
            this.activePane = pane;
            pane.cursorIndex = itemIdx;

            if (btn === 2) {
              this.openContextMenu(clickX, clickY, pane, item);
              return;
            }

            if (isDoubleClick) {
              this.activateCurrentItem();
            } else {
              this.dragItem = { items: pane.getSelectedItems(), sourcePane: pane };
              this.render();
            }
          },
        });
      } else {
        buf.push(BG_BLACK + ' '.repeat(width) + RESET);
      }
    }
  }

  renderContextMenu(buf) {
    const cm = this.contextMenu;
    for (let r = 0; r < cm.height; r++) {
      const curY = cm.y + r;
      buf.push(`${CSI}${curY};${cm.x}H${BG_DARK_GRAY}${FG_WHITE}`);
      if (r === 0) {
        buf.push(`${BOLD}${BG_BLUE}${FG_WHITE}${padRight('  メニュー', cm.width)}${RESET}`);
      } else if (r === cm.height - 1) {
        buf.push(' '.repeat(cm.width) + RESET);
      } else {
        const item = cm.items[r - 1];
        const isHovered = this.isHover(item.x1, item.y, item.x2, item.y);
        if (isHovered) {
          buf.push(`${BG_YELLOW}${FG_BLACK}${BOLD}${padRight(` ${item.label}`, cm.width)}${RESET}`);
        } else {
          buf.push(`${padRight(` ${item.label}`, cm.width)}${RESET}`);
        }
      }
    }
  }

  renderModal(buf) {
    const modalW = Math.min(64, this.cols - 4);
    const modalH = Math.min(22, this.rows - 6);
    const startX = Math.floor((this.cols - modalW) / 2);
    const startY = Math.floor((this.rows - modalH) / 2);

    for (let r = 0; r < modalH; r++) {
      const curY = startY + r;
      buf.push(`${CSI}${curY};${startX}H${BG_DARK_GRAY}${FG_WHITE}`);
      if (r === 0) {
        buf.push(`${BOLD}${BG_BLUE}${FG_WHITE}${padRight(`  ${this.modal.title}`, modalW)}${RESET}`);
      } else if (r === modalH - 1) {
        buf.push(' '.repeat(modalW) + RESET);
      } else {
        buf.push(' '.repeat(modalW) + RESET);
      }
    }

    if (this.modal.type === 'input') {
      buf.push(`${CSI}${startY + 2};${startX + 2}H${BG_DARK_GRAY}${FG_YELLOW}入力してください (マウスまたはキーボード):${RESET}`);
      buf.push(`${CSI}${startY + 4};${startX + 2}H${BG_BLACK}${FG_WHITE}${padRight(` ${this.modal.value}_`, modalW - 4)}${RESET}`);

      for (const btn of this.modal.buttons || []) {
        const isHovered = this.isHover(btn.x1, btn.y, btn.x2, btn.y);
        buf.push(`${CSI}${btn.y};${btn.x1}H${isHovered ? BG_YELLOW + FG_BLACK + BOLD : BG_BLUE + FG_WHITE}${btn.label}${RESET}`);
      }
    } else if (this.modal.type === 'confirm' || this.modal.type === 'choice') {
      buf.push(`${CSI}${startY + 2};${startX + 2}H${BG_DARK_GRAY}${FG_WHITE}${this.modal.message}${RESET}`);
      for (const btn of this.modal.buttons || []) {
        const isHovered = this.isHover(btn.x1, btn.y, btn.x2, btn.y);
        buf.push(`${CSI}${btn.y};${btn.x1}H${isHovered ? BG_YELLOW + FG_BLACK + BOLD : BG_BLUE + FG_WHITE}${btn.label}${RESET}`);
      }
    } else if (this.modal.type === 'view' || this.modal.type === 'help') {
      const visibleLines = modalH - 4;
      const offset = this.modal.scrollOffset || 0;
      for (let i = 0; i < visibleLines; i++) {
        const lineIdx = offset + i;
        const curY = startY + 2 + i;
        buf.push(`${CSI}${curY};${startX + 2}H${BG_DARK_GRAY}${FG_WHITE}`);
        if (lineIdx < this.modal.lines.length) {
          buf.push(padRight(this.modal.lines[lineIdx], modalW - 4));
        } else {
          buf.push(' '.repeat(modalW - 4));
        }
        buf.push(RESET);
      }

      for (const btn of this.modal.buttons || []) {
        const isHovered = this.isHover(btn.x1, btn.y, btn.x2, btn.y);
        buf.push(`${CSI}${btn.y};${btn.x1}H${isHovered ? BG_YELLOW + FG_BLACK + BOLD : BG_BLUE + FG_WHITE}${btn.label}${RESET}`);
      }
    }
  }
}

// Start application
const app = new FileTuiApp();
app.start();
