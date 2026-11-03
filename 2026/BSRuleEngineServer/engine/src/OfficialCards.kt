package bstools.model

import bstools.model.cards.*

/**
 * 全カード定義リスト。
 * カード名称、属性、系統、BP、効果等のマスターデータは各カードごとのファイルに定義される。
 */
val ALL_CARD_DEFINITIONS: List<CardDefinition> = listOf(
    Card26RSD03_001,
    Card26RSD03_002,
    Card26RSD03_003,
    Card26RSD03_004,
    Card26RSD03_005,
    Card26RSD03_006,
    Card26RSD03_007,
    Card26RSD03_008,
    Card26RSD03_009,
    Card26RSD03_010,
    Card26RSD03_011,
    Card26RSD03_013,
    Card26RSD03_014,
    Card26RSD03_X01,
    Card26RSD03_X02,
    Card26RSD05_001,
    Card26RSD05_002,
    Card26RSD05_003,
    Card26RSD05_004,
    Card26RSD05_005,
    Card26RSD05_006,
    Card26RSD05_007,
    Card26RSD05_008,
    Card26RSD05_009,
    Card26RSD05_010,
    Card26RSD05_011,
    Card26RSD05_012,
    Card26RSD05_013,
    Card26RSD05_014,
    Card26RSD05_X01,
    Card26RSD05_X02,
    CardBS76_033,
    CardBS76_034,
    CardBS76_035,
    CardBS76_037,
    CardBS76_038,
    CardBS76_039,
    CardBS76_041,
    CardBS76_042,
    CardBS76_043,
    CardBS76_072,
    CardBS76_085,
    CardBS76_086,
    CardBS76_CX03,
    CardBS76_T001,
    CardBS76_X07,
    CardPX26_02,
)

/** 公式カードリストから取得したカード情報 (カード番号 → CardData) */
val OFFICIAL_CARDS: Map<String, CardData> = ALL_CARD_DEFINITIONS.associate { it.cardNo to it.data }
