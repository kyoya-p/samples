package bstools.model

// このファイルは engine/tools/fetch-official-cards.mjs が生成する。手で編集しないこと。
// 出典: 公式カードリスト https://www.battlespirits.com/cardlist/ (取得日 2026-10-25)
// シンボルは公式ページに文字情報がないため「カードの色のシンボル1個」としている (未確認)。

/** 公式カードリストから取得したカード情報 (カード番号 → CardData) */
val OFFICIAL_CARDS: Map<String, CardData> = mapOf(
    "26RSD03-001" to CardData(
        cardNo = "26RSD03-001", name = "フラッティ", cost = 2,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[フラッシュ]『相手のアタックステップ』手札のこのカードは、召喚コストの支払いと上に置くコアをリザーブから使用して召喚できる。",
        lvBps = listOf(1000, 3000), shinkai = false
    ),
    "26RSD03-002" to CardData(
        cardNo = "26RSD03-002", name = "パッファー", cost = 3,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『召喚時』ボイドからコア1個を自分のトラッシュに置く。",
        lvBps = listOf(2000, 3000), shinkai = false
    ),
    "26RSD03-003" to CardData(
        cardNo = "26RSD03-003", name = "ラスヘッド", cost = 3,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("海緑", "甲魚"),
        effect = "[Lv1-2]『召喚時』コスト4以下の相手のスピリット1体を指定できる。それを疲労させる。\n[Lv2]《真界放》『アタック中』このスピリットをBP+2000する。",
        lvBps = listOf(3000, 5000), shinkai = true
    ),
    "26RSD03-004" to CardData(
        cardNo = "26RSD03-004", name = "ストラレイ", cost = 3,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("海緑", "甲魚"),
        effect = "",
        lvBps = listOf(4000, 7000), shinkai = false
    ),
    "26RSD03-005" to CardData(
        cardNo = "26RSD03-005", name = "モラモーラ", cost = 4,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("海緑", "甲魚"),
        effect = "",
        lvBps = listOf(5000, 8000), shinkai = false
    ),
    "26RSD03-006" to CardData(
        cardNo = "26RSD03-006", name = "海幻コエラ＝カンサス", cost = 5,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『召喚時』このスピリット以外の系統：「甲魚」を持つ自分のスピリット1体を指定できる。ボイドからコア1個をそれに置く。その後、疲労状態の系統：「甲魚」を持つ自分のスピリットがいるなら、自分のスピリット1体を指定できる。それを回復させる。",
        lvBps = listOf(4000, 6000), shinkai = false
    ),
    "26RSD03-007" to CardData(
        cardNo = "26RSD03-007", name = "ガリザロト", cost = 5,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『アタック時』疲労状態の相手のスピリット1体を指定できる。このバトル中、それは可能なら疲労状態でもブロックしなければならない。\n[Lv2]《真界放》『アタック中』このスピリットをBP+2000する。",
        lvBps = listOf(5000, 6000), shinkai = true
    ),
    "26RSD03-008" to CardData(
        cardNo = "26RSD03-008", name = "重顎ドロゴリアス", cost = 5,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『アタック中』系統：「甲魚」を持つ自分のスピリット1体につき、このスピリットをBP+1000する。\n[Lv2]『アタック時』コスト3以下の相手のスピリット1体を指定できる。それを疲労させる。",
        lvBps = listOf(5000, 6000), shinkai = false
    ),
    "26RSD03-009" to CardData(
        cardNo = "26RSD03-009", name = "透笠オリンディアス", cost = 6,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『召喚時』自分のデッキの上から3枚をオープンする。その中のコスト3以下で系統：「甲魚」を持つカード1枚を召喚/配置できる。残りは好きな順でデッキの下に戻す。\n[Lv2]『ブロック時』相手のスピリット1体を指定できる。それを疲労させる。",
        lvBps = listOf(6000, 7000), shinkai = false
    ),
    "26RSD03-010" to CardData(
        cardNo = "26RSD03-010", name = "最奥：緑淵深", cost = 3,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.NEXUS, lvCosts = listOf(0, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『自分のアタックステップ』アタックしている系統：「甲魚」を持つ自分のスピリットすべてをBP+1000する。\n[Lv2]《真界放》【起動：メイン】〔ターン1回〕自分の手札のコスト4以下で系統：「甲魚」を持つカード1枚を相手に見せる。それをデッキの下に戻す▶1枚ドローする。",
        lvBps = listOf(), shinkai = true
    ),
    "26RSD03-011" to CardData(
        cardNo = "26RSD03-011", name = "水泡群域", cost = 4,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.NEXUS, lvCosts = listOf(0, 2),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "[Lv1-2]『配置時』相手のスピリット1体を指定できる。それを疲労させる。\n[Lv2]『自分のメインステップ開始時』疲労状態の系統：「甲魚」を持つ自分のスピリットが2体以上いるなら、ボイドからコア1個を自分のリザーブに置く。",
        lvBps = listOf(), shinkai = false
    ),
    "26RSD03-013" to CardData(
        cardNo = "26RSD03-013", name = "ボルテクスシェイブ", cost = 4,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("甲魚"),
        effect = "[フラッシュ]相手のスピリット1体を指定する。それを疲労させる。",
        lvBps = listOf(), shinkai = false
    ),
    "26RSD03-014" to CardData(
        cardNo = "26RSD03-014", name = "テンタクルアタック", cost = 6,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("甲魚"),
        effect = "《ソウルマジック：緑》自分の緑シンボルがあればだけで使用できる。\n[フラッシュ]相手のスピリット1体を指定する。それを重疲労させる。",
        lvBps = listOf(), shinkai = false
    ),
    "26RSD03-X01" to CardData(
        cardNo = "26RSD03-X01", name = "甲手スクィード", cost = 5,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.GREEN), systems = listOf("海緑", "甲魚"),
        effect = "[Lv1-2]『召喚時』疲労状態の相手のスピリット1体を指定できる。それを重疲労させる。\n[Lv2]『アタック中』〔ターン1回〕バトル終了時、このスピリットを回復できる。",
        lvBps = listOf(5000, 7000), shinkai = false
    ),
    "26RSD03-X02" to CardData(
        cardNo = "26RSD03-X02", name = "深巣ダンテクレオ", cost = 6,
        colors = listOf(CardColor.GREEN),
        reductionSymbols = listOf(CardColor.GREEN, CardColor.GREEN, CardColor.GREEN),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 4),
        symbols = listOf(CardColor.GREEN), systems = listOf("甲魚"),
        effect = "《継召》トラッシュのEXシンボルを除外し軽減できる。\n[Lv1-2]『召喚時』系統：「甲魚」を持つ自分のスピリット2体までを指定する。このターン中、それらをBP+3000する。\n[Lv2]《真界放》『自分のアタックステップ』【起動：フラッシュ】〔ターン1回〕このスピリット以外の系統：「甲魚」を持つ自分のスピリット1体を指定する。それを回復させる。",
        lvBps = listOf(6000, 9000), shinkai = true
    ),
    "26RSD05-001" to CardData(
        cardNo = "26RSD05-001", name = "クイール", cost = 2,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.YELLOW), systems = listOf("黄玉", "雷竜"),
        effect = "[Lv2]『破壊時』相手のスピリット1体を指定できる。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(1000, 2000), shinkai = false
    ),
    "26RSD05-002" to CardData(
        cardNo = "26RSD05-002", name = "アマール", cost = 3,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv1-2]『召喚時』自分のデッキの上から3枚をオープンできる。その中の系統：「雷竜」を持つマジックカード1枚を手札に加える。残りは好きな順でデッキの下に戻す。",
        lvBps = listOf(2000, 3000), shinkai = false
    ),
    "26RSD05-003" to CardData(
        cardNo = "26RSD05-003", name = "ラニラヤ", cost = 3,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.YELLOW), systems = listOf("黄玉", "雷竜"),
        effect = "[Lv1-2]『破壊時』相手のスピリット1体を指定できる。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(3000, 4000), shinkai = false
    ),
    "26RSD05-004" to CardData(
        cardNo = "26RSD05-004", name = "ディヴァエス", cost = 4,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv2]《真界放》『アタック時』相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(4000, 6000), shinkai = true
    ),
    "26RSD05-005" to CardData(
        cardNo = "26RSD05-005", name = "ルクアンス", cost = 4,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("黄玉", "雷竜"),
        effect = "",
        lvBps = listOf(5000, 8000), shinkai = false
    ),
    "26RSD05-006" to CardData(
        cardNo = "26RSD05-006", name = "セマローグ", cost = 5,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("黄玉", "雷竜"),
        effect = "[Lv1-2]『アタック時』相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。\n[Lv2]《真界放》『アタック中』〔重複不可〕自分が系統：「雷竜」を持つマジックカードを使用した時、1枚ドローする。",
        lvBps = listOf(5000, 7000), shinkai = true
    ),
    "26RSD05-007" to CardData(
        cardNo = "26RSD05-007", name = "雷探ペルボーグ", cost = 5,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv1-2]『召喚時』自分のデッキの上から3枚をオープンできる。その中の「雷探ペルボーグ」以外の系統：「雷竜」を持つカード1枚を手札に加える。残りは好きな順でデッキの下に戻す。",
        lvBps = listOf(3000, 4000), shinkai = false
    ),
    "26RSD05-008" to CardData(
        cardNo = "26RSD05-008", name = "雷天拳ウィーギル", cost = 6,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv1-2]『召喚時』相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。\n[Lv2]《真界放》『アタック時』相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(6000, 7000), shinkai = true
    ),
    "26RSD05-009" to CardData(
        cardNo = "26RSD05-009", name = "雷坑道", cost = 3,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.NEXUS, lvCosts = listOf(0, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv1-2]『自分のアタックステップ』系統：「雷竜」を持つ自分のスピリットすべては、BP1000以下の相手のスピリットからブロックされない。\n[Lv2]『自分のメインステップ開始時』〔重複不可〕疲労状態の系統：「雷竜」を持つ自分のスピリットがいるなら、相手のスピリット1体を指定できる。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(), shinkai = false
    ),
    "26RSD05-010" to CardData(
        cardNo = "26RSD05-010", name = "最奥：大雷山嶺", cost = 4,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.NEXUS, lvCosts = listOf(0, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "[Lv1-2]『相手のアタックステップ開始時』色1色を指定できる。このステップ中、BP1000以下の相手のスピリットすべては、その色のシンボル1つを失う。\n[Lv2]《真界放》『自分のアタックステップ』〔重複不可・ターン1回〕自分のスピリットの効果で、BP0の相手のスピリットを破壊した時、1枚ドローする。",
        lvBps = listOf(), shinkai = true
    ),
    "26RSD05-011" to CardData(
        cardNo = "26RSD05-011", name = "ファイアピラー", cost = 3,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("雷竜"),
        effect = "[メイン]1枚ドローする。\n[フラッシュ]相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(), shinkai = false
    ),
    "26RSD05-012" to CardData(
        cardNo = "26RSD05-012", name = "リバースサンダー", cost = 3,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("雷竜"),
        effect = "トラッシュのこのカードは、系統：「雷竜」を持つ自分のスピリットが破壊された時、手札に戻せる。自身は同じカード名のこの効果をターンに1回しか使えない。\n[フラッシュ]相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(), shinkai = false
    ),
    "26RSD05-013" to CardData(
        cardNo = "26RSD05-013", name = "ネストリング", cost = 4,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("雷竜"),
        effect = "《継召》トラッシュのEXシンボルを除外し軽減できる。\n[メイン]自分の手札の系統：「雷竜」を持つカード2枚までを相手に見せる。それらを好きな順でデッキの下に戻す。2枚戻したなら、3枚ドローする。\n[フラッシュ]自分のスピリット1体を指定する。このターン中、それをBP+3000する。",
        lvBps = listOf(), shinkai = false
    ),
    "26RSD05-014" to CardData(
        cardNo = "26RSD05-014", name = "トリプルサンダー", cost = 6,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("雷竜"),
        effect = "《ソウルマジック：黄》自分の黄シンボルがあればだけで使用できる。\n[フラッシュ]相手のスピリット1体を指定する。このターン中、それをBP-2000する。その後、それがBP0なら破壊する。これらを3回行う。",
        lvBps = listOf(), shinkai = false
    ),
    "26RSD05-X01" to CardData(
        cardNo = "26RSD05-X01", name = "雷噴ペールクアール", cost = 7,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "《継召》トラッシュのEXシンボルを除外し軽減できる。\n[Lv1-2]『アタック中』このスピリットはBP4000以下の相手のスピリットからブロックされない。\n[Lv2]『自分のアタックステップ開始時』このターン中、相手のスピリットすべてをBP-2000する。その後、それらがBP0なら破壊する。",
        lvBps = listOf(8000, 10000), shinkai = false
    ),
    "26RSD05-X02" to CardData(
        cardNo = "26RSD05-X02", name = "雷術ルクーナス", cost = 7,
        colors = listOf(CardColor.YELLOW),
        reductionSymbols = listOf(CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW, CardColor.YELLOW),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.YELLOW), systems = listOf("雷竜"),
        effect = "《継召》トラッシュのEXシンボルを除外し軽減できる。\n[Lv1-2]『召喚時』相手のスピリット1体を指定する。このターン中、それをBP-4000する。その後、それがBP0なら破壊する。\n[Lv2]《真界放》『アタック時』相手のスピリット1体を指定する。このターン中、それをBP-4000する。その後、それがBP0なら破壊する。",
        lvBps = listOf(7000, 9000), shinkai = true
    ),
    "BS76-033" to CardData(
        cardNo = "BS76-033", name = "プラチナム・グッピー", cost = 2,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "空魚"),
        effect = "このカードが系統：「旗種」を持つ効果でデッキからオープンされたとき、自分のトークンの「プラチナム・バグ」1体を出せる。［Lv1-2］『このスピリットの召喚時』魂状態/煌臨元の自分の白のフラッグネクサスカード1枚を配置できる。",
        lvBps = listOf(2000, 4000), shinkai = false
    ),
    "BS76-034" to CardData(
        cardNo = "BS76-034", name = "プラチナム・パラボリザード", cost = 3,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "甲竜"),
        effect = "このカードが系統：「旗種」を持つ効果でデッキからオープンされたとき、自分のトークンの「プラチナム・バグ」1体を出せる。［Lv1-2］『このスピリットの召喚時』自分のカウント+1する。その後、ボイドからコア1個ずつをこのスピリットと自分の「プラチナム・バグ」1体に置ける。\n［Lv2］お互い、コストを支払わずにミラージュカードをセットするとき、そのカードの本来のコストを支払わなければセットできない。",
        lvBps = listOf(3000, 4000), shinkai = false
    ),
    "BS76-035" to CardData(
        cardNo = "BS76-035", name = "プラチナム・バシリスク", cost = 4,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2, 5),
        symbols = listOf(CardColor.WHITE), systems = listOf("光契約", "旗種", "甲竜"),
        effect = "［フラッシュ］《F契約煌臨：光契約》『自分のターン』自分のをトラッシュに置くことで、対象の自分のスピリット/ネクサスに手札から重ねる。［Lv1-2-3］『このスピリットの煌臨/アタック時』ターンに1回、自分のデッキを上から3枚オープンできる。その中の系統：「旗種」を持つ白のカード1枚を手札に加える。自分の「プラチナム・バグ」1体を破壊することで、この効果で手札に加えるカードの枚数を+1枚する。残ったカードはデッキの下に戻す。\n［Lv2-3］『このスピリットのアタック時』煌臨元を含む自分の「プラチナム・バグ」1体につき、相手の創界神ネクサスのコア3個をボイドに置ける。",
        lvBps = listOf(4000, 6000, 8000), shinkai = false
    ),
    "BS76-037" to CardData(
        cardNo = "BS76-037", name = "プラチナム・トリバ", cost = 4,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "光虫"),
        effect = "デッキからオープンされたこのカードは、自分のミラージュをセットしていないとき、コストを支払わずにセットできる。［ミラージュ：コスト2（このカードは手札からセットできる）］【セット中】自分が《F契約煌臨》したとき、自分のトークンの「プラチナム・バグ」1体を出せる。そうしたとき、このターンの間、そのトークンは相手の効果を受けない。",
        lvBps = listOf(5000, 7000), shinkai = false
    ),
    "BS76-038" to CardData(
        cardNo = "BS76-038", name = "プラチナム・アゲハ", cost = 5,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2, 5),
        symbols = listOf(CardColor.WHITE), systems = listOf("光契約", "旗種", "光虫"),
        effect = "［フラッシュ］《F契約煌臨：光契約＆C1以上》『お互いのアタックステップ』自分のをトラッシュに置くことで、対象の自分のスピリット/ネクサスに手札から重ねる。［Lv1-2-3］『このスピリットの煌臨/アタック時』相手のスピリット/ネクサス1つをデッキの下に戻すことで、ボイドからコア1個をこのスピリットに置く。\n［Lv2-3］『このスピリットのアタック/ブロック時』自分のトークンの「プラチナム・バグ」1体を出せる。",
        lvBps = listOf(4000, 8000, 11000), shinkai = false
    ),
    "BS76-039" to CardData(
        cardNo = "BS76-039", name = "プラチナム・アーマードフィン", cost = 6,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2, 5),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "空魚"),
        effect = "〔ターンに1回：同名〕このカードを召喚するとき、自分の「プラチナム・バグ」1体を破壊することで、このカードをコスト3にする。［Lv1-2-3］『このスピリットの召喚/アタック時』自分のデッキを上から3枚オープンできる。その中の系統：「旗種」を持つ白のカード1枚を手札に加える。残ったカードはデッキの下に戻す。\n［Lv2-3］『自分のアタックステップ』系統：「旗種」を持つ自分の白のスピリットがアタックしている間、相手は効果でアタックステップを終了できない。",
        lvBps = listOf(5000, 9000, 12000), shinkai = false
    ),
    "BS76-041" to CardData(
        cardNo = "BS76-041", name = "プラチナム・マンティス", cost = 8,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3, 5),
        symbols = listOf(CardColor.WHITE), systems = listOf("光契約", "旗種", "光虫"),
        effect = "［フラッシュ］《F契約煌臨：光契約＆C6以上》『お互いのアタックステップ』自分のをトラッシュに置くことで、対象の自分のスピリット/ネクサスに手札から重ねる。このカードが系統：「旗種」を持つ効果でデッキからオープンされたとき、自分のトークンの「プラチナム・バグ」1体を出せる。［Lv1-2-3］『このスピリットの煌臨/アタック時』相手のスピリット/アルティメット/ネクサス1つをデッキの下に戻せる。自分の「プラチナム・バグ」1体を破壊することで、この効果でデッキに戻す数を+1する。",
        lvBps = listOf(7000, 13000, 15000), shinkai = false
    ),
    "BS76-042" to CardData(
        cardNo = "BS76-042", name = "プラチナム・ジンベー", cost = 9,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "空魚"),
        effect = "セットしているこのカードは、自分の「プラチナム・バグ」が出たとき、自分のカウント3以上なら、ただちに発動できる。［バースト：相手の『このスピリット/ブレイヴの召喚時』発揮後］煌臨元を含む自分の「光虫の旗手ファラ」1つにつき、自分のトークンの「プラチナム・バグ」1体を好きなだけ出す。その後、ボイドからコア1個ずつを自分の「プラチナム・バグ」3体に置ける。この効果発揮後、このカードをコストを支払わずに召喚する。\n［Lv2］『このスピリットのアタック/ブロック時』相手のスピリット/ネクサス1つをデッキの下に戻せる。",
        lvBps = listOf(8000, 14000), shinkai = false
    ),
    "BS76-043" to CardData(
        cardNo = "BS76-043", name = "プラチナム・イグアナ", cost = 10,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 3),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "甲竜"),
        effect = "［フラッシュ］手札にあるこのカードは召喚できる。この効果で召喚するとき、自分の「プラチナム・バグ」3体を破壊することで、このカードをコスト1にする。この効果で召喚したとき、相手のスピリット1体をデッキの下に戻せる。［Lv1-2］系統：「旗種」を持つ自分の白のスピリットすべてに“【重装甲：赤/紫/黄/青】このスピリットは、相手の赤/紫/黄/青のスピリット/ブレイヴ/ネクサス/マジックの効果を受けない”を与える。",
        lvBps = listOf(12000, 16000), shinkai = false
    ),
    "BS76-072" to CardData(
        cardNo = "BS76-072", name = "白の大音楽堂", cost = 3,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.NEXUS, lvCosts = listOf(0, 1),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種"),
        effect = "［Lv1-2］『このネクサスの配置時』自分のデッキを上から3枚オープンできる。その中の系統：「旗種」を持つ白のカードを、自分の手札と好きなだけ入れ替える。オープンしたカードはデッキの下に戻す。この効果で入れ替えたとき、自分のカウント+1する。\n［Lv2］自分の手札にある系統：「旗種」を持つ白のカードすべては、相手の効果を受けない。さらに、〔ターンに1回：同名〕自分が「プラチナム・バグ」を出すとき、対象すべては、Lv1コストと同じになるようにボイドからコアを置いて出せる。",
        lvBps = listOf(), shinkai = false
    ),
    "BS76-085" to CardData(
        cardNo = "BS76-085", name = "フォトンサークル", cost = 2,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("旗種"),
        effect = "〔ターンに1回：同名〕トラッシュにあるこのカードは、系統：「光契約」を持つ自分のスピリットが《F契約煌臨》したとき、手札に戻せる。\n［フラッシュ］自分のトークンの「プラチナム・バグ」1体を出せる。その後、自分のカウント6以上のとき、このターンの間、自分の「プラチナム・バグ」すべては相手の効果を受けない。",
        lvBps = listOf(), shinkai = false
    ),
    "BS76-086" to CardData(
        cardNo = "BS76-086", name = "ラージスクアッシュ", cost = 4,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.MAGIC, lvCosts = listOf(0),
        symbols = listOf(), systems = listOf("旗種"),
        effect = "手札にあるこのカードは、相手の効果を受けない。使用したこのカードは、煌臨元を含む自分のフラッグネクサスがあるとき、このマジックの効果発揮後まで、相手の効果を受けない。\n［フラッシュ］相手のスピリット/アルティメット1体までを手札に戻す。その後、自分の「プラチナム・バグ」1体を破壊することで、このターンの間、自分のライフは1しか減らない。",
        lvBps = listOf(), shinkai = false
    ),
    "BS76-CX03" to CardData(
        cardNo = "BS76-CX03", name = "光虫の旗手ファラ", cost = 3,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.NEXUS, lvCosts = listOf(0),
        symbols = listOf(CardColor.WHITE), systems = listOf("フラッグ", "光契約", "旗種"),
        effect = "このカード/ネクサスの効果すべては相手の効果を受けず、魂状態を含むこのネクサスには自分のフィールド/リザーブのコアを好きなだけ置いて《F契約煌臨》できる。【契約煌臨元】/【ネクサス】［Lv1］『このスピリット/ネクサスの配置/アタック時』自分のトークンの「プラチナム・バグ」1体を出せる。このとき、ターンに1回、対象のLv1コストと同じになるようにボイドからコアを置いて出せる。【契約煌臨元】/【ネクサス】［Lv1］自分が《F契約煌臨》したか、自分の「プラチナム・バグ」が出たとき、自分のカウント+1できる（カウントは最大10まで）。その後、《F契約煌臨》していたとき、自分の「プラチナム・バグ」1体を破壊できる。そうしたとき、相手のバースト1つをデッキの下に戻す。",
        lvBps = listOf(), shinkai = false
    ),
    "BS76-T001" to CardData(
        cardNo = "BS76-T001", name = "プラチナム・バグ", cost = 1,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(),
        category = CardCategory.SPIRIT, lvCosts = listOf(1),
        symbols = listOf(CardColor.WHITE), systems = listOf("旗種", "光虫"),
        effect = "このカードはデッキに入れられず、魂状態/煌臨元を含む自分の「光虫の旗手ファラ」がなければ出せない。【契約煌臨元】/【スピリット】［Lv1］自分の効果か相手によってフィールドを離れるこのスピリットを、《F契約煌臨》を持つ自分の白のスピリットの下に煌臨元カードとして追加できる。【契約煌臨元】このスピリットは、BP+1000され、相手のスピリット/アルティメットの効果を受けない。さらに、〔重複不可〕このスピリットの煌臨元カードが6枚以上の間、系統：「旗種」を持つ自分の白1色のスピリットすべてをBP+10000し、白シンボル1つを追加する。",
        lvBps = listOf(1000), shinkai = false
    ),
    "BS76-X07" to CardData(
        cardNo = "BS76-X07", name = "光翅の導き手ファラ・ルクス", cost = 7,
        colors = listOf(CardColor.WHITE),
        reductionSymbols = listOf(CardColor.WHITE, CardColor.WHITE, CardColor.WHITE, CardColor.WHITE),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2, 5),
        symbols = listOf(CardColor.WHITE), systems = listOf("光契約", "旗種", "光虫"),
        effect = "［フラッシュ］《F契約煌臨：光契約＆C4以上》『お互いのアタックステップ』自分のをトラッシュに置くことで、対象の自分のスピリット/ネクサスに手札から重ねる。【煌臨中】［Lv1-2-3：フラッシュ］『お互いのアタックステップ』自分の「プラチナム・バグ」1体を破壊できる。そうしたとき、相手のスピリット/アルティメット1体をデッキの下に戻す。この効果は相手の効果では防げない。その後、ターンに1回、このスピリットは回復できる。\n【煌臨中】［Lv2-3：フラッシュ］『自分のアタックステップ』煌臨元の自分の「プラチナム・バグ」4枚を破棄することで、相手のライフのコア2個までをトラッシュに置く。",
        lvBps = listOf(7000, 13000, 16000), shinkai = false
    ),
    "PX26-02" to CardData(
        cardNo = "PX26-02", name = "ピットリポーター ポリッシュ", cost = 4,
        colors = listOf(CardColor.RED, CardColor.PURPLE, CardColor.GREEN, CardColor.WHITE, CardColor.YELLOW, CardColor.BLUE),
        reductionSymbols = listOf(),
        category = CardCategory.SPIRIT, lvCosts = listOf(1, 2, 4),
        symbols = listOf(CardColor.RED), systems = listOf("旗種", "眷属", "道化"),
        effect = "自分の効果でデッキからオープンされたこのカードは、セットできる。〔ターンに1回：同名〕セットしているこのカードは、系統：「旗種」/「十冠」/「神皇」を持つ自分のスピリットがアタックしたとき、ただちに発動できる。［Ｓバースト：自分か相手の《封印》後］1枚ドローし、ボイドからコア1個を自分のリザーブに置く。この効果発揮後、このカードをコストを支払わずに召喚する。\n［Lv2-3］このスピリットのシンボルは赤/紫/緑/白/青としても扱う。",
        lvBps = listOf(3000, 5000, 6000), shinkai = false
    ),
)
