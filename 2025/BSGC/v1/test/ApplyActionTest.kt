import kotlin.test.Test

class WorldTest {
    @Test
    fun hands012() {
        val hand0 = listOf<Card.SpiritCard>()
        val hand1 = listOf(ドラグノ偵察兵)
        val hand2 = listOf(ドラグノ偵察兵, ロクケラトプス)

        val g0 = Game(Board(hand = hand0))
        val g1 = Game(Board(hand = hand1))
        val g2 = Game(Board(hand = hand2))

        assert(g0.listChoices().toSet() == setOf(Action.DoNothing))
        assert(g1.listChoices().toSet() == setOf(Action.DoNothing, Action.Summon(ドラグノ偵察兵)))
        assert(
            g2.listChoices().toSet() == setOf(
                Action.DoNothing,
                Action.Summon(ドラグノ偵察兵),
                Action.Summon(ロクケラトプス)
            )
        )
    }

    @Test
    fun field12() {
        val s1 = FieldObject.Spirit(ドラグノ偵察兵, 1)
        val g1 = Game(Board(hand = emptyList(), fieldObjects = listOf(s1)))
        assert(g1.listChoices().toSet() == setOf(Action.DoNothing, Action.SwapReserveCores(0)))

        val g2 = Game(Board(hand = emptyList(), fieldObjects = listOf(s1, s1)))
        assert(
            g2.listChoices().toSet() == setOf(
                Action.DoNothing,
                Action.SwapReserveCores(0),
                Action.SwapReserveCores(1),
                Action.SwapObjectCores(0, 1)
            )
        )
        val g3 = Game(Board(hand = emptyList(), fieldObjects = listOf(s1, s1, s1)))
        assert(
            g3.listChoices().toSet() == setOf(
                Action.DoNothing,
                Action.SwapReserveCores(0),
                Action.SwapReserveCores(1),
                Action.SwapReserveCores(2),
                Action.SwapObjectCores(0, 1),
                Action.SwapObjectCores(0, 2),
                Action.SwapObjectCores(1, 2),
            )
        )
    }

    @Test
    fun filedSyms() {
        val s1 = FieldObject.Spirit(ドラグノ偵察兵, 1)
        val g1 = Game(Board(hand = emptyList(), fieldObjects = listOf(s1)))
        assert(g1.fieldSymbols() == R1)
    }
}

val ドラグノ偵察兵 = Card.SpiritCard(
    name = "ドラグノ偵察兵",
    cost = 2,
    reductionSymbol = 1,
    levelCosts = mapOf(1 to 1, 2 to 2),
    symbols = R1,
)

val ロクケラトプス = Card.SpiritCard(
    name = "ロクケラトプス",
    cost = 1,
    reductionSymbol = 1,
    levelCosts = mapOf(1 to 1, 2 to 2, 3 to 3),
    symbols = R1,
)
