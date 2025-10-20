import kotlin.test.Test

class WorldTest {
    @Test
    fun hands012() {
        val hand0 = listOf<Card.SpiritCard>()
        val hand1 = listOf<Card.SpiritCard>(`ドラグノ偵察兵`)
        val hand2 = listOf<Card.SpiritCard>(`ドラグノ偵察兵`, `ロクケラトプス`)

        val b0 = Board(hand = hand0)
        val b1 = Board(hand = hand1)
        val b2 = Board(hand = hand2)

        assert(b0.listChoices().toSet() == setOf(Action.DoNothing))
        assert(b1.listChoices().toSet() == setOf(Action.DoNothing, Action.Summon(ドラグノ偵察兵)))
        assert(
            b2.listChoices().toSet() == setOf(
                Action.DoNothing,
                Action.Summon(ドラグノ偵察兵),
                Action.Summon(ロクケラトプス)
            )
        )
    }

    @Test
    fun field12() {
        val s1 = FieldObject.Spirit(ドラグノ偵察兵, 1)
        val b1 = Board(hand = emptyList(), fieldObjects = listOf(s1))
        assert(b1.listChoices().toSet() == setOf(Action.DoNothing, Action.SwapReserveCores(0)))

        val b2 = Board(hand = emptyList(), fieldObjects = listOf(s1, s1))
        b2.listChoices().forEach { println(it) } // TODO
        assert(
            b2.listChoices().toSet() == setOf(
                Action.DoNothing,
                Action.SwapReserveCores(0),
                Action.SwapReserveCores(1),
                Action.SwapObjectCores(0, 1)
            )
        )
    }
}

val `ドラグノ偵察兵` = Card.SpiritCard(
    name = "ドラグノ偵察兵",
    cost = 2,
    reductionSymbol = 1,
    levelCosts = mapOf(1 to 1, 2 to 2),
    symbols = 1,
)

val `ロクケラトプス` = Card.SpiritCard(
    name = "ロクケラトプス",
    cost = 1,
    reductionSymbol = 1,
    levelCosts = mapOf(1 to 1, 2 to 2, 3 to 3),
    symbols = 1,

    )
