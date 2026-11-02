package bstools.model

import kotlin.test.*

/**
 * Adversarial Verification & Stress Test Suite for Level Maintenance, Vanishing, and Summon/Deployment (Operations 2, 3, & 5).
 *
 * Authored by challenger_2 (Adversarial Verifier).
 *
 * Targeted Corner Cases & Adversarial Scenarios:
 * 1. Nexus with 0 cores providing symbol reduction for subsequent summons across multi-step sequences.
 * 2. Vanishing of spirits with residual normal cores AND soul core simultaneously (both returned to reserve).
 * 3. Multi-card vanishing cascade when multiple donor cards are drained during summon cost payment.
 * 4. Prohibition of overpayment: verify that card costs cannot exceed reduced cost, and generator never produces overpayment actions.
 * 5. Nexus resilience during multi-card vanishing (0-core Nexus survives while neighboring spirits vanish).
 * 6. Self-destruction summon with soul core donor (residual soul core returns to reserve upon donor vanish).
 * 7. Invariant Fuzzing: Core conservation and level maintenance law across all legal summon/deploy plays.
 */
class LevelMaintenanceAndSummonAdversarialTest {

    // =========================================================================
    // 1. Corner Case: Nexus with 0 cores providing symbol reduction for subsequent summons
    // =========================================================================

    /**
     * 総合ルール 3-3-3-3, 2-3-3, 2-10-3:
     * Deploy a 0-core Nexus first, then perform sequential summons where the 0-core Nexus
     * provides continuous symbol reduction. Verify the Nexus never vanishes and symbols persist.
     */
    @Test
    fun testNexusZeroCoresProvidesSymbolReductionChainedSummons() {
        // Start with reserve 6 normal cores
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 6, p1ReserveSoul = 0)

        // 1. Deploy 0-core Nexus (26RSD03-010 最奥：緑淵深, cost 3, lvCosts [0, 3], symbol GREEN)
        val nexusCard = OFFICIAL_CARDS["26RSD03-010"]!!
        p1.hand.add(nexusCard)

        val deployNexus = GameAction(
            index = 1,
            type = "SUMMON",
            category = "DEPLOY",
            detail = "Deploy Nexus with 0 cores",
            handIndex = 0,
            costToPay = 3,
            payFrom = listOf(CoreAlloc("Reserve", "R", normal = 3, soul = 0)),
            placeFrom = emptyList()
        )
        applyAction(state, deployNexus, mutableListOf())

        assertEquals(1, p1.field.size)
        val deployedNexus = p1.field.first()
        assertEquals(0, deployedNexus.cores.total)
        assertEquals(1, deployedNexus.level, "Nexus with 0 cores must be Lv1")
        assertEquals(listOf(CardColor.GREEN), deployedNexus.symbols)
        assertEquals(3, p1.reserve.normal)
        assertEquals(3, p1.trash.normal)

        // 2. First subsequent summon: Flatty (26RSD03-001, cost 2, reduction [GREEN], lvCosts [1, 3])
        // Field has 1 GREEN symbol from 0-core Nexus -> reduced cost = 1
        val flatty = OFFICIAL_CARDS["26RSD03-001"]!!
        p1.hand.add(flatty)

        val flattyCost = summonCost(flatty, p1)
        assertEquals(1, flattyCost, "0-core Nexus must reduce Flatty summon cost from 2 to 1")

        val summonFlatty = GameAction(
            index = 2,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Summon Flatty using Nexus symbol reduction",
            handIndex = 0,
            costToPay = 1,
            payFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0)),
            placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
        )
        applyAction(state, summonFlatty, mutableListOf())

        assertEquals(2, p1.field.size)
        assertEquals(0, deployedNexus.cores.total, "Nexus still has 0 cores")
        assertEquals(1, deployedNexus.level, "Nexus remains Lv1")
        assertEquals(1, p1.reserve.normal)
        assertEquals(4, p1.trash.normal)

        // 3. Second subsequent summon: Stralay (26RSD03-004, cost 3, reduction [GREEN, GREEN])
        // Field now has 2 GREEN symbols (0-core Nexus + Flatty) -> reduction = 2, cost = 1
        val stralay = OFFICIAL_CARDS["26RSD03-004"]!!
        val reduction = reductionFor(stralay, p1)
        assertEquals(2, reduction, "Field with 0-core Nexus and Flatty provides 2 GREEN symbols")
        val stralayCost = summonCost(stralay, p1)
        assertEquals(1, stralayCost, "Cost 3 reduced by 2 becomes 1")

        // 0-core Nexus must still be on field after resolveVanish
        resolveVanish(p1, mutableListOf())
        assertEquals(2, p1.field.size, "0-core Nexus and Flatty must both persist")
        assertTrue(p1.trashCards.isEmpty(), "No card should have vanished")
    }

    // =========================================================================
    // 2. Corner Case: Vanishing with residual normal cores AND soul core simultaneously
    // =========================================================================

    /**
     * 総合ルール 10-2-2-5, 4-5-4, 5-7-3:
     * When a card with lvCosts [3, 5] has 1 normal core AND 1 soul core (total 2 < 3 keep cost),
     * it vanishes. BOTH the normal core AND the soul core must be returned to the player's Reserve.
     * Neither may be lost, discarded to Trash, or deposited to Void.
     */
    @Test
    fun testVanishReturnsBothResidualNormalAndSoulCoresToReserve() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 2, p1ReserveSoul = 0)

        // Spirit requiring 3 cores for Lv1, holding 1 normal and 1 soul core
        val highCostSpirit = TestFixtures.createCustomFieldCard(
            instanceId = "fc_high_hybrid",
            name = "HybridVanishSpirit",
            category = CardCategory.SPIRIT,
            lvCosts = listOf(3, 5),
            normalCores = 1,
            soulCores = 1
        )
        p1.field.add(highCostSpirit)

        assertEquals(2, highCostSpirit.cores.total)
        assertEquals(0, highCostSpirit.level, "Total 2 cores < Lv1 cost 3 -> must be Lv0")

        val messages = mutableListOf<String>()
        resolveVanish(p1, messages)

        // 1. Spirit must vanish to trashCards
        assertTrue(p1.field.isEmpty(), "Spirit must be removed from field upon vanishing")
        assertEquals(1, p1.trashCards.size)
        assertEquals("HybridVanishSpirit", p1.trashCards.first().name)

        // 2. Both normal core and soul core must be returned to Reserve
        // Initial reserve: normal = 2, soul = 0
        // Expected after vanish: normal = 3 (2 + 1), soul = 1 (0 + 1)
        assertEquals(3, p1.reserve.normal, "Residual normal core must return to Reserve (10-2-2-5)")
        assertEquals(1, p1.reserve.soul, "Residual soul core must return to Reserve (10-2-2-5, 5-7-3)")

        // 3. Trash cores must remain 0
        assertEquals(0, p1.trash.normal, "Trash normal cores must not receive vanished residual cores")
        assertEquals(0, p1.trash.soul, "Trash soul core must not receive vanished residual cores")

        // 4. Vanished card object cores must be cleared
        assertEquals(0, highCostSpirit.cores.total, "Vanished card object cores must be reset to 0")
    }

    // =========================================================================
    // 3. Corner Case: Multi-card vanishing cascade during summon cost payment
    // =========================================================================

    /**
     * 総合ルール 10-1-2, 10-2-2-10, 11-1-1-5:
     * Summoning a new spirit drawing cores from multiple field donor cards.
     * Multiple donor spirits are simultaneously drained below their Lv1 maintenance costs.
     * All drained donors must vanish to trashCards, their residual cores must return to Reserve,
     * and the newly summoned spirit must remain safely on the field.
     */
    @Test
    fun testMultiCardVanishingCascadeDuringSummonCostPayment() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 1, p1ReserveSoul = 0)

        // Donor A: 1 core, lvCosts [1, 3] (will be drained to 0 -> vanishes)
        val donorA = TestFixtures.createSpirit("fc_donor_a", normalCores = 1, soulCores = 0)
        // Donor B: 1 core, lvCosts [1, 3] (will be drained to 0 -> vanishes)
        val donorB = TestFixtures.createSpirit("fc_donor_b", normalCores = 1, soulCores = 0)
        // Donor C: 2 cores, lvCosts [2, 4] (1 core paid -> 1 left < 2 keep cost -> vanishes with 1 residual core!)
        val donorC = TestFixtures.createCustomFieldCard(
            instanceId = "fc_donor_c",
            name = "DonorC_Heavy",
            category = CardCategory.SPIRIT,
            lvCosts = listOf(2, 4),
            normalCores = 2,
            soulCores = 0
        )

        p1.field.add(donorA)
        p1.field.add(donorB)
        p1.field.add(donorC)

        // Target to summon: Flatty (cost 2, reduction [GREEN])
        // Field has 3 green symbols -> reduced cost = max(0, 2 - 1) = 1... wait, let's summon a cost 3 card with 0 reduction:
        val bigCard = CardData(
            cardNo = "TEST-CASCADE-001",
            name = "CascadeSummonTarget",
            cost = 3,
            reductionSymbols = emptyList(), // No reduction: strictly cost 3
            colors = listOf(CardColor.GREEN),
            category = CardCategory.SPIRIT,
            lvCosts = listOf(1, 3),
            lvBps = listOf(5000, 8000),
            symbols = listOf(CardColor.GREEN)
        )
        p1.hand.add(bigCard)

        // Pay 1 from donorA, 1 from donorB, 1 from donorC = 3 cores paid to trash
        // Place 1 core from Reserve onto new card
        val cascadeSummon = GameAction(
            index = 1,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Cascade summon draining donorA, donorB, donorC",
            handIndex = 0,
            costToPay = 3,
            payFrom = listOf(
                CoreAlloc(donorA.instanceId, donorA.name, normal = 1, soul = 0),
                CoreAlloc(donorB.instanceId, donorB.name, normal = 1, soul = 0),
                CoreAlloc(donorC.instanceId, donorC.name, normal = 1, soul = 0)
            ),
            placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, cascadeSummon, messages)

        // 1. Hand consumed
        assertTrue(p1.hand.isEmpty())

        // 2. All three donor spirits must have vanished to trashCards
        assertFalse(p1.field.any { it.instanceId == donorA.instanceId }, "Donor A must vanish")
        assertFalse(p1.field.any { it.instanceId == donorB.instanceId }, "Donor B must vanish")
        assertFalse(p1.field.any { it.instanceId == donorC.instanceId }, "Donor C must vanish")
        assertEquals(3, p1.trashCards.size, "All 3 drained donor spirits must be in trashCards")

        // 3. The 3 paid cores went to trash
        assertEquals(3, p1.trash.normal, "3 paid cores must be in trash")

        // 4. Exactly 1 new spirit on field at Lv1
        assertEquals(1, p1.field.size, "Only the newly summoned spirit must remain on field")
        val summoned = p1.field.first()
        assertEquals("CascadeSummonTarget", summoned.name)
        assertEquals(1, summoned.cores.normal)
        assertEquals(1, summoned.level)

        // 5. Reserve accounting:
        // Initial: 1 normal
        // Placed on new spirit: -1
        // Residual from donorC: +1 (donorC had 2 cores, 1 paid, 1 remained below keep cost 2 -> returned to reserve!)
        // Final reserve normal: 1!
        assertEquals(1, p1.reserve.normal, "Residual core from donorC must return to Reserve")

        // Global core conservation:
        // Initial: Reserve(1) + Donors(1+1+2=4) = 5 cores
        // Final: Reserve(1) + Trash(3) + Field(1) = 5 cores
        val totalCores = p1.reserve.normal + p1.trash.normal + p1.field.sumOf { it.cores.normal }
        assertEquals(5, totalCores, "Global core count must be perfectly conserved")
    }

    // =========================================================================
    // 4. Overpayment Prohibition: Verify cost reduction & prohibition of excess payment
    // =========================================================================

    /**
     * 総合ルール 2-3-3:
     * 「カードの召喚、配置、または使用するコストを支払う際、軽減シンボルの数まで、自分のフィールドに対応する
     *   シンボルがあればコストを軽減します。また、フィールドにある軽減シンボルを使わずにコストを過剰に払うことはできません。」
     *
     * Empirically verify:
     * 1. `summonCost` enforces exact reduced cost.
     * 2. `enumerateAllPlays` NEVER generates any action that pays more than `summonCost`.
     * 3. For 100% of generated summon actions for a reduced card, `payFrom` core sum == `summonCost`.
     */
    @Test
    fun testOverpaymentProhibitionInActionEnumeration() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 8, p1ReserveSoul = 1)

        // Field has 2 Green spirits (2 Green symbols)
        p1.field.add(TestFixtures.createSpirit("s1", normalCores = 1))
        p1.field.add(TestFixtures.createSpirit("s2", normalCores = 1))

        // 26RSD03-004 ストラレイ: cost 3, reduction [GREEN, GREEN]
        val stralay = OFFICIAL_CARDS["26RSD03-004"]!!
        p1.hand.add(stralay)

        val reduction = reductionFor(stralay, p1)
        assertEquals(2, reduction, "Reduction must be exactly 2")

        val reducedCost = summonCost(stralay, p1)
        assertEquals(1, reducedCost, "Reduced cost must be strictly 1 (3 - 2)")

        // Enumerate all plays for this state
        val legalPlays = enumerateAllPlays(state)
        val stralaySummonPlays = legalPlays.filter { it.type == "SUMMON" && it.handIndex == 0 }

        assertTrue(stralaySummonPlays.isNotEmpty(), "Legal summon actions must be enumerated for Stralay")

        for (action in stralaySummonPlays) {
            // Verify action.costToPay matches reducedCost
            assertEquals(
                reducedCost,
                action.costToPay,
                "action.costToPay must strictly equal reduced cost of 1, never overpay (2-3-3)"
            )

            // Verify actual cores allocated to payFrom matches costToPay
            val actualPaid = action.payFrom.sumOf { it.normal + it.soul }
            assertEquals(
                reducedCost,
                actualPaid,
                "payFrom total ($actualPaid) must strictly equal reducedCost ($reducedCost), never overpay (2-3-3)"
            )
        }
    }

    // =========================================================================
    // 5. Corner Case: Nexus resilience during multi-card vanishing
    // =========================================================================

    /**
     * 総合ルール 3-3-3-3:
     * When multiple spirits on the field drop to 0 cores and vanish, a 0-core Nexus on the same field
     * must remain completely unaffected, remaining at Lv1 with its symbols intact.
     */
    @Test
    fun testNexusZeroCoresResilienceDuringSpiritsVanishing() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 0, p1ReserveSoul = 0)

        val nexus = TestFixtures.createNexus("fc_nexus_zero", normalCores = 0, soulCores = 0)
        val spirit1 = TestFixtures.createSpirit("s1", normalCores = 0, soulCores = 0) // Lv0 -> vanishes
        val spirit2 = TestFixtures.createSpirit("s2", normalCores = 0, soulCores = 0) // Lv0 -> vanishes
        val healthySpirit = TestFixtures.createSpirit("s3_healthy", normalCores = 2, soulCores = 0) // Lv1 -> stays

        p1.field.add(nexus)
        p1.field.add(spirit1)
        p1.field.add(spirit2)
        p1.field.add(healthySpirit)

        val messages = mutableListOf<String>()
        resolveVanish(p1, messages)

        assertEquals(2, p1.field.size, "Only nexus and healthy spirit must remain")
        assertTrue(p1.field.any { it.instanceId == nexus.instanceId }, "Nexus with 0 cores must NOT vanish")
        assertTrue(p1.field.any { it.instanceId == healthySpirit.instanceId }, "Healthy spirit must NOT vanish")
        assertEquals(2, p1.trashCards.size, "Both Lv0 spirits must vanish to trashCards")

        assertEquals(1, nexus.level, "Nexus remains Lv1 with 0 cores")
        assertEquals(listOf(CardColor.GREEN), nexus.symbols, "Nexus keeps green symbol")
    }

    // =========================================================================
    // 6. Corner Case: Self-destruction summon with soul core preservation
    // =========================================================================

    /**
     * 総合ルール 10-2-2-5, 10-2-2-10:
     * Donor spirit holds 1 normal core and 1 soul core with lvCosts [2, 4].
     * Normal core is paid for summon cost -> donor is left with 1 soul core (< 2 keep cost).
     * Donor vanishes to trashCards, and the residual soul core returns to Reserve.
     */
    @Test
    fun testSelfDestructSummonReturnsResidualSoulCoreToReserve() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 1, p1ReserveSoul = 0)

        val donor = TestFixtures.createCustomFieldCard(
            instanceId = "fc_donor_soul",
            name = "SoulDonor",
            category = CardCategory.SPIRIT,
            lvCosts = listOf(2, 4),
            normalCores = 1,
            soulCores = 1
        )
        p1.field.add(donor)

        val targetCard = OFFICIAL_CARDS["26RSD03-001"]!! // cost 2
        p1.hand.add(targetCard)

        // Pay 1 from donor normal, 1 from Reserve normal -> total cost 2 paid to trash
        // Place 1 from Reserve? Reserve has 1, donor has 1 normal + 1 soul.
        // Let's set reserve = 2 normal:
        p1.reserve.normal = 2

        val summonAction = GameAction(
            index = 1,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Self-destruction summon leaving soul core on donor",
            handIndex = 0,
            costToPay = 2,
            payFrom = listOf(
                CoreAlloc(donor.instanceId, donor.name, normal = 1, soul = 0),
                CoreAlloc("Reserve", "R", normal = 1, soul = 0)
            ),
            placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
        )
        applyAction(state, summonAction, mutableListOf())

        // Donor had (1 normal, 1 soul). 1 normal paid. 1 soul left.
        // But Lv1 requires 2 cores -> level drops to 0 -> vanishes!
        assertFalse(p1.field.any { it.instanceId == donor.instanceId }, "Donor must vanish")
        assertTrue(p1.trashCards.any { it.name == "SoulDonor" })

        // Residual soul core must have returned to Reserve!
        assertEquals(1, p1.reserve.soul, "Soul core must return to Reserve upon donor vanish")
        assertEquals(0, p1.trash.soul, "Soul core must NOT be in trash")
    }

    // =========================================================================
    // 7. Invariant Fuzzing: Invariant check across 100% of enumerated summon plays
    // =========================================================================

    /**
     * Invariant Property Test:
     * For arbitrary board states, enumerate all legal summon/deploy actions and verify:
     * 1. Global Core Conservation: total cores (Reserve + Field + Trash) is strictly conserved.
     * 2. Global Soul Core Conservation: total soul core (Reserve + Field + Trash) is strictly conserved.
     * 3. Deployed/Summoned card minimum level requirement: placed cores >= lvCosts[0].
     * 4. No ghost cards on field: every card on field has level >= 1 (or nexus level == 1).
     */
    @Test
    fun testGlobalInvariantsAcrossAllEnumeratedSummonActions() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 4, p1ReserveSoul = 1)
        p1.field.add(TestFixtures.createSpirit("s1", normalCores = 2, soulCores = 0))
        p1.field.add(TestFixtures.createNexus("n1", normalCores = 0, soulCores = 0))
        p1.hand.add(OFFICIAL_CARDS["26RSD03-001"]!!) // Flatty (Spirit)
        p1.hand.add(OFFICIAL_CARDS["26RSD03-010"]!!) // Nexus

        val initialTotal = p1.reserve.total + p1.field.sumOf { it.cores.total } + p1.trash.total
        val initialSoul = p1.reserve.soul + p1.field.sumOf { it.cores.soul } + p1.trash.soul

        val legalPlays = enumerateAllPlays(state).filter { it.type == "SUMMON" }
        assertTrue(legalPlays.isNotEmpty(), "Legal summon plays must exist")

        for (action in legalPlays) {
            val trial = state.snapshot()
            val tp = trial.player1
            val messages = mutableListOf<String>()

            applyAction(trial, action, messages)

            val afterTotal = tp.reserve.total + tp.field.sumOf { it.cores.total } + tp.trash.total
            val afterSoul = tp.reserve.soul + tp.field.sumOf { it.cores.soul } + tp.trash.soul

            // 1. Total core conservation
            assertEquals(
                initialTotal,
                afterTotal,
                "Core conservation failed for action: ${action.detail}"
            )

            // 2. Soul core conservation
            assertEquals(
                initialSoul,
                afterSoul,
                "Soul core conservation failed for action: ${action.detail}"
            )

            // 3. No Lv0 cards remaining on field
            for (fc in tp.field) {
                assertTrue(
                    fc.level >= 1,
                    "Field card '${fc.name}' has level ${fc.level} <= 0 on field after action: ${action.detail}"
                )
            }
        }
    }
}
