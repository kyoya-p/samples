package bstools.model

import kotlin.test.*

/**
 * Verification Test Suite for Operation 2 (Level Maintenance Cost) and Operation 3 (Vanishing / resolveVanish).
 *
 * Verifies compliance against:
 * - バトルスピリッツ スタンダード 総合ルール Ver.1.0 (2026/04/10):
 *   - 3-3-3-3: ネクサスの維持コスト0と非消滅性（最重要条項）
 *     「コアの数が Lv1 コスト未満の数になったネクサスはフィールドに存在できなくなり、消滅し、待機状態を経てトラッシュに置かれます。
 *      なお、ネクサスは、Lv1 の Lv コストは 0 になりますので、コアが置かれていなくても消滅しません。」
 *   - 3-2-5-4: スピリットの維持コストと消滅
 *     「コアの数が Lv1 コスト未満の数になったスピリットはフィールドに存在できなくなり、消滅し、待機状態を経てトラッシュに置かれます。」
 *   - 2-8-2 / 2-10-3: Lvとシンボル保有条件（Lv0のスピリットはシンボルを持たない、Lv1ネクサスは0コアでもシンボルを保持）
 *   - 12-2-1-1-2: 《真界放》（ソウルコア1個でLv2維持コストを満たす）
 *   - 5-11-1 / 10-2-2-1: 消滅の定義とルール処理による待機状態移行
 *   - 10-2-2-5 / 4-5-4: 消滅時のコア移動先（置かれていたコアはすべてトラッシュではなくリザーブに置く）
 *   - 5-11-2 / 5-10-2: 消滅と破壊の明確な区別
 */
class LevelMaintenanceAndVanishTest {

    /**
     * 総合ルール 3-3-3-3:
     * ネクサスは Lv1 の Lv コストが 0 であり、コアが置かれていなくても消滅せず Lv1 を維持する。
     */
    @Test
    fun testNexusPersistenceAtZeroCores() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 3, p1ReserveSoul = 1)
        // 26RSD03-010 最奥：緑淵深 (category = NEXUS, lvCosts = listOf(0, 3), symbols = listOf(GREEN))
        val nexus = TestFixtures.createNexus("fc_nexus_1", normalCores = 0, soulCores = 0)
        p1.field.add(nexus)

        // 1. Cores = 0 -> level must be 1
        assertEquals(0, nexus.cores.total)
        assertEquals(1, nexus.level, "Nexus with 0 cores must be Lv1 (3-3-3-3)")

        // 2. Retains base symbols even at 0 cores
        assertEquals(listOf(CardColor.GREEN), nexus.symbols, "Nexus with 0 cores must retain its symbols at Lv1 (2-8-2)")

        // 3. resolveVanish must NOT remove nexus from field
        val messages = mutableListOf<String>()
        resolveVanish(p1, messages)

        assertEquals(1, p1.field.size, "Nexus must remain on field after resolveVanish")
        assertTrue(p1.trashCards.isEmpty(), "trashCards must be empty")
        assertEquals(nexus.instanceId, p1.field.first().instanceId)
    }

    /**
     * Moving the last core from a Nexus to Reserve leaves it at 0 cores, still at Lv1 and non-vanished.
     */
    @Test
    fun testNexusCoreMoveToZeroDoesNotVanish() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 2, p1ReserveSoul = 0)
        val nexus = TestFixtures.createNexus("fc_nexus_1", normalCores = 1, soulCores = 0)
        p1.field.add(nexus)

        val action = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "MOVE",
            detail = "Nexus -> Reserve (1 core)",
            moves = listOf(CoreMove(nexus.instanceId, "Reserve", nexus.name, "R", normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, action, messages)

        assertEquals(3, p1.reserve.normal)
        assertEquals(0, nexus.cores.total)
        assertEquals(1, nexus.level, "Nexus remains Lv1 with 0 cores")
        assertEquals(listOf(CardColor.GREEN), nexus.symbols)
        assertEquals(1, p1.field.size, "Nexus must NOT vanish when its cores become 0")
        assertTrue(p1.trashCards.isEmpty())
    }

    /**
     * 総合ルール 2-3-3, 2-10-3:
     * 0コアのネクサスがフィールドに存在する場合、そのシンボルは軽減シンボル計算に正常に計上される。
     */
    @Test
    fun testNexusProvidesSymbolReductionAtZeroCores() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 4, p1ReserveSoul = 0)
        val nexus = TestFixtures.createNexus("fc_nexus_1", normalCores = 0, soulCores = 0)
        p1.field.add(nexus)

        val counts = fieldSymbolCounts(p1)
        assertEquals(1, counts[CardColor.GREEN], "Field symbol counts must include the 0-core Nexus symbol")

        // Card with 1 green reduction symbol (26RSD03-001 フラッティ: cost 2, reduction [GREEN])
        val flatty = OFFICIAL_CARDS["26RSD03-001"]!!
        val reduction = reductionFor(flatty, p1)
        assertEquals(1, reduction, "Nexus at 0 cores must reduce summon cost by 1")

        val cost = summonCost(flatty, p1)
        assertEquals(1, cost, "Summon cost must be reduced from 2 to 1")
    }

    /**
     * 総合ルール 3-2-5-4, 2-10-3:
     * スピリットはLv1維持コストが1以上であり、コアが0個になるとLv0となりシンボルを失う。
     */
    @Test
    fun testSpiritDropsToLevelZeroWhenBelowLv1Cost() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 2, p1ReserveSoul = 0)
        // 26RSD03-001 フラッティ: lvCosts = listOf(1, 3)
        val spirit = TestFixtures.createSpirit("fc_spirit_1", normalCores = 0, soulCores = 0)
        p1.field.add(spirit)

        assertEquals(0, spirit.cores.total)
        assertEquals(0, spirit.level, "Spirit with 0 cores must drop to Lv0 (3-2-5-4)")
        assertTrue(spirit.symbols.isEmpty(), "Spirit at Lv0 must have 0 symbols (2-10-3)")
    }

    /**
     * 総合ルール 5-11-1, 10-2-2-1:
     * コアの移動によってコアがLv1未満になったスピリットは消滅し、trashCardsに送られる。
     */
    @Test
    fun testSpiritVanishesWhenBelowLv1CostViaMoveCore() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 1, p1ReserveSoul = 0)
        val spirit = TestFixtures.createSpirit("fc_spirit_1", normalCores = 1, soulCores = 0)
        p1.field.add(spirit)

        // Move the only core from spirit to Reserve
        val action = GameAction(
            index = 1,
            type = "MOVE_CORE",
            category = "MOVE",
            detail = "Spirit -> Reserve (1 core)",
            moves = listOf(CoreMove(spirit.instanceId, "Reserve", spirit.name, "R", normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, action, messages)

        assertEquals(2, p1.reserve.normal)
        assertTrue(p1.field.isEmpty(), "Spirit must be removed from field upon vanishing")
        assertEquals(1, p1.trashCards.size, "Vanished spirit must be added to trashCards")
        assertEquals(spirit.cardNo, p1.trashCards.first().cardNo)
        assertTrue(messages.any { it.contains("消滅") })
    }

    /**
     * 総合ルール 10-2-2-5 / 4-5-4 (最重要条項):
     * 「また、置かれていたコアはすべてリザーブに置きます。」
     * 「特に指示がない場合、フィールドにあるカードが他の領域に移動するとき、
     *   それに置かれていたコアはそのカードの持ち主のリザーブに置かれます。」
     *
     * Lv1維持コストが2のスピリットが1個のコアを持った状態で消滅した場合、
     * 残存コアはトラッシュではなくリザーブに戻らなければならない。
     */
    @Test
    fun testVanishReturnsResidualNormalCoresToReserve() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 2, p1ReserveSoul = 0)
        // High maintenance cost spirit: Lv1 requires 2 cores
        val highCostSpirit = TestFixtures.createCustomFieldCard(
            instanceId = "fc_high_cost",
            name = "HeavySpirit",
            category = CardCategory.SPIRIT,
            lvCosts = listOf(2, 4),
            normalCores = 1, // Only 1 core (below Lv1 maintenance cost of 2)
            soulCores = 0
        )
        p1.field.add(highCostSpirit)

        assertEquals(0, highCostSpirit.level, "Spirit with 1 core when lvCosts is [2, 4] is Lv0")

        val messages = mutableListOf<String>()
        resolveVanish(p1, messages)

        // 1. Card vanishes to trashCards
        assertTrue(p1.field.none { it.instanceId == highCostSpirit.instanceId })
        assertEquals(1, p1.trashCards.size)
        assertEquals("HeavySpirit", p1.trashCards.first().name)

        // 2. Residual core on vanished card returned to Reserve (NOT Trash!)
        assertEquals(3, p1.reserve.normal, "Residual core must return to Reserve (10-2-2-5, 4-5-4)")
        assertEquals(0, p1.trash.normal, "Trash normal cores must remain 0")
        assertEquals(0, highCostSpirit.cores.total, "Cores on vanished card object must be reset to 0")
    }

    /**
     * 総合ルール 10-2-2-5, 5-7-3:
     * 消滅したカード上のソウルコアもトラッシュやボイドではなくリザーブに戻る。
     */
    @Test
    fun testVanishReturnsResidualSoulCoreToReserve() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 1, p1ReserveSoul = 0)
        // Spirit requires 2 cores for Lv1, but holds only 1 soul core
        val highCostSpirit = TestFixtures.createCustomFieldCard(
            instanceId = "fc_soul_vanish",
            name = "SoulVanishSpirit",
            category = CardCategory.SPIRIT,
            lvCosts = listOf(2, 4),
            normalCores = 0,
            soulCores = 1
        )
        p1.field.add(highCostSpirit)

        assertEquals(0, highCostSpirit.level)

        val messages = mutableListOf<String>()
        resolveVanish(p1, messages)

        assertTrue(p1.field.isEmpty())
        assertEquals(1, p1.trashCards.size)
        // Soul core returned to Reserve!
        assertEquals(1, p1.reserve.soul, "Soul core must return to Reserve (10-2-2-5)")
        assertEquals(0, p1.trash.soul, "Trash soul core must remain 0")
    }

    /**
     * 総合ルール 12-2-1-1-2:
     * 《真界放》を持つカードはソウルコア1個が置かれている場合、Lv2維持コストを満たさなくてもLv2となる。
     */
    @Test
    fun testShinkaiLevelJumpWithSoulCore() {
        // 26RSD03-003 ラスヘッド: category = SPIRIT, lvCosts = listOf(1, 3), shinkai = true
        val shinkaiSpirit = TestFixtures.createSpirit(
            instanceId = "fc_shinkai",
            cardNo = "26RSD03-003",
            normalCores = 1,
            soulCores = 1 // Total 2 cores: less than lvCost 3 for Lv2
        )

        assertEquals(2, shinkaiSpirit.cores.total)
        assertEquals(1, shinkaiSpirit.cores.soul)
        assertTrue(shinkaiSpirit.shinkai)
        assertEquals(2, shinkaiSpirit.level, "Shinkai spirit with soul core must jump to Lv2 even with cores < 3 (12-2-1-1-2)")
    }

    /**
     * 総合ルール 12-2-1-1-2:
     * 《真界放》を持つカードであっても、ソウルコアがなければ通常のLvコスト判定（2コアではLv1のまま）に従う。
     */
    @Test
    fun testShinkaiWithoutSoulCoreFollowsNormalThreshold() {
        val shinkaiSpirit = TestFixtures.createSpirit(
            instanceId = "fc_shinkai_norm",
            cardNo = "26RSD03-003",
            normalCores = 2,
            soulCores = 0 // Total 2 cores, no soul core
        )

        assertEquals(2, shinkaiSpirit.cores.total)
        assertEquals(0, shinkaiSpirit.cores.soul)
        assertEquals(1, shinkaiSpirit.level, "Shinkai spirit without soul core remains Lv1 at 2 cores (needs 3 for Lv2)")
    }

    /**
     * 総合ルール 10-1-2:
     * 1手で複数のスピリットがLv1未満になった場合、すべて同時に消滅解決され全コアがリザーブに戻る。
     */
    @Test
    fun testMultipleCardsVanishingSimultaneously() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 1, p1ReserveSoul = 0)
        val s1 = TestFixtures.createCustomFieldCard("s1", "Spirit1", CardCategory.SPIRIT, listOf(2), normalCores = 1)
        val s2 = TestFixtures.createCustomFieldCard("s2", "Spirit2", CardCategory.SPIRIT, listOf(2), normalCores = 1)
        p1.field.add(s1)
        p1.field.add(s2)

        val messages = mutableListOf<String>()
        resolveVanish(p1, messages)

        assertTrue(p1.field.isEmpty())
        assertEquals(2, p1.trashCards.size)
        // 1 initial reserve + 1 from s1 + 1 from s2 = 3 cores in reserve
        assertEquals(3, p1.reserve.normal)
        assertEquals(0, p1.trash.normal)
    }

    /**
     * 総合ルール 5-11-2, 5-10-2:
     * 消滅は破壊とは異なるため、破壊関連のフラグや処理は一切トリガーされない。
     */
    @Test
    fun testVanishDoesNotTriggerDestructionEffects() {
        val (state, p1) = TestFixtures.createTestState()
        val spirit = TestFixtures.createSpirit("fc_test", normalCores = 0, soulCores = 0)
        p1.field.add(spirit)

        val messages = mutableListOf<String>()
        resolveVanish(p1, messages)

        // Verifies resolveVanish only performs rule-based vanish into trashCards, not destroySpirit
        assertTrue(messages.all { !it.contains("破壊") })
        assertTrue(messages.any { it.contains("消滅") })
    }
}
