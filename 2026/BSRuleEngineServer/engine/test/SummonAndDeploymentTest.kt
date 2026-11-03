package bstools.model

import kotlin.test.*

/**
 * Verification Test Suite for Operation 5 (Summon & Nexus Deployment).
 *
 * Verifies compliance against:
 * - バトルスピリッツ スタンダード 総合ルール Ver.1.0 (2026/04/10):
 *   - 11-1-1 / 11-2-1: スピリットの召喚手順・ネクサスの配置手順
 *   - 11-1-1-5 / 11-2-1-5: コスト支払いはフィールドまたはリザーブからトラッシュへ移動。過剰支払いの禁止
 *   - 11-1-1-6 / 11-2-1-6: 最低LvのLvコスト以上のコアをフィールドまたはリザーブからカードに置く
 *   - 2-3-3: 軽減シンボルと過剰支払いの禁止（「フィールドにある軽減シンボルを使わずにコストを過剰に払うことはできません」）
 *   - 3-3-3-3: ネクサスの最低Lvコストは0（0個配置で配置可能）
 *   - 10-2-2-10: 自壊召喚（召喚・配置に伴うコア拠出によってドナーが消滅するルール処理）
 */
class SummonAndDeploymentTest {

    /**
     * 総合ルール 11-2-1-6, 3-3-3-3:
     * ネクサスは最低Lvコストが0であるため、配置時に置くコアが0個であっても正常に配置され、Lv1としてフィールドに出る。
     */
    @Test
    fun testNexusDeploymentWithZeroCoresPlaced() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 5, p1ReserveSoul = 0)
        // 26RSD03-010 最奥：緑淵深 (cost = 3, lvCosts = listOf(0, 3))
        val nexusCard = OFFICIAL_CARDS["26RSD03-010"]!!
        p1.hand.add(nexusCard)

        assertEquals(0, requiredCoresOnSummon(nexusCard), "Required cores on deploy for nexus must be 0 (11-2-1-6, 3-3-3-3)")

        val deployAction = GameAction(
            index = 1,
            type = "SUMMON",
            category = "DEPLOY",
            detail = "Deploy Nexus with 0 cores placed",
            handIndex = 0,
            costToPay = 3,
            payFrom = listOf(CoreAlloc("Reserve", "R", normal = 3, soul = 0)),
            placeFrom = emptyList() // 0 cores placed!
        )
        val messages = mutableListOf<String>()
        applyAction(state, deployAction, messages)

        // Hand card consumed
        assertTrue(p1.hand.isEmpty())
        // Cost moved to trash
        assertEquals(3, p1.trash.normal, "Cost cores must be placed in trash (11-2-1-5)")
        assertEquals(2, p1.reserve.normal)

        // Nexus placed on field with 0 cores and level 1
        assertEquals(1, p1.field.size)
        val deployedNexus = p1.field.first()
        assertEquals(nexusCard.cardNo, deployedNexus.cardNo)
        assertEquals(0, deployedNexus.cores.total)
        assertEquals(1, deployedNexus.level, "Deployed Nexus with 0 cores must be Lv1")
        assertEquals(listOf(CardColor.GREEN), deployedNexus.symbols)
        assertTrue(p1.trashCards.isEmpty(), "0-core Nexus must NOT vanish")
    }

    /**
     * Deploy Nexus placing 3 cores to enter directly at Lv2.
     */
    @Test
    fun testNexusDeploymentWithPlacementCores() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 6, p1ReserveSoul = 0)
        val nexusCard = OFFICIAL_CARDS["26RSD03-010"]!! // lvCosts: [0, 3]
        p1.hand.add(nexusCard)

        val deployAction = GameAction(
            index = 1,
            type = "SUMMON",
            category = "DEPLOY",
            detail = "Deploy Nexus with 3 cores placed (Lv2)",
            handIndex = 0,
            costToPay = 3,
            payFrom = listOf(CoreAlloc("Reserve", "R", normal = 3, soul = 0)),
            placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 3, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, deployAction, messages)

        assertEquals(0, p1.reserve.normal)
        assertEquals(3, p1.trash.normal)
        val deployedNexus = p1.field.first()
        assertEquals(3, deployedNexus.cores.normal)
        assertEquals(2, deployedNexus.level, "Nexus with 3 cores must be Lv2")
    }

    /**
     * 総合ルール 11-1-1-6, 11-2-1-6:
     * requiredCoresOnSummon: スピリットはLv1維持コスト（1以上）が必須、ネクサスは0。
     */
    @Test
    fun testSpiritSummonRequiresMinimumLv1Core() {
        val spiritCard = OFFICIAL_CARDS["26RSD03-001"]!! // フラッティ
        val nexusCard = OFFICIAL_CARDS["26RSD03-010"]!! // 最奥：緑淵深

        assertEquals(1, requiredCoresOnSummon(spiritCard))
        assertEquals(0, requiredCoresOnSummon(nexusCard))
    }

    /**
     * 総合ルール 11-1-1-5:
     * 召喚コストのコアはトラッシュに移動する。
     */
    @Test
    fun testSummonCostPaidToTrash() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 4, p1ReserveSoul = 0)
        val spiritCard = OFFICIAL_CARDS["26RSD03-001"]!! // cost 2
        p1.hand.add(spiritCard)

        val summonAction = GameAction(
            index = 1,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Summon Flatty (cost 2, place 1)",
            handIndex = 0,
            costToPay = 2,
            payFrom = listOf(CoreAlloc("Reserve", "R", normal = 2, soul = 0)),
            placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, summonAction, messages)

        assertEquals(1, p1.reserve.normal)
        assertEquals(2, p1.trash.normal, "2 cores paid as summon cost must be deposited into trash (11-1-1-5)")
        assertEquals(1, p1.field.first().cores.normal)
    }

    /**
     * 総合ルール 11-1-1-5:
     * 召喚コストはフィールドのカード上のコアから支払うことができる。
     */
    @Test
    fun testSummonPaymentFromFieldCards() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 1, p1ReserveSoul = 0)
        // Existing donor spirit on field with 3 cores (surplus cores)
        val donor = TestFixtures.createSpirit("fc_donor", normalCores = 3, soulCores = 0)
        p1.field.add(donor)

        val spiritCard = OFFICIAL_CARDS["26RSD03-001"]!! // cost 2
        p1.hand.add(spiritCard)

        // Pay 2 cores directly from donor spirit on field
        val summonAction = GameAction(
            index = 1,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Pay 2 from donor spirit",
            handIndex = 0,
            costToPay = 2,
            payFrom = listOf(CoreAlloc(donor.instanceId, donor.name, normal = 2, soul = 0)),
            placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, summonAction, messages)

        assertEquals(0, p1.reserve.normal)
        assertEquals(2, p1.trash.normal)
        // Donor retains 1 core -> remains at Lv1
        assertEquals(1, donor.cores.normal)
        assertEquals(1, donor.level)
        assertEquals(2, p1.field.size)
    }

    /**
     * 総合ルール 11-1-1-6:
     * 召喚時にカードの上に置くコアは、フィールドのカードから供給することができる。
     */
    @Test
    fun testSummonPlacementFromFieldCards() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 3, p1ReserveSoul = 0)
        val donor = TestFixtures.createSpirit("fc_donor", normalCores = 3, soulCores = 0)
        p1.field.add(donor)

        val spiritCard = OFFICIAL_CARDS["26RSD03-001"]!! // cost 2
        p1.hand.add(spiritCard)

        // Pay 2 from reserve, place 1 from donor
        val summonAction = GameAction(
            index = 1,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Pay from reserve, place from donor",
            handIndex = 0,
            costToPay = 2,
            payFrom = listOf(CoreAlloc("Reserve", "R", normal = 2, soul = 0)),
            placeFrom = listOf(CoreAlloc(donor.instanceId, donor.name, normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, summonAction, messages)

        assertEquals(1, p1.reserve.normal)
        assertEquals(2, p1.trash.normal)
        assertEquals(2, donor.cores.normal)
        assertEquals(2, p1.field.size)
        val newSpirit = p1.field.last()
        assertEquals(1, newSpirit.cores.normal)
    }

    /**
     * 総合ルール 10-2-2-10 & 公式Q&A:
     * 自壊召喚（召喚・配置に伴うコア拠出によってドナーがLv1未満となり消滅するルール処理）。
     */
    @Test
    fun testSelfDestructionSummon() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 0, p1ReserveSoul = 0)
        // Donor spirit has 2 cores (lvCosts: [1, 3])
        val donor = TestFixtures.createSpirit("fc_donor_sac", normalCores = 2, soulCores = 0)
        p1.field.add(donor)

        val newSpiritCard = OFFICIAL_CARDS["26RSD03-001"]!! // cost 2
        p1.hand.add(newSpiritCard)

        // Pay 1 from donor, place 1 from donor -> donor cores become 0 -> vanishes!
        val selfDestructSummon = GameAction(
            index = 1,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Self-destruction summon taking all cores from donor",
            handIndex = 0,
            costToPay = 1, // reduced or specified
            payFrom = listOf(CoreAlloc(donor.instanceId, donor.name, normal = 1, soul = 0)),
            placeFrom = listOf(CoreAlloc(donor.instanceId, donor.name, normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, selfDestructSummon, messages)

        // Donor must have vanished to trashCards
        assertFalse(p1.field.any { it.instanceId == donor.instanceId }, "Donor spirit must vanish from field")
        assertTrue(p1.trashCards.any { it.cardNo == donor.cardNo }, "Donor spirit must be placed in trashCards")
        assertEquals(1, p1.trash.normal, "Paid core is in trash")

        // New spirit must be on field
        assertEquals(1, p1.field.size)
        val newCard = p1.field.first()
        assertEquals(newSpiritCard.cardNo, newCard.cardNo)
        assertEquals(1, newCard.cores.normal)
        assertEquals(1, newCard.level)
    }

    /**
     * 総合ルール 10-2-2-5, 10-2-2-10:
     * 自壊召喚時に、Lv1維持コスト2のドナーから2個を支払いトラッシュへ送り、
     * 残存1個（Lv1維持コスト未満）になった場合、ドナーは消滅し残存1個はリザーブに戻る。
     */
    @Test
    fun testSelfDestructionSummonReturnsResidualCoreToReserve() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 1, p1ReserveSoul = 0)
        val donor = TestFixtures.createCustomFieldCard(
            instanceId = "fc_donor_heavy",
            name = "HeavyDonor",
            category = CardCategory.SPIRIT,
            lvCosts = listOf(2, 4), // Lv1 requires 2 cores
            normalCores = 3,
            soulCores = 0
        )
        p1.field.add(donor)

        val spiritCard = OFFICIAL_CARDS["26RSD03-001"]!! // cost 2
        p1.hand.add(spiritCard)

        // Pay 2 cores from donor (sent to trash), place 1 from reserve
        // Donor is left with 1 core (< 2 keep cost) -> vanishes, residual core goes to Reserve!
        val action = GameAction(
            index = 1,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Pay 2 from donor",
            handIndex = 0,
            costToPay = 2,
            payFrom = listOf(CoreAlloc(donor.instanceId, donor.name, normal = 2, soul = 0)),
            placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, action, messages)

        // Donor vanished
        assertFalse(p1.field.any { it.instanceId == donor.instanceId })
        assertTrue(p1.trashCards.any { it.name == "HeavyDonor" })

        // 2 cores from donor went to trash
        assertEquals(2, p1.trash.normal)
        // 1 core placed on new spirit from Reserve
        assertEquals(1, p1.field.first().cores.normal)
        // Residual 1 core on donor returned to Reserve!
        // Initial reserve was 1, took 1 for placement (0), plus 1 residual from vanished donor = 1 in reserve!
        assertEquals(1, p1.reserve.normal, "Residual core on vanished donor must return to Reserve")
    }

    /**
     * 総合ルール 2-3-3, 11-1-1-4-1:
     * コスト軽減の計算（フィールドにある同色シンボル1つにつきコスト1軽減）。
     */
    @Test
    fun testSymbolReductionCalculation() {
        val (state, p1) = TestFixtures.createTestState()
        // 2 Green spirits on field
        p1.field.add(TestFixtures.createSpirit("s1", normalCores = 1))
        p1.field.add(TestFixtures.createSpirit("s2", normalCores = 1))

        // 26RSD03-004 ストラレイ: cost 3, reduction: [GREEN, GREEN]
        val stralay = OFFICIAL_CARDS["26RSD03-004"]!!
        val reduction = reductionFor(stralay, p1)
        assertEquals(2, reduction, "Field with 2 green symbols provides 2 cost reduction")

        val finalCost = summonCost(stralay, p1)
        assertEquals(1, finalCost, "Cost 3 reduced by 2 becomes 1")
    }

    /**
     * 総合ルール 2-3-3:
     * 「フィールドにある軽減シンボルを使わずにコストを過剰に払うことはできません。」
     */
    @Test
    fun testSymbolReductionDoesNotOverpay() {
        val (state, p1) = TestFixtures.createTestState()
        p1.field.add(TestFixtures.createSpirit("s1", normalCores = 1))
        p1.field.add(TestFixtures.createSpirit("s2", normalCores = 1))

        val stralay = OFFICIAL_CARDS["26RSD03-004"]!! // cost 3, reduction [GREEN, GREEN]
        // summonCost enforces maxOf(0, card.cost - reductionFor(card, p))
        val cost = summonCost(stralay, p1)
        assertEquals(1, cost, "Excess payment is not allowed; summonCost must strictly be 1")
    }

    /**
     * 総合ルール 11-1-1-6:
     * 「最低LvのLvコスト以上のコアを置く」
     * placementOptions は最低Lvコストから余剰コアに応じた複数選択肢を列挙する。
     */
    @Test
    fun testPlacementOptionsGeneratesMultiLevelChoices() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 6, p1ReserveSoul = 0)
        val spiritCard = OFFICIAL_CARDS["26RSD03-001"]!! // lvCosts: [1, 3]
        val sources = coreSources(p1)

        val options = placementOptions(spiritCard, cost = 2, sources = sources)
        // Total available = 6. Cost = 2. Remaining = 4.
        // Required on summon = 1.
        // Allowed placements = 1..4
        assertEquals(listOf(1, 2, 3, 4), options)
    }

    /**
     * 総合ルール 11-1-1:
     * コアが不足している場合、召喚は実行されず局面は変化しない。
     */
    @Test
    fun testInsufficientCoresFailsSummon() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 1, p1ReserveSoul = 0)
        val spiritCard = OFFICIAL_CARDS["26RSD03-001"]!! // cost 2
        p1.hand.add(spiritCard)

        // Attempt summon requiring cost 2 when reserve only has 1
        val invalidSummon = GameAction(
            index = 1,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Summon with insufficient cores",
            handIndex = 0,
            costToPay = 2,
            payFrom = listOf(CoreAlloc("Reserve", "R", normal = 2, soul = 0)),
            placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, invalidSummon, messages)

        // State remains unchanged
        assertEquals(1, p1.hand.size)
        assertEquals(1, p1.reserve.normal)
        assertEquals(0, p1.trash.normal)
        assertTrue(p1.field.isEmpty())
        assertTrue(messages.any { it.contains("コアが不足") })
    }

    /**
     * 総合ルール 10-2-2-10:
     * 複数のスピリットが場に存在する場合、各スピリットを消滅させて召喚する選択肢が
     * distinctBy で誤って単一にまとめられることなく複数列挙される。
     */
    @Test
    fun testLevelPlaysPreservesVanishingOptionsAcrossMultipleSpirits() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 0, p1ReserveSoul = 0, step = Step.MAIN_2)
        // Spirit A (1 soul core)
        val spiritA = TestFixtures.createSpirit("fc_spirit_a", normalCores = 0, soulCores = 1)
        // Spirit B (2 normal cores)
        val spiritB = TestFixtures.createSpirit("fc_spirit_b", normalCores = 2, soulCores = 0)
        // Spirit C (3 normal cores)
        val spiritC = TestFixtures.createSpirit("fc_spirit_c", normalCores = 3, soulCores = 0)
        p1.field.addAll(listOf(spiritA, spiritB, spiritC))

        // 26RSD03-001 フラッティ (cost = 2, lvCosts = [1, 3])
        val card = OFFICIAL_CARDS["26RSD03-001"]!!
        p1.hand.add(card)

        val actions = enumerateLevelActions(state)
        val summonActions = actions.filter { it.type == "SUMMON" }

        // Must have multiple distinct summon actions (e.g. paying/placing from spirit B, from spirit C, etc.)
        assertTrue(summonActions.size > 1, "Must enumerate multiple distinct summon actions when multiple spirits can be vanished/used: got ${summonActions.size}")

        // Ensure different vanishing targets exist among the summon actions
        val vanishingNotations = summonActions.mapNotNull {
            Regex("⚠️(.*?消滅)").find(it.detail)?.groupValues?.get(1)
        }
        assertTrue(vanishingNotations.isNotEmpty(), "Must have vanishing notations in summon actions")
    }
}
