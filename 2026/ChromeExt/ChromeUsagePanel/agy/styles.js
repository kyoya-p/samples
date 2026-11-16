// 🤖[AI 生成・編集を含む (Antigravity)]
// Shared CSS for both Shadow DOM drawer and standalone panel.html
globalThis.__USAGE_PANEL_CSS = `
:host {
  all: initial;
}

.usage-scope {
  --bg: #fafaf7;
  --card: #ffffff;
  --fg: #1d1d1b;
  --mut: #6b6b65;
  --bar: #3c6fb4;
  --bar2: #9bb8df;
  --line: #e3e2dc;
  --s1: #2a78d6;
  --s2: #1baf7a;
  --s3: #eb6834;
  --s4: #eda100;
  --s5: #e87ba4;
  --s6: #008300;
  --s7: #6250d6;
  --s8: #17a2b8;
  --s9: #8c564b;
  --s10: #6c757d;
  --s0: #8a8a84;
  --wk: #d08c4a;
  --err: #c9302c;
  --ok: #1baf7a;
  color-scheme: light;
  font: 14px/1.5 system-ui, "Segoe UI", sans-serif;
  color: var(--fg);
}

.usage-scope * {
  box-sizing: border-box;
}

.usage-scope button,
.usage-scope select {
  touch-action: manipulation;
  font-family: inherit;
}

.usage-scope button:focus-visible,
.usage-scope select:focus-visible,
.usage-scope input:focus-visible {
  outline: 2px solid var(--bar);
  outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
  .usage-scope *,
  .drawer {
    transition: none !important;
    animation: none !important;
  }
}

/* Drawer wrapper */
.drawer {
  position: fixed;
  top: 0;
  right: 0;
  height: auto;
  max-height: 100vh;
  width: 690px;
  max-width: 96vw;
  min-width: 380px;
  background: var(--bg);
  border-left: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  border-bottom-left-radius: 10px;
  box-shadow: none;
  display: flex;
  flex-direction: column;
  transform: translateX(100%);
  transition: transform 0.2s cubic-bezier(0.2, 0, 0, 1);
  pointer-events: none;
  z-index: 2147483647;
}

.drawer.open {
  transform: translateX(0);
  box-shadow: -8px 4px 28px rgba(0, 0, 0, 0.18);
  pointer-events: auto;
}

.drawer.dragging {
  transition: none !important;
}

.drawer-resizer {
  position: absolute;
  top: 0;
  left: -3px;
  width: 6px;
  height: 100%;
  cursor: ew-resize;
  z-index: 10;
}

.drawer-resizer:hover,
.drawer-resizer.active {
  background: rgba(60, 111, 180, 0.2);
}

.drawer-tab {
  position: absolute;
  left: -24px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 74px;
  padding: 6px 0;
  margin: 0;
  border: 1px solid var(--line);
  border-right: none;
  border-radius: 8px 0 0 8px;
  background: var(--card);
  color: var(--mut);
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.14);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  cursor: pointer;
  user-select: none;
  touch-action: none;
  pointer-events: auto;
  z-index: 12;
  opacity: 0.92;
  transition: background-color 0.15s ease, color 0.15s ease, opacity 0.15s ease;
}

.drawer-tab:hover,
.drawer-tab.active {
  background: var(--bg);
  color: var(--fg);
  opacity: 1;
}

.drawer-tab-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.drawer-tab-grip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 16px;
}

.drawer-tab-grip i {
  display: block;
  width: 1.5px;
  height: 14px;
  border-radius: 1px;
  background: currentColor;
  opacity: 0.65;
}

.drawer-tab-tag {
  font: 700 10px/1 system-ui, sans-serif;
  letter-spacing: 0;
}

.drawer-scroll {
  flex: 0 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 14px 12px;
  border-bottom-left-radius: 10px;
}

/* Standalone page wrapper */
.page-body {
  margin: 0;
  padding: 14px 18px 24px;
  background: var(--bg);
  min-height: 100vh;
}

.usage-scope h1 {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  line-height: 1.2;
}

.usage-scope h2 {
  font-size: 14px;
  margin: 0;
}

.mut {
  color: var(--mut);
  font-size: 12px;
}

.topbar {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 12px;
}

.topbar-main {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 12px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.provider-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.provider-dot.err {
  background: var(--err) !important;
}

.provider-dot.busy {
  animation: usage-dot-pulse 1.1s ease-in-out infinite;
}

@keyframes usage-dot-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.55; }
}

.controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 8px;
  width: 100%;
}

.btn {
  border: 1px solid var(--line);
  background: var(--card);
  color: var(--fg);
  font: 12px/1.4 system-ui, sans-serif;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 6px;
  cursor: pointer;
}

.btn:hover:not(:disabled) {
  background: var(--bg);
}

.btn:disabled {
  opacity: 0.55;
  cursor: default;
}

.btn-primary {
  background: var(--bar);
  border-color: var(--bar);
  color: #ffffff;
}

.btn-primary:hover:not(:disabled) {
  background: #325e99;
}

.btn-sm {
  padding: 3px 8px;
  font-size: 11px;
}

.btn-icon {
  width: 26px;
  height: 26px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 14px;
  line-height: 1;
  border-color: transparent;
  background: transparent;
  color: var(--mut);
}

.btn-icon:hover:not(:disabled) {
  background: var(--bg);
  color: var(--fg);
}

.btn-sq {
  width: 26px;
  height: 26px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-color: transparent;
  background: transparent;
  color: var(--mut);
}

.btn-sq:hover:not(:disabled) {
  background: var(--bg);
  color: var(--fg);
}

.btn-sq.btn-primary {
  background: transparent;
  border-color: transparent;
  color: var(--bar);
}

.btn-sq.btn-primary:hover:not(:disabled) {
  background: rgba(60, 111, 180, 0.12);
  color: #325e99;
}

.btn-primary.busy svg {
  animation: usage-dot-pulse 0.9s ease-in-out infinite;
}

.ctrl-right {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.ctrl-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.ctrl-group select {
  font: 12px system-ui, sans-serif;
  padding: 3px 7px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--card);
  color: var(--fg);
}

.ctrl-chk {
  cursor: pointer;
  user-select: none;
  font-size: 12px;
  color: var(--mut);
}

.ctrl-chk input {
  margin: 0;
  accent-color: var(--bar);
  cursor: pointer;
}

.ctrl-chk:has(input:checked) {
  color: var(--fg);
}

.err-text {
  display: block;
  margin-top: 6px;
  color: var(--err);
  font-size: 12px;
}

.err-text[hidden] {
  display: none;
}

.setup-banner {
  margin-top: 8px;
  padding: 8px 10px;
  background: #fff9eb;
  border: 1px solid #eed69c;
  border-radius: 8px;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.setup-banner[hidden] {
  display: none;
}

.setup-cmd-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.setup-cmd-row code {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 4px;
  padding: 2px 6px;
  font-family: ui-monospace, Consolas, monospace;
  font-size: 11px;
  word-break: break-all;
}

.grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(340px, 640px));
  justify-content: start;
}

.drawer .grid {
  grid-template-columns: 1fr;
}

.card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 14px;
}

.sw {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  margin-right: 4px;
  vertical-align: middle;
}

.mlg-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
}

.mlg-head .it {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.mlg {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 12px;
  font-size: 12px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--line);
}

.mlg .v {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  margin-left: 4px;
}

.cl-toggle {
  display: inline-flex;
  align-items: center;
  background: none;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 2px 8px;
  font: 12px system-ui, sans-serif;
  color: var(--mut);
  cursor: pointer;
  user-select: none;
  transition: all 0.15s ease;
}

.cl-toggle:hover {
  background: var(--card);
  border-color: var(--line);
  color: var(--fg);
}

.cl-toggle.expanded {
  background: var(--card);
  border-color: var(--line);
  color: var(--fg);
}

.cl-toggle.collapsed {
  opacity: 0.65;
}

.rem-badge {
  font-size: 12px;
  font-weight: 400;
  color: var(--mut);
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: 4px;
  padding: 1px 6px;
  margin-left: 8px;
  vertical-align: middle;
}

.ch {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.rt {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.now {
  font-size: 22px;
  font-weight: 600;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.range {
  display: flex;
}

svg text {
  paint-order: stroke;
  stroke: var(--card);
  stroke-width: 3px;
  stroke-linejoin: round;
}

.range select {
  font: 12px system-ui, sans-serif;
  padding: 2px 8px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--card);
  color: var(--fg);
}

.empty-msg {
  padding: 30px 12px;
  text-align: center;
  color: var(--mut);
  font-size: 13px;
}
`;
