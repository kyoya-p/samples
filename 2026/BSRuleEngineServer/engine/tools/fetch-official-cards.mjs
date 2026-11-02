// 公式カードリスト (battlespirits.com の detail_iframe.php) から、デッキで使う全カードの情報を取得し、
// エンジン用のカードデータ (engine/src/OfficialCards.kt) を生成する。値は推測せず、ページの記載だけを使う。
//
// usage (BSToolsCla フォルダで): node engine/tools/fetch-official-cards.mjs
//   decks/*.yaml のカード番号を集め、1枚ずつ取得 (1秒間隔) → engine/src/OfficialCards.kt を上書き
//
// 取得できない項目: シンボル (ページに文字情報がなくカード画像にのみ記載)。生成コードでは
// 「カードの色のシンボル1個」とし、未確認であることをコメントに残す。
import fs from 'fs';
import path from 'path';

const DECK_DIR = 'decks';
const OUT = 'engine/src/OfficialCards.kt';
const BASE = 'https://www.battlespirits.com/cardlist/detail_iframe.php?card_no=';

const COLORS = { 赤: 'RED', 紫: 'PURPLE', 緑: 'GREEN', 白: 'WHITE', 黄: 'YELLOW', 青: 'BLUE' };
const CATEGORY = { スピリット: 'SPIRIT', トークンスピリット: 'SPIRIT', ネクサス: 'NEXUS', 契約ネクサス: 'NEXUS', マジック: 'MAGIC', ブレイヴ: 'BRAVE', アルティメット: 'ULTIMATE' };

const cardNos = new Set();
for (const f of fs.readdirSync(DECK_DIR).filter(f => f.endsWith('.yaml'))) {
  for (const m of fs.readFileSync(path.join(DECK_DIR, f), 'utf8').matchAll(/^\s+([0-9A-Z]+-[0-9A-Z]+)\s*:/gm)) cardNos.add(m[1]);
}

const strip = s => s.replace(/<[^>]*>/g, '').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&').trim();
function field(html, term) {
  const i = html.indexOf(`detailColListTerm">${term}</dt>`);
  if (i < 0) return null;
  const j = html.indexOf('</dd>', i);
  return html.slice(html.indexOf('</dt>', i) + 5, j); // 見出し (dt) を除いた値 (dd) の部分
}
function parse(no, html) {
  const nameBlock = html.match(/class="cardName"[^>]*>([\s\S]*?)<\//) || html.match(/<h2[^>]*>([\s\S]*?)<\/h2>/);
  const texts = strip(html).split('\n').map(s => s.trim()).filter(Boolean);
  // 先頭は「カード番号 / レアリティ / カード名」の順
  const idx = texts.findIndex(t => t === no);
  const name = nameBlock ? strip(nameBlock[1]) : texts[idx + 2];
  const cat = strip(field(html, 'カテゴリー') || '').replace(/\s+/g, '');
  const colorsRaw = [...(field(html, '属性') || '').matchAll(/attributeItem[^>]*>([^<]+)</g)].map(m => m[1].trim());
  const cost = parseInt(strip(field(html, 'コスト') || ''), 10);
  const reduction = [...(field(html, '軽減コスト') || '').matchAll(/alt="([^"]+)"/g)].map(m => m[1]);
  const systemsRaw = strip(field(html, '系統') || '').replace(/\s+/g, '');
  const lv = [...(field(html, 'BP/コア') || '').matchAll(/bpCoreVal">([^<]*)<\/span>\s*<span class="bpCoreLevel">([\s\S]*?)<\/span>/g)]
    .map(m => ({ bp: parseInt(m[1], 10), core: parseInt(strip(m[2]), 10), soul: m[2].includes('ico_soulcore') }));
  const effect = [...(field(html, '能力・効果') || '').matchAll(/<p class="text">([\s\S]*?)<\/p>/g)].map(m => strip(m[1])).join('\n');
  const block = strip(field(html, 'ブロックアイコン') || '');
  return {
    no, name, category: CATEGORY[cat] ?? null, categoryRaw: cat,
    colors: colorsRaw.flatMap(c => [...c]).map(c => COLORS[c]).filter(Boolean),
    cost, reduction: reduction.map(c => COLORS[c]).filter(Boolean),
    systems: systemsRaw ? systemsRaw.split(/[・\/]/).map(s => s.trim()).filter(Boolean) : [],
    lvCosts: lv.map(x => x.core), lvBps: lv.every(x => Number.isFinite(x.bp)) ? lv.map(x => x.bp) : [],
    shinkai: lv.some(x => x.soul), effect, block,
  };
}

const sleep = ms => new Promise(r => setTimeout(r, ms));
const cards = [];
for (const no of [...cardNos].sort()) {
  const res = await fetch(BASE + encodeURIComponent(no), { headers: { 'User-Agent': 'Mozilla/5.0 (BSToolsCla card fetch)' } });
  const html = await res.text();
  const c = parse(no, html);
  const problems = [];
  if (!c.name) problems.push('名前');
  if (!c.category) problems.push(`カテゴリー(${c.categoryRaw})`);
  if (!Number.isFinite(c.cost)) problems.push('コスト');
  if (problems.length) console.log(`  ⚠ ${no}: 取得できず ${problems.join(', ')}`);
  else console.log(`  ✓ ${no} ${c.name} ${c.categoryRaw} コスト${c.cost} 軽減${c.reduction.length} Lv${JSON.stringify(c.lvCosts)} BP${JSON.stringify(c.lvBps)}${c.shinkai ? ' 真界放' : ''}`);
  if (!problems.length) cards.push(c);
  await sleep(1000);
}

const kt = s => JSON.stringify(s); // Kotlin の文字列リテラルとしても有効 (\n, \" を含む)
const list = (xs, f) => `listOf(${xs.map(f).join(', ')})`;
const today = new Date().toISOString().slice(0, 10);
let out = `package bstools.model

// このファイルは engine/tools/fetch-official-cards.mjs が生成する。手で編集しないこと。
// 出典: 公式カードリスト https://www.battlespirits.com/cardlist/ (取得日 ${today})
// シンボルは公式ページに文字情報がないため「カードの色のシンボル1個」としている (未確認)。

/** 公式カードリストから取得したカード情報 (カード番号 → CardData) */
val OFFICIAL_CARDS: Map<String, CardData> = mapOf(
`;
for (const c of cards) {
  const color = c.colors[0] ?? 'GREEN';
  const symbols = c.category === 'MAGIC' ? [] : [color];
  const lvCosts = c.lvCosts.length ? c.lvCosts : [0];
  out += `    ${kt(c.no)} to CardData(\n` +
    `        cardNo = ${kt(c.no)}, name = ${kt(c.name)}, cost = ${c.cost},\n` +
    `        colors = ${list(c.colors.length ? c.colors : [color], x => 'CardColor.' + x)},\n` +
    `        reductionSymbols = ${list(c.reduction, x => 'CardColor.' + x)},\n` +
    `        category = CardCategory.${c.category}, lvCosts = ${list(lvCosts, String)},\n` +
    `        symbols = ${list(symbols, x => 'CardColor.' + x)}, systems = ${list(c.systems, kt)},\n` +
    `        effect = ${kt(c.effect)},\n` +
    `        lvBps = ${list(c.lvBps, String)}, shinkai = ${c.shinkai}\n    ),\n`;
}
out += `)\n`;
fs.writeFileSync(OUT, out);
console.log(`生成: ${OUT} (${cards.length} 枚 / デッキ内 ${cardNos.size} 種)`);
