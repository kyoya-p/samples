package BSSim

val Lv111k = listOf(Card.LevelInfo(1, 1, 1000))

class ちょうちん : SpiritCard(Category.SPIRITCARD, "ちょ", Color.Y, 0, Sbl.Y, Sbl.Zero, setOf(Family.妖戒), Lv111k)
class 子フ : SpiritCard(Category.SPIRITCARD, "子フ", Color.Y, 2, Sbl.Y, Sbl.Y * 2, setOf(Family.想獣), Lv111k)
class オルリ : SpiritCard(Category.SPIRITCARD, "オルリ", Color.G, 3, Sbl.G, Sbl.G + Sbl.Gd, setOf(Family.殻人), Lv111k)
class ピグレ : SpiritCard(Category.SPIRITCARD, "ピグレ", Color.G, 3, Sbl.G, Sbl.G, setOf(Family.遊精, Family.天渡), Lv111k)

/*デッキ定義*/

val deck_殻人1 = listOf(
        オルリ()
        , ちょうちん()
        , ピグレ()
        , 子フ()
)
