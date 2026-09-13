package xtool

// page.evalOnSelectorAll に渡すJS関数。マッチした要素配列(articles)を受け取り、
// ツイート情報の配列を返す。DOM構造(data-testid)はX側の仕様変更で崩れる可能性がある。
const val TWEET_EXTRACT_SCRIPT = """
articles => {
  function textOf(el) { return el ? el.innerText.trim() : ''; }
  function statOf(article, testId) {
    const el = article.querySelector(`[data-testid="${'$'}{testId}"]`);
    if (!el) return 0;
    const label = el.getAttribute('aria-label') || textOf(el);
    const match = label.replace(/,/g, '').match(/([\d.]+[KkMm]?)/);
    if (!match) return 0;
    let num = match[1];
    let mul = 1;
    if (/[Kk]${'$'}/.test(num)) { mul = 1000; num = num.slice(0, -1); }
    if (/[Mm]${'$'}/.test(num)) { mul = 1000000; num = num.slice(0, -1); }
    return Math.round(parseFloat(num) * mul);
  }
  return articles.map((article) => {
    const link = article.querySelector('a[href*="/status/"]');
    const href = link ? link.getAttribute('href') : '';
    const idMatch = href.match(/status\/(\d+)/);
    const id = idMatch ? idMatch[1] : null;
    const timeEl = article.querySelector('time');
    const nameEl = article.querySelector('[data-testid="User-Name"]');
    const bodyEl = article.querySelector('[data-testid="tweetText"]');
    let author = '';
    let handle = '';
    if (nameEl) {
      const spans = Array.from(nameEl.querySelectorAll('span')).map((s) => s.innerText);
      author = spans.find((s) => s && !s.startsWith('@')) || '';
      handle = (spans.find((s) => s && s.startsWith('@')) || '').replace('@', '');
    }
    return {
      id,
      url: id ? `https://x.com/i/web/status/${'$'}{id}` : null,
      author,
      handle,
      postedAt: timeEl ? timeEl.getAttribute('datetime') : null,
      text: textOf(bodyEl),
      hasImage: !!article.querySelector('[data-testid="tweetPhoto"]'),
      hasVideo: !!article.querySelector('[data-testid="videoPlayer"]'),
      replyCount: statOf(article, 'reply'),
      retweetCount: statOf(article, 'retweet'),
      likeCount: statOf(article, 'like'),
    };
  }).filter((t) => t.id);
}
"""
