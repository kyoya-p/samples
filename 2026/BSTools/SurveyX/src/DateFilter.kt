package xtool

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * X検索用日付フィルタ文字列 (since:YYYY-MM-DD / until:YYYY-MM-DD) をパース・生成する。
 *
 * 対応形式例:
 * - "8" -> 直前8か月 (since:YYYY-MM-DD)
 * - "2508-" -> 2025年8月1日〜現在 (since:2025-08-01)
 * - "2508-2608" -> 2025年8月1日〜2026年8月1日 (since:2025-08-01 until:2026-08-01)
 * - "-2608" -> 〜2026年8月1日 (until:2026-08-01)
 * - "202508-202608" -> 2025年8月1日〜2026年8月1日
 * - "250801-260815" -> 2025年8月1日〜2026年8月15日
 */
fun parseDateFilter(spec: String, now: LocalDate = LocalDate.now()): String {
    val range = parseDateRange(spec, now) ?: return ""
    val sinceClause = "since:${range.first.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
    val untilClause = if (range.second != now) "until:${range.second.format(DateTimeFormatter.ISO_LOCAL_DATE)}" else ""
    return listOf(sinceClause, untilClause).filter { it.isNotEmpty() }.joinToString(" ")
}

/**
 * 日付指定文字列から (開始日, 終了日) の LocalDate ペアを抽出する。
 */
fun parseDateRange(spec: String, now: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate>? {
    val trimmed = spec.trim()
    if (trimmed.isEmpty()) return null

    // パターン1: 直前 N か月 (例: "8", "12")
    if (trimmed.matches(Regex("""^\d{1,2}$"""))) {
        val months = trimmed.toLong()
        val sinceDate = now.minusMonths(months)
        return Pair(sinceDate, now)
    }

    // パターン2: ハイフン区切りの範囲 (例: "2508-", "2508-2608", "-2608")
    if (trimmed.contains("-")) {
        val parts = trimmed.split("-", limit = 2)
        val startStr = parts[0].trim()
        val endStr = parts.getOrNull(1)?.trim().orEmpty()

        val startDate = if (startStr.isNotEmpty()) parseDateToken(startStr) ?: now.minusYears(1) else now.minusYears(1)
        val endDate = if (endStr.isNotEmpty()) parseDateToken(endStr) ?: now else now
        return Pair(startDate, endDate)
    }

    // パターン3: 単一の年月/日付指定 (例: "2508", "202508") -> 指定日〜現在
    val date = parseDateToken(trimmed)
    if (date != null) {
        return Pair(date, now)
    }

    return null
}

/**
 * 開始日〜終了日の期間を指定日数（デフォルト30日）ごとに分割した区間リストを生成する。
 * 直近（新しい日付）から過去に向かって順次検索できるよう、新しい区間から降順で返す。
 */
fun splitDateRange(start: LocalDate, end: LocalDate, splitDays: Int = 30): List<Pair<LocalDate, LocalDate>> {
    if (splitDays <= 0 || !start.isBefore(end)) {
        return listOf(Pair(start, end))
    }

    val chunks = mutableListOf<Pair<LocalDate, LocalDate>>()
    var curEnd = end

    while (curEnd.isAfter(start)) {
        val curStart = curEnd.minusDays(splitDays.toLong())
        val actualStart = if (curStart.isBefore(start)) start else curStart
        chunks.add(Pair(actualStart, curEnd))
        curEnd = actualStart
    }

    return chunks
}


fun parseDateToken(token: String): LocalDate? {
    val t = token.replace("/", "").replace("-", "")

    return try {
        when (t.length) {
            4 -> { // YYMM -> 20YY-MM-01
                val yy = t.substring(0, 2).toInt()
                val mm = t.substring(2, 4).toInt()
                LocalDate.of(2000 + yy, mm, 1)
            }
            6 -> {
                val v1 = t.substring(0, 4).toInt()
                if (v1 in 2000..2099) { // YYYYMM -> YYYY-MM-01
                    val mm = t.substring(4, 6).toInt()
                    LocalDate.of(v1, mm, 1)
                } else { // YYMMDD -> 20YY-MM-DD
                    val yy = t.substring(0, 2).toInt()
                    val mm = t.substring(2, 4).toInt()
                    val dd = t.substring(4, 6).toInt()
                    LocalDate.of(2000 + yy, mm, dd)
                }
            }
            8 -> { // YYYYMMDD -> YYYY-MM-DD
                val yyyy = t.substring(0, 4).toInt()
                val mm = t.substring(4, 6).toInt()
                val dd = t.substring(6, 8).toInt()
                LocalDate.of(yyyy, mm, dd)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
