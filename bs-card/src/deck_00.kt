package BSSim

enum class Family { 殻人, 想獣, 妖戒, 天渡, 遊精, パイロット, CB, 闘神, 武装, 界渡, 道化, 創界神, ウル, 覇王 }

/*デッキ定義*/

val Lv101k = listOf(Card.LevelInfo(1, 0, 1000))
val Lv113k = listOf(Card.LevelInfo(1, 1, 3000))
val Lv10 = listOf(Card.LevelInfo(1, 0, 0))
val Lv112k = listOf(Card.LevelInfo(1, 1, 2000))

class GNアームズ : BraveCard(Category.BRAVECARD, "GNア", Color.B, 3, Sbl.Zero, Sbl.B * 3, setOf(Family.CB), Lv113k)
class ロックオン : BraveCard(Category.BRAVECARD, "ロック", Color.B, 3, Sbl.Zero, Sbl.B * 2, setOf(Family.パイロット), Lv101k)
class ハロ : NexusCard(Category.NEXUSCARD, "ハロ", Color.B, 3, Sbl.B, Sbl.B * 2, setOf(), Lv10)
class プトレマイオス : NexusCard(Category.NEXUSCARD, "プトレ", Color.B, 2, Sbl.B, Sbl.B * 1, setOf(Family.CB), Lv10)
class ラクシュマナ : SpiritCard(Category.SPIRITCARD, "ラクシ", Color.B, 1, Sbl.B, Sbl.B * 1, setOf(Family.闘神), Lv112k)

val deck_00 = listOf(
        ハロ()
        , ラクシュマナ()
        , GNアームズ()
        , ロックオン()
        , プトレマイオス()
)

