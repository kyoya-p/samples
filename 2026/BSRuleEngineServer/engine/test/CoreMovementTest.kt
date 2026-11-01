package bstools.model

import kotlin.test.*

/**
 * Verification Test Suite for Operation 1 (Main Step Core Movement) and Operation 4 (Soul Core Swapping).
 *
 * Verifies compliance against:
 * - バトルスピリッツ スタンダード 総合ルール Ver.1.0 (2026/04/10):
 *   - 7-6-1-2-1 / 7-6-1-2-2: メインステップにおけるコア移動の自由度、リザーブ ⇄ フィールド、カード間直接移動、ソウルコアの移動・交換
 *   - 7-8-1: 第2メインステップにおけるコア移動の同一性
 *   - 1-4-2-2: 同一領域・同一カード移動の扱い
 * - 株式会社バンダイ公式Q&A:
 *   - 1コアのスピリット同士でのソウルコアと通常コアの交換が同時解決され消滅しない裁定
 */
class CoreMovementTest {

    @Test
    fun testReserveToFieldCardSingleNormalCore() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 4, p1ReserveSoul = 1)
        val spirit = TestFixtures.createSpirit("fc_spirit_1", normalCores = 1, soulCores = 0)
        p1.field.add(spirit)

        val action = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "MOVE",
            detail = "Reserve -> Spirit (1 normal core)",
            moves = listOf(CoreMove("Reserve", spirit.instanceId, "R", spirit.name, normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, action, messages)

        assertEquals(3, p1.reserve.normal)
        assertEquals(1, p1.reserve.soul)
        assertEquals(2, spirit.cores.normal)
        assertEquals(0, spirit.cores.soul)
        assertEquals(2, spirit.cores.total)
        assertEquals(1, spirit.level) // フラッティ lvCosts: [1, 3] -> 2 cores is Lv1
    }

    @Test
    fun testReserveToFieldCardBatchNormalCores() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 5, p1ReserveSoul = 1)
        val spirit = TestFixtures.createSpirit("fc_spirit_1", normalCores = 1, soulCores = 0)
        p1.field.add(spirit)

        // Move 2 cores in a single atomic action (1..N batch increment)
        val action = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "MOVE",
            detail = "Reserve -> Spirit (2 normal cores)",
            moves = listOf(CoreMove("Reserve", spirit.instanceId, "R", spirit.name, normal = 2, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, action, messages)

        assertEquals(3, p1.reserve.normal)
        assertEquals(3, spirit.cores.normal)
        assertEquals(3, spirit.cores.total)
        assertEquals(2, spirit.level) // フラッティ reaches Lv2 at 3 cores (lvCosts: [1, 3])
    }

    @Test
    fun testFieldCardToReserveMove() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 2, p1ReserveSoul = 1)
        val spirit = TestFixtures.createSpirit("fc_spirit_1", normalCores = 3, soulCores = 0)
        p1.field.add(spirit)

        // Move 2 cores from Spirit back to Reserve
        val action = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "MOVE",
            detail = "Spirit -> Reserve (2 normal cores)",
            moves = listOf(CoreMove(spirit.instanceId, "Reserve", spirit.name, "R", normal = 2, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, action, messages)

        assertEquals(4, p1.reserve.normal)
        assertEquals(1, spirit.cores.normal)
        assertEquals(1, spirit.level)
        assertFalse(p1.field.isEmpty())
        assertTrue(p1.trashCards.isEmpty())
    }

    @Test
    fun testDirectCardToCardMoveSingleCore() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 2, p1ReserveSoul = 1)
        val spiritA = TestFixtures.createSpirit("fc_spirit_a", normalCores = 2, soulCores = 0)
        val spiritB = TestFixtures.createSpirit("fc_spirit_b", normalCores = 1, soulCores = 0)
        p1.field.add(spiritA)
        p1.field.add(spiritB)

        // Move 1 core directly from Spirit A to Spirit B without touching Reserve
        val action = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "MOVE",
            detail = "Spirit A -> Spirit B (1 normal core)",
            moves = listOf(CoreMove(spiritA.instanceId, spiritB.instanceId, spiritA.name, spiritB.name, normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, action, messages)

        assertEquals(2, p1.reserve.normal, "Reserve must remain unchanged on direct card-to-card move")
        assertEquals(1, spiritA.cores.normal)
        assertEquals(2, spiritB.cores.normal)
        assertEquals(1, spiritA.level)
        assertEquals(1, spiritB.level)
    }

    @Test
    fun testDirectCardToCardMoveBatchCores() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 3, p1ReserveSoul = 0)
        val spiritA = TestFixtures.createSpirit("fc_spirit_a", normalCores = 4, soulCores = 0)
        val spiritB = TestFixtures.createSpirit("fc_spirit_b", normalCores = 1, soulCores = 0)
        p1.field.add(spiritA)
        p1.field.add(spiritB)

        // Move 2 cores in a single batch directly from A to B
        val action = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "MOVE",
            detail = "Spirit A -> Spirit B (2 normal cores)",
            moves = listOf(CoreMove(spiritA.instanceId, spiritB.instanceId, spiritA.name, spiritB.name, normal = 2, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, action, messages)

        assertEquals(3, p1.reserve.normal)
        assertEquals(2, spiritA.cores.normal)
        assertEquals(3, spiritB.cores.normal)
        assertEquals(1, spiritA.level)
        assertEquals(2, spiritB.level) // Spirit B reaches Lv2
    }

    @Test
    fun testSoulCoreMovementToFieldCard() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 3, p1ReserveSoul = 1)
        val spirit = TestFixtures.createSpirit("fc_spirit_1", normalCores = 1, soulCores = 0)
        p1.field.add(spirit)

        // Move soul core from Reserve to Spirit
        val action = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "MOVE",
            detail = "Reserve -> Spirit (1 soul core)",
            moves = listOf(CoreMove("Reserve", spirit.instanceId, "R", spirit.name, normal = 0, soul = 1))
        )
        val messages = mutableListOf<String>()
        applyAction(state, action, messages)

        assertEquals(0, p1.reserve.soul)
        assertEquals(3, p1.reserve.normal)
        assertEquals(1, spirit.cores.soul)
        assertEquals(1, spirit.cores.normal)
        assertEquals(2, spirit.cores.total)
    }

    @Test
    fun testMixedNormalAndSoulCoreMovement() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 5, p1ReserveSoul = 1)
        val spirit = TestFixtures.createSpirit("fc_spirit_1", normalCores = 1, soulCores = 0)
        p1.field.add(spirit)

        // Move 2 normal cores and 1 soul core simultaneously in a single action
        val action = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "MOVE",
            detail = "Reserve -> Spirit (2 normal + 1 soul)",
            moves = listOf(CoreMove("Reserve", spirit.instanceId, "R", spirit.name, normal = 2, soul = 1))
        )
        val messages = mutableListOf<String>()
        applyAction(state, action, messages)

        assertEquals(3, p1.reserve.normal)
        assertEquals(0, p1.reserve.soul)
        assertEquals(3, spirit.cores.normal)
        assertEquals(1, spirit.cores.soul)
        assertEquals(4, spirit.cores.total)
        assertEquals(2, spirit.level) // Lv2 satisfied
    }

    @Test
    fun testSoulCoreSwapBetweenReserveAndFieldCard() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 2, p1ReserveSoul = 1)
        val spirit = TestFixtures.createSpirit("fc_spirit_1", normalCores = 2, soulCores = 0)
        p1.field.add(spirit)

        // Swap soul core from Reserve with 1 normal core on Spirit
        val swapAction = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "SWAP",
            detail = "Swap Reserve soul core with Spirit normal core",
            moves = listOf(
                CoreMove("Reserve", spirit.instanceId, "R", spirit.name, normal = 0, soul = 1),
                CoreMove(spirit.instanceId, "Reserve", spirit.name, "R", normal = 1, soul = 0)
            )
        )
        val messages = mutableListOf<String>()
        applyAction(state, swapAction, messages)

        assertEquals(3, p1.reserve.normal)
        assertEquals(0, p1.reserve.soul)
        assertEquals(1, spirit.cores.normal)
        assertEquals(1, spirit.cores.soul)
        assertEquals(2, spirit.cores.total)
        assertEquals(1, spirit.level)
    }

    @Test
    fun testSoulCoreSwapBetweenTwoSpirits() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 2, p1ReserveSoul = 0)
        val spiritA = TestFixtures.createSpirit("fc_spirit_a", normalCores = 1, soulCores = 1)
        val spiritB = TestFixtures.createSpirit("fc_spirit_b", normalCores = 2, soulCores = 0)
        p1.field.add(spiritA)
        p1.field.add(spiritB)

        // Swap soul core on Spirit A with 1 normal core on Spirit B
        val swapAction = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "SWAP",
            detail = "Swap Spirit A soul core with Spirit B normal core",
            moves = listOf(
                CoreMove(spiritA.instanceId, spiritB.instanceId, spiritA.name, spiritB.name, normal = 0, soul = 1),
                CoreMove(spiritB.instanceId, spiritA.instanceId, spiritB.name, spiritA.name, normal = 1, soul = 0)
            )
        )
        val messages = mutableListOf<String>()
        applyAction(state, swapAction, messages)

        assertEquals(2, spiritA.cores.normal)
        assertEquals(0, spiritA.cores.soul)
        assertEquals(2, spiritA.cores.total)

        assertEquals(1, spiritB.cores.normal)
        assertEquals(1, spiritB.cores.soul)
        assertEquals(2, spiritB.cores.total)
    }

    /**
     * Bandai Official Q&A Verification:
     * Q: コアの移動で、ソウルコア1個のスピリットと、コア1個のスピリットのコアを入れ替えてもいいの？
     * A: はい、消滅せずに入れ替えることができます（同時に入れ替える処理になるため、一時的にコアが0個になって消滅する状況は発生しません）。
     */
    @Test
    fun testAtomicSwapPreventsVanishingForOneCoreSpirits() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 0, p1ReserveSoul = 0)
        // Spirit A has exactly 1 soul core (Lv1 keep cost = 1)
        val spiritA = TestFixtures.createSpirit("fc_spirit_a", normalCores = 0, soulCores = 1)
        // Spirit B has exactly 1 normal core (Lv1 keep cost = 1)
        val spiritB = TestFixtures.createSpirit("fc_spirit_b", normalCores = 1, soulCores = 0)
        p1.field.add(spiritA)
        p1.field.add(spiritB)

        assertEquals(1, spiritA.level)
        assertEquals(1, spiritB.level)

        // Swap soul core on A with normal core on B simultaneously
        val swapAction = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "SWAP",
            detail = "Atomic swap 🔶 on Spirit A with 🔷 on Spirit B",
            moves = listOf(
                CoreMove(spiritA.instanceId, spiritB.instanceId, spiritA.name, spiritB.name, normal = 0, soul = 1),
                CoreMove(spiritB.instanceId, spiritA.instanceId, spiritB.name, spiritA.name, normal = 1, soul = 0)
            )
        )
        val messages = mutableListOf<String>()
        applyAction(state, swapAction, messages)

        // Crucial assertions:
        // Neither spirit vanished!
        assertEquals(2, p1.field.size, "Both spirits must persist on field without vanishing")
        assertTrue(p1.trashCards.isEmpty(), "trashCards must be empty")

        // Spirit A now has 1 normal core, 0 soul core, level 1
        assertEquals(1, spiritA.cores.normal)
        assertEquals(0, spiritA.cores.soul)
        assertEquals(1, spiritA.level)

        // Spirit B now has 0 normal core, 1 soul core, level 1
        assertEquals(0, spiritB.cores.normal)
        assertEquals(1, spiritB.cores.soul)
        assertEquals(1, spiritB.level)
    }

    @Test
    fun testInsufficientCoresRejectsMove() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 1, p1ReserveSoul = 0)
        val spirit = TestFixtures.createSpirit("fc_spirit_1", normalCores = 1, soulCores = 0)
        p1.field.add(spirit)

        // Attempt to move 2 cores from Reserve when Reserve only has 1
        val invalidAction = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "MOVE",
            detail = "Attempt to move 2 cores from 1-core Reserve",
            moves = listOf(CoreMove("Reserve", spirit.instanceId, "R", spirit.name, normal = 2, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, invalidAction, messages)

        // State remains unchanged
        assertEquals(1, p1.reserve.normal)
        assertEquals(1, spirit.cores.normal)
    }

    /**
     * User Guidance Verification:
     * Normal-to-normal core swap leaves board position identical (hash invariant).
     * Comprehensive Rule 1-4-2-2: Moving between same configurations is a no-op.
     */
    @Test
    fun testNormalCoreSwapEquivalenceAndHashConsistency() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 2, p1ReserveSoul = 1)
        val spiritA = TestFixtures.createSpirit("fc_spirit_a", normalCores = 2, soulCores = 0)
        val spiritB = TestFixtures.createSpirit("fc_spirit_b", normalCores = 2, soulCores = 0)
        p1.field.add(spiritA)
        p1.field.add(spiritB)

        val hashBefore = positionHash(state)

        // Normal-normal swap between Spirit A and Spirit B
        val swapNormalAction = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "SWAP",
            detail = "Swap 1 normal core between Spirit A and Spirit B",
            moves = listOf(
                CoreMove(spiritA.instanceId, spiritB.instanceId, spiritA.name, spiritB.name, normal = 1, soul = 0),
                CoreMove(spiritB.instanceId, spiritA.instanceId, spiritB.name, spiritA.name, normal = 1, soul = 0)
            )
        )
        val messages = mutableListOf<String>()
        applyAction(state, swapNormalAction, messages)

        val hashAfter = positionHash(state)
        assertEquals(hashBefore, hashAfter, "Normal-to-normal core swap must produce an identical board position hash")
    }

    @Test
    fun testCoreMovementInSecondMainStep() {
        // 総合ルール 7-8-1: 第2メインステップはメインステップと同様のことが行える
        val (state, p1) = TestFixtures.createTestState(
            p1ReserveNormal = 3,
            p1ReserveSoul = 1,
            step = Step.MAIN_2
        )
        val spirit = TestFixtures.createSpirit("fc_spirit_1", normalCores = 1, soulCores = 0)
        p1.field.add(spirit)

        val action = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "MOVE",
            detail = "Move in MAIN_2",
            moves = listOf(CoreMove("Reserve", spirit.instanceId, "R", spirit.name, normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, action, messages)

        assertEquals(2, p1.reserve.normal)
        assertEquals(2, spirit.cores.normal)
    }
}
