package bstools.model

import bstools.model.cards.Card26RSD03_002
import bstools.model.cards.Card26RSD03_010
import bstools.model.cards.Card26RSD03_011
import bstools.model.effects.*
import kotlin.test.*

class CardPluginAndEffectsTest {

    @BeforeTest
    fun setup() {
        // Ensure standard scripts are registered before each test
        Card26RSD03_002.register()
        Card26RSD03_010.register()
        Card26RSD03_011.register()
    }

    @AfterTest
    fun tearDown() {
        // Restore standard scripts after each test
        Card26RSD03_002.register()
        Card26RSD03_010.register()
        Card26RSD03_011.register()
    }

    @Test
    fun testPufferSummonEffectAddsCoreFromVoidToTrash() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 4, p1ReserveSoul = 0, step = Step.MAIN_2)
        // 26RSD03-002 パッファー (cost = 2, reduction = 1 green, lvCosts = listOf(1, 3))
        val pufferCard = OFFICIAL_CARDS["26RSD03-002"]!!
        p1.hand.add(pufferCard)

        val initialTrash = p1.trash.normal

        // Summon Puffer: pay 2 from Reserve, place 1 from Reserve
        val summonAction = GameAction(
            index = 1,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Summon Puffer",
            handIndex = 0,
            costToPay = 2,
            payFrom = listOf(CoreAlloc("Reserve", "R", normal = 2, soul = 0)),
            placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, summonAction, messages)

        // Reserve: 4 - 2 (paid) - 1 (placed) = 1
        assertEquals(1, p1.reserve.normal)
        // Trash: initial (0) + 2 (paid cost) + 1 (from Void via onSummon effect) = 3
        assertEquals(initialTrash + 2 + 1, p1.trash.normal, "Trash must contain paid cost + 1 core from Void")
        assertTrue(messages.any { it.contains("【パッファー】召喚時効果") }, "Summon message must include Puffer effect message")

        // Puffer is on field with 1 core
        assertEquals(1, p1.field.size)
        assertEquals("26RSD03-002", p1.field[0].cardNo)
        assertEquals(1, p1.field[0].cores.normal)
        assertEquals(1, p1.field[0].level)
    }

    @Test
    fun testCardScriptUnregisterDisablesEffect() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 4, p1ReserveSoul = 0, step = Step.MAIN_2)
        val pufferCard = OFFICIAL_CARDS["26RSD03-002"]!!
        p1.hand.add(pufferCard)

        // Unregister Puffer script
        assertTrue(CardScriptRegistry.unregister("26RSD03-002"), "Unregister should succeed")
        assertNull(CardScriptRegistry.get("26RSD03-002"), "Script should no longer be registered")

        val summonAction = GameAction(
            index = 1,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Summon Puffer without script",
            handIndex = 0,
            costToPay = 2,
            payFrom = listOf(CoreAlloc("Reserve", "R", normal = 2, soul = 0)),
            placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, summonAction, messages)

        // Trash: initial (0) + 2 (cost) = 2 (No +1 from Void since script is unregistered)
        assertEquals(2, p1.trash.normal, "Trash must only contain paid cost without Void bonus")
        assertFalse(messages.any { it.contains("【パッファー】召喚時効果") })
    }

    @Test
    fun testDynamicPluginAdapterRegistration() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 4, p1ReserveSoul = 0, step = Step.MAIN_2)
        val pufferCard = OFFICIAL_CARDS["26RSD03-002"]!!
        p1.hand.add(pufferCard)

        // Create a custom external plugin that places 2 cores to Reserve instead of 1 to Trash
        val customPlugin = object : CardEffectPlugin {
            override val pluginName: String = "TestCustomPlugin"
            override val supportedCardNos: Set<String> = setOf("26RSD03-002")

            override fun handleEvent(event: EffectEvent): EffectResult? {
                if (event.eventType == "ON_SUMMON" && event.cardNo == "26RSD03-002") {
                    return EffectResult(
                        message = "Custom plugin invoked: +2 cores to Reserve from Void",
                        commands = listOf(
                            EffectCommand(
                                type = "ADD_CORE",
                                source = "Void",
                                target = "Reserve",
                                count = 2
                            )
                        )
                    )
                }
                return null
            }
        }

        // Register custom plugin via PluginCardScript adapter
        val adapter = PluginCardScript(customPlugin, "26RSD03-002")
        CardScriptRegistry.register(adapter)

        val summonAction = GameAction(
            index = 1,
            type = "SUMMON",
            category = "SUMMON",
            detail = "Summon Puffer with custom plugin",
            handIndex = 0,
            costToPay = 2,
            payFrom = listOf(CoreAlloc("Reserve", "R", normal = 2, soul = 0)),
            placeFrom = listOf(CoreAlloc("Reserve", "R", normal = 1, soul = 0))
        )
        val messages = mutableListOf<String>()
        applyAction(state, summonAction, messages)

        // Cost: 2 moved to Trash (trash = 2)
        assertEquals(2, p1.trash.normal)
        // Reserve: initial 4 - 2 (paid) - 1 (placed) + 2 (custom plugin from Void) = 3
        assertEquals(3, p1.reserve.normal)
        assertTrue(messages.any { it.contains("Custom plugin invoked") })
    }

    @Test
    fun testFlattyFlashSummonDuringOpponentAttackStep() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 4, p1ReserveSoul = 1, step = Step.BLOCK_DECLARATION)
        // Opponent (Player 2) is active attacker
        state.activePlayerId = 2
        val p2 = state.player2
        val attacker = TestFixtures.createSpirit("fc_opp_attacker", cardNo = "26RSD03-004", normalCores = 2)
        p2.field.add(attacker)
        state.attackingCardId = attacker.instanceId

        // Player 1 (defending) has Flatty in hand
        val flatty = OFFICIAL_CARDS["26RSD03-001"]!!
        p1.hand.add(flatty)

        // Enumerate actions for Player 1 during block declaration
        val actions = enumerateActions(state)
        val flashActions = actions.filter { it.type == "FLASH_SUMMON" }

        assertTrue(flashActions.isNotEmpty(), "Must enumerate Flash Summon options for Flatty")
        assertTrue(flashActions.any { it.detail.contains("フラッシュ召喚 Lv1") })

        // Apply Flash Summon (cost 2 paid from Reserve, 1 placed from Reserve)
        val flashAction = flashActions.first { it.costToPay == 2 && it.placeFrom.first().normal == 1 }
        val messages = mutableListOf<String>()
        applyAction(state, flashAction, messages)

        // Hand is now empty
        assertTrue(p1.hand.isEmpty())
        // Flatty is now on field
        assertEquals(1, p1.field.size)
        val summonedFlatty = p1.field.first()
        assertEquals("26RSD03-001", summonedFlatty.cardNo)
        assertEquals(1, summonedFlatty.cores.normal)
        assertEquals(1, summonedFlatty.level)
        assertFalse(summonedFlatty.isExhausted, "Summoned Flatty must be unexhausted")

        // Reserve: initial 4 normal - 2 paid - 1 placed = 1 normal, 1 soul remaining
        assertEquals(1, p1.reserve.normal)
        assertEquals(1, p1.reserve.soul)
        // Trash: 2 paid cost
        assertEquals(2, p1.trash.normal)

        // Now re-enumerate actions: Flatty can now block!
        val nextActions = enumerateActions(state)
        val blockActions = nextActions.filter { it.type == "BLOCK" }
        assertTrue(blockActions.any { it.targetInstanceId == summonedFlatty.instanceId }, "Newly summoned Flatty must be available to block")
    }

    @Test
    fun testFlattyFlashSummonNotAvailableInOwnTurn() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 4, p1ReserveSoul = 0, step = Step.BLOCK_DECLARATION)
        // Player 1 is active player (own turn)
        state.activePlayerId = 1
        p1.hand.add(OFFICIAL_CARDS["26RSD03-001"]!!)

        val actions = enumerateActions(state)
        val flashActions = actions.filter { it.type == "FLASH_SUMMON" }
        assertTrue(flashActions.isEmpty(), "Must NOT enumerate Flash Summon during own turn")
    }

    @Test
    fun testRasheadLv2ShinkaiBpBoostWhileAttacking() {
        val (state, p1) = TestFixtures.createTestState(step = Step.ATTACK_DECLARATION)
        val p2 = state.player2

        // Rashead (#3: 26RSD03-003) on P1 field with 1 normal core + 1 soul core -> Lv2 via Shinkai
        val rashead = TestFixtures.createSpirit("fc_rashead", cardNo = "26RSD03-003", normalCores = 1, soulCores = 1)
        p1.field.add(rashead)

        assertEquals(2, rashead.level, "Rashead with Soul Core must be Lv2 via Shinkai")
        assertEquals(5000, rashead.baseBp, "Rashead Lv2 base BP is 5000")
        assertEquals(5000, rashead.effectiveBp(state), "When not attacking, effective BP must be 5000")

        // Opponent (P2) has a blocker with 6000 BP (26RSD03-009 透笠オリンディアス Lv1 BP 6000)
        val blocker = TestFixtures.createSpirit("fc_p2_blocker", cardNo = "26RSD03-009", normalCores = 1)
        p2.field.add(blocker)
        assertEquals(6000, blocker.effectiveBp(state), "Blocker BP is 6000")

        // Enumerate attack actions for P1
        val attackActions = enumerateActions(state).filter { it.type == "ATTACK" }
        assertTrue(attackActions.isNotEmpty())
        val attackAction = attackActions.first { it.targetInstanceId == rashead.instanceId }
        assertTrue(attackAction.detail.contains("BP7000"), "Attack action detail should preview boosted BP7000: ${attackAction.detail}")

        // Apply ATTACK action
        val messages = mutableListOf<String>()
        applyAction(state, attackAction, messages)

        // Now state.attackingCardId is set to rashead and step is FLASH_TIMING
        assertEquals(rashead.instanceId, state.attackingCardId)
        assertEquals(Step.FLASH_TIMING, state.step)
        assertEquals(7000, rashead.effectiveBp(state), "While attacking, Rashead Lv2 BP must be boosted to 7000")
        assertTrue(messages.any { it.contains("アタック中BP+2000！ (BP7000)") })

        // Flash timing: both players pass flash to proceed to block declaration (総合ルール 8-1-2)
        // 1. Defending player (P2) passes
        val p2PassAction = enumerateActions(state).first { it.type == "PASS_FLASH" }
        applyAction(state, p2PassAction, messages)
        assertEquals(Step.FLASH_TIMING, state.step)

        // 2. Attacking player (P1) passes
        val p1PassAction = enumerateActions(state).first { it.type == "PASS_FLASH" }
        applyAction(state, p1PassAction, messages)
        assertEquals(Step.BLOCK_DECLARATION, state.step)

        // Opponent's turn to declare block
        val blockActions = enumerateActions(state).filter { it.type == "BLOCK" }
        assertTrue(blockActions.isNotEmpty())
        val blockAction = blockActions.first { it.targetInstanceId == blocker.instanceId }
        assertTrue(blockAction.detail.contains("BP6000 vs BP7000 → #1 透笠オリンディアスが破壊される"), "Blocker detail must show Rashead winning battle: ${blockAction.detail}")

        // Apply BLOCK action -> advances to 2nd Flash Timing (総合ルール 8-1-4)
        applyAction(state, blockAction, messages)
        assertEquals(Step.FLASH_TIMING, state.step, "Blocking advances to 2nd Flash Timing")
        assertEquals(blocker.instanceId, state.blockingCardId)

        // 2nd Flash Timing: both players pass to proceed to Battle Resolution
        // 1. Defending player (P2) passes
        val p2SecondPass = enumerateActions(state).first { it.type == "PASS_FLASH" }
        applyAction(state, p2SecondPass, messages)
        assertEquals(Step.FLASH_TIMING, state.step)

        // 2. Attacking player (P1) passes -> resolves battle
        val p1SecondPass = enumerateActions(state).first { it.type == "PASS_FLASH" }
        applyAction(state, p1SecondPass, messages)

        // Battle resolved: Rashead (7000 BP) destroyed Blocker (6000 BP)
        assertTrue(p2.field.isEmpty(), "Opponent blocker (6000 BP) must be destroyed by 7000 BP Rashead")
        assertEquals(1, p1.field.size, "Rashead (7000 BP) must survive battle")

        // After battle ends, state.attackingCardId is cleared
        assertNull(state.attackingCardId)
        assertEquals(5000, rashead.effectiveBp(state), "After battle ends, Rashead BP must return to 5000")
    }

    @Test
    fun testRasheadLv1DoesNotGetBpBoost() {
        val (state, p1) = TestFixtures.createTestState(step = Step.ATTACK_DECLARATION)

        // Rashead with 1 normal core and 0 soul core -> Lv1 (base BP 3000)
        val rashead = TestFixtures.createSpirit("fc_rashead", cardNo = "26RSD03-003", normalCores = 1, soulCores = 0)
        p1.field.add(rashead)

        assertEquals(1, rashead.level, "Rashead with 1 normal core must be Lv1")
        assertEquals(3000, rashead.baseBp)

        val attackAction = enumerateActions(state).first { it.type == "ATTACK" && it.targetInstanceId == rashead.instanceId }
        assertTrue(attackAction.detail.contains("BP3000"), "Lv1 Rashead should not preview boosted BP: ${attackAction.detail}")

        val messages = mutableListOf<String>()
        applyAction(state, attackAction, messages)

        assertEquals(3000, rashead.effectiveBp(state), "Lv1 Rashead while attacking must remain 3000 BP")
    }

    @Test
    fun testFlashTimingPriorityDefendingPlayerFirstThenAttacker() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 4, p1ReserveSoul = 1, step = Step.ATTACK_DECLARATION)
        val p2 = state.player2
        p2.reserve.normal = 2

        // P1 has Rashead on field and Vortex Shave (26RSD03-013) [フラッシュ] in hand
        val rashead = TestFixtures.createSpirit("fc_rashead", cardNo = "26RSD03-003", normalCores = 2)
        p1.field.add(rashead)

        val vortexShave = OFFICIAL_CARDS["26RSD03-013"]!!
        p1.hand.add(vortexShave)

        // Opponent has a spirit
        val oppSpirit = TestFixtures.createSpirit("fc_opp", cardNo = "26RSD03-001", normalCores = 1)
        p2.field.add(oppSpirit)

        // P1 declares attack
        val attackAction = enumerateActions(state).first { it.type == "ATTACK" }
        val messages = mutableListOf<String>()
        applyAction(state, attackAction, messages)

        // Game enters FLASH_TIMING: defending player (P2) chooses first!
        assertEquals(Step.FLASH_TIMING, state.step)
        assertEquals(2, state.choosingPlayerId, "Defending player (P2) must choose first in Flash Timing (総合ルール 8-1-2-1)")

        // P2 (defender) passes flash
        val p2FlashActions = enumerateActions(state)
        val p2PassAction = p2FlashActions.first { it.type == "PASS_FLASH" }
        applyAction(state, p2PassAction, messages)

        // Priority now alternates to attacking player (P1)
        assertEquals(Step.FLASH_TIMING, state.step, "State remains in FLASH_TIMING after 1 pass")
        assertEquals(1, state.choosingPlayerId, "Attacking player (P1) now chooses after P2 passes")

        // Enumerate flash actions for attacker (P1)
        val p1FlashActions = enumerateActions(state)
        val magicActions = p1FlashActions.filter { it.type == "USE_MAGIC" }
        assertTrue(magicActions.isNotEmpty(), "Must enumerate Flash Magic (Vortex Shave) for attacker in Flash Timing")
        assertTrue(magicActions.any { it.category.contains("ボルテクスシェイブ") }, "Magic actions must include Vortex Shave: ${magicActions.map { it.category }}")

        // Attacker (P1) uses Vortex Shave
        val magicAct = magicActions.first()
        applyAction(state, magicAct, messages)

        assertTrue(p1.hand.isEmpty(), "Magic card in hand must be consumed")
        assertTrue(messages.any { it.contains("「ボルテクスシェイブ」を使用") })
        assertEquals(Step.FLASH_TIMING, state.step, "State remains in FLASH_TIMING after using flash magic")

        // Using magic transfers priority back to P2 (defender)
        assertEquals(2, state.choosingPlayerId, "Using flash magic transfers priority back to opponent (P2)")

        // P2 passes flash
        val p2Pass2 = enumerateActions(state).first { it.type == "PASS_FLASH" }
        applyAction(state, p2Pass2, messages)

        // Priority back to P1
        assertEquals(1, state.choosingPlayerId, "P1 gets priority after P2 passes")

        // P1 now also passes flash -> consecutive passes = 2
        val p1Pass = enumerateActions(state).first { it.type == "PASS_FLASH" }
        applyAction(state, p1Pass, messages)

        // Both passed consecutively -> advances to BLOCK_DECLARATION
        assertEquals(Step.BLOCK_DECLARATION, state.step, "Passing flash consecutively must advance to BLOCK_DECLARATION")
        assertEquals(2, state.choosingPlayerId, "Defending player (P2) chooses in BLOCK_DECLARATION")
    }

    @Test
    fun testGreenDepthsLv2ShinkaiMainActivationEnumeratesAndExecutes() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 3, p1ReserveSoul = 0, step = Step.MAIN)
        // Nexus: 26RSD03-010 最奥：緑淵深 (Lv costs: [0, 3], shinkai = true)
        val depthsCard = OFFICIAL_CARDS["26RSD03-010"]!!
        // Placed on field with 0 normal cores and 1 soul core -> Lv2 via shinkai!
        val depthsNexus = FieldCard(
            instanceId = "fc_depths",
            cardNo = "26RSD03-010",
            name = depthsCard.name,
            category = depthsCard.category,
            colors = depthsCard.colors,
            cores = Cores(normal = 0, soul = 1),
            lvCosts = depthsCard.lvCosts,
            baseSymbols = depthsCard.symbols,
            systems = depthsCard.systems,
            seq = 2,
            cost = depthsCard.cost,
            shinkai = depthsCard.shinkai
        )
        p1.field.add(depthsNexus)
        assertEquals(2, depthsNexus.level, "Depths with Soul Core must be Lv2 via Shinkai")

        // Hand has cost 3 甲魚 card (Puffer: 26RSD03-002) and cost 6 card (26RSD03-X01 Squid)
        val puffer = OFFICIAL_CARDS["26RSD03-002"]!!
        val squid = OFFICIAL_CARDS["26RSD03-X01"]!!
        p1.hand.add(puffer)
        p1.hand.add(squid)

        // Deck has 2 dummy cards to draw from
        val deckTopCard = OFFICIAL_CARDS["26RSD03-001"]!!
        val deckBottomCard = OFFICIAL_CARDS["26RSD03-004"]!!
        p1.deck = mutableListOf(deckTopCard, deckBottomCard)
        p1.deckCount = 2

        // Enumerate actions in MAIN
        val actions = enumerateActions(state)
        val activateActions = actions.filter { it.type == "ACTIVATE_FIELD" && it.targetInstanceId == depthsNexus.instanceId }

        assertEquals(1, activateActions.size, "Must enumerate exactly 1 activate action for Puffer (cost 3 甲魚), Squid is cost 6 so ineligible")
        val act = activateActions.first()
        assertTrue(act.category.contains("最奥：緑淵深"), "Action category must mention 最奥：緑淵深")
        assertTrue(act.detail.contains("パッファー"), "Action detail must mention Puffer")

        // Apply action
        val messages = mutableListOf<String>()
        applyAction(state, act, messages)

        // Puffer returned to bottom of deck, top card (deckTopCard: 001) drawn to hand
        assertEquals(2, p1.hand.size)
        assertTrue(p1.hand.any { it.cardNo == "26RSD03-001" }, "Drawn card must be in hand")
        assertFalse(p1.hand.any { it.cardNo == "26RSD03-002" }, "Puffer must no longer be in hand")
        assertEquals("26RSD03-002", p1.deck.last().cardNo, "Puffer must be at the bottom of deck")
        assertEquals(2, p1.deck.size, "Deck size should remain 2 (1 returned to bottom, 1 drawn from top)")
        assertEquals(1, state.openedCards.size, "Puffer must be recorded in openedCards for opponent confirmation")
        assertEquals("26RSD03-002", state.openedCards.first().cardNo)

        // Effect is 〔ターン1回〕: cannot be used again in the same turn (第1メインでも第2メインでも使用不可)
        val actionsAfter = enumerateActions(state)
        val activateActionsAfter = actionsAfter.filter { it.type == "ACTIVATE_FIELD" && it.targetInstanceId == depthsNexus.instanceId }
        assertTrue(activateActionsAfter.isEmpty(), "Must not enumerate effect again in the same turn (MAIN)")

        // 第2メインステップへ移行しても、同ターン内なら再使用不可
        state.step = Step.MAIN_2
        val actionsMain2 = enumerateActions(state)
        val activateActionsMain2 = actionsMain2.filter { it.type == "ACTIVATE_FIELD" && it.targetInstanceId == depthsNexus.instanceId }
        assertTrue(activateActionsMain2.isEmpty(), "Must NOT enumerate effect in MAIN_2 if already used in MAIN (第一メインで使用したら第二メインでは使用できない)")
    }

    @Test
    fun testGreenDepthsAuraBoostsAttackingSpiritWithKogyoSystem() {
        val (state, p1) = TestFixtures.createTestState(step = Step.ATTACK_DECLARATION)

        // Nexus: 最奥：緑淵深 on field with 0 cores (Lv1)
        val depthsCard = OFFICIAL_CARDS["26RSD03-010"]!!
        val depthsNexus = FieldCard(
            instanceId = "fc_depths",
            cardNo = "26RSD03-010",
            name = depthsCard.name,
            category = depthsCard.category,
            colors = depthsCard.colors,
            cores = Cores(normal = 0, soul = 0),
            lvCosts = depthsCard.lvCosts,
            baseSymbols = depthsCard.symbols,
            systems = depthsCard.systems,
            seq = 2,
            cost = depthsCard.cost,
            shinkai = depthsCard.shinkai
        )
        p1.field.add(depthsNexus)
        assertEquals(1, depthsNexus.level, "Nexus with 0 cores is Lv1")

        // Attacking spirit with 甲魚 system: Puffer (base BP 2000 at Lv1)
        val puffer = TestFixtures.createSpirit("fc_puffer", cardNo = "26RSD03-002", normalCores = 1)
        p1.field.add(puffer)
        assertEquals(2000, puffer.baseBp)

        // Preview BP for hypothetical attack should include +1000 from Depths
        val attackActions = enumerateActions(state).filter { it.type == "ATTACK" && it.targetInstanceId == puffer.instanceId }
        assertEquals(1, attackActions.size)
        assertTrue(attackActions.first().detail.contains("BP3000"), "Attacking Puffer should preview BP3000 (+1000 from Depths Lv1-2 effect): ${attackActions.first().detail}")

        // Apply attack
        val messages = mutableListOf<String>()
        applyAction(state, attackActions.first(), messages)

        assertEquals("fc_puffer", state.attackingCardId)
        assertEquals(3000, puffer.effectiveBp(state), "Effective BP while attacking must be 3000 (2000 base + 1000 from Depths)")
    }

    /**
     * [26RSD03-011] 水泡群域 Lv2メインステップ開始時効果:
     * 疲労状態の系統：「甲魚」を持つスピリットが2体以上いる場合、ボイドからコア1個をリザーブに置く。
     */
    @Test
    fun testWaterClusterLv2MainStepStartCoreBoostWhenConditionMet() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 2, step = Step.MAIN)
        val waterClusterCard = OFFICIAL_CARDS["26RSD03-011"]!!
        val nexus = FieldCard(
            instanceId = "fc_water_cluster",
            cardNo = "26RSD03-011",
            name = waterClusterCard.name,
            category = waterClusterCard.category,
            colors = waterClusterCard.colors,
            cores = Cores(normal = 2, soul = 0), // Lv2 (lvCosts=[0, 2])
            lvCosts = waterClusterCard.lvCosts,
            baseSymbols = waterClusterCard.symbols,
            systems = waterClusterCard.systems,
            seq = 3
        )
        p1.field.add(nexus)
        assertEquals(2, nexus.level)

        // 疲労状態の甲魚スピリット2体
        val spirit1 = TestFixtures.createSpirit("fc_sp1", cardNo = "26RSD03-003", normalCores = 1)
        spirit1.isExhausted = true
        val spirit2 = TestFixtures.createSpirit("fc_sp2", cardNo = "26RSD03-005", normalCores = 1)
        spirit2.isExhausted = true
        p1.field.add(spirit1)
        p1.field.add(spirit2)

        // メインステップ開始時の効果が最優先で列挙されること
        val actions = enumerateActions(state)
        assertEquals(1, actions.size, "未解決のステップ開始時効果がある間は、それのみが優先列挙されること")
        val effectAction = actions.first()
        assertEquals("STEP_START_EFFECT", effectAction.type)
        assertTrue(effectAction.detail.contains("ボイドからコア1個をリザーブに置く"))

        // 解決実行
        val messages = mutableListOf<String>()
        applyAction(state, effectAction, messages)

        // リザーブのコアが2 -> 3に増加
        assertEquals(3, p1.reserve.normal)
        assertTrue(messages.any { it.contains("水泡群域") && it.contains("ボイドからコア1個をリザーブに置いた") })

        // 解決後は通常のメインステップ行動 (召喚・コア移動・ステップ終了など) が列挙されること
        val normalActions = enumerateActions(state)
        assertTrue(normalActions.size > 1, "効果解決後は通常のアクションが列挙されること")
        assertFalse(normalActions.any { it.type == "STEP_START_EFFECT" }, "同じターンに再発揮しないこと")
    }

    /**
     * 水泡群域 Lv2効果: 疲労状態の甲魚スピリットが1体のみの場合 (ユーザーの画像条件)、発揮しない。
     */
    @Test
    fun testWaterClusterLv2NotTriggeredWhenConditionNotMet() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 2, step = Step.MAIN)
        val waterClusterCard = OFFICIAL_CARDS["26RSD03-011"]!!
        val nexus = FieldCard(
            instanceId = "fc_water_cluster",
            cardNo = "26RSD03-011",
            name = waterClusterCard.name,
            category = waterClusterCard.category,
            colors = waterClusterCard.colors,
            cores = Cores(normal = 2, soul = 0),
            lvCosts = waterClusterCard.lvCosts,
            baseSymbols = waterClusterCard.symbols,
            systems = waterClusterCard.systems,
            seq = 3
        )
        p1.field.add(nexus)

        // 疲労1体 + 回復1体 (今回のスクリーンショットの状況)
        val spirit1 = TestFixtures.createSpirit("fc_sp1", cardNo = "26RSD03-003", normalCores = 1)
        spirit1.isExhausted = true
        val spirit2 = TestFixtures.createSpirit("fc_sp2", cardNo = "26RSD03-005", normalCores = 1)
        spirit2.isExhausted = false
        p1.field.add(spirit1)
        p1.field.add(spirit2)

        val actions = enumerateActions(state)
        // 条件を満たしていないため STEP_START_EFFECT は列挙されない
        assertFalse(actions.any { it.type == "STEP_START_EFFECT" }, "疲労スピリットが1体のみの場合は発揮しないこと")
    }

    /**
     * 総合ルール 8-1: 複数のステップ開始時効果が同時に発揮する場合、プレイヤーが解決順を選択する。
     */
    @Test
    fun testMultipleStepStartEffectsAllowPlayerToChooseResolutionOrder() {
        val (state, p1) = TestFixtures.createTestState(p1ReserveNormal = 0, step = Step.MAIN)
        val waterClusterCard = OFFICIAL_CARDS["26RSD03-011"]!!

        // 水泡群域2枚 (両方Lv2)
        val nexus1 = FieldCard(
            instanceId = "fc_wc1",
            cardNo = "26RSD03-011",
            name = "水泡群域A",
            category = waterClusterCard.category,
            colors = waterClusterCard.colors,
            cores = Cores(normal = 2, soul = 0),
            lvCosts = waterClusterCard.lvCosts,
            baseSymbols = waterClusterCard.symbols,
            systems = waterClusterCard.systems,
            seq = 1
        )
        val nexus2 = FieldCard(
            instanceId = "fc_wc2",
            cardNo = "26RSD03-011",
            name = "水泡群域B",
            category = waterClusterCard.category,
            colors = waterClusterCard.colors,
            cores = Cores(normal = 2, soul = 0),
            lvCosts = waterClusterCard.lvCosts,
            baseSymbols = waterClusterCard.symbols,
            systems = waterClusterCard.systems,
            seq = 2
        )
        p1.field.add(nexus1)
        p1.field.add(nexus2)

        // 疲労状態の甲魚スピリット2体
        val sp1 = TestFixtures.createSpirit("fc_sp1", cardNo = "26RSD03-003", normalCores = 1)
        sp1.isExhausted = true
        val sp2 = TestFixtures.createSpirit("fc_sp2", cardNo = "26RSD03-005", normalCores = 1)
        sp2.isExhausted = true
        p1.field.add(sp1)
        p1.field.add(sp2)

        // 1. 同時発揮: 2つの効果が同時に列挙され、プレイヤーが選択可能
        val simultaneousActions = enumerateActions(state)
        assertEquals(2, simultaneousActions.size, "両方の効果が解決待ちとして列挙されること")
        assertTrue(simultaneousActions.any { it.targetInstanceId == "fc_wc1" })
        assertTrue(simultaneousActions.any { it.targetInstanceId == "fc_wc2" })

        // 2. プレイヤーが nexus1 を先に解決する選択を行う
        val chooseFirst = simultaneousActions.first { it.targetInstanceId == "fc_wc1" }
        val messages = mutableListOf<String>()
        applyAction(state, chooseFirst, messages)
        assertEquals(1, p1.reserve.normal, "1枚目解決でコア+1")

        // 3. まだ nexus2 が未解決なので、引き続き nexus2 のみが列挙される
        val nextActions = enumerateActions(state)
        assertEquals(1, nextActions.size, "未解決の2枚目のみが列挙されること")
        assertEquals("fc_wc2", nextActions.first().targetInstanceId)

        // 4. 2枚目を解決
        applyAction(state, nextActions.first(), messages)
        assertEquals(2, p1.reserve.normal, "2枚目解決でさらにコア+1 (計2個)")

        // 5. 全ての効果が解決されたので、通常のメインステップ行動へ進む
        val afterAllResolved = enumerateActions(state)
        assertTrue(afterAllResolved.size > 1)
        assertFalse(afterAllResolved.any { it.type == "STEP_START_EFFECT" })
    }

    /**
     * 総合ルール 8-1-4: ブロック宣言後、第2フラッシュタイミングが発生し、
     * 防御側から優先権を得て、2連続パス後にバトル解決 (BP勝負) が行われる。
     */
    @Test
    fun testBlockFollowedBySecondFlashTimingAndBattleResolution() {
        val (state, p1) = TestFixtures.createTestState(step = Step.ATTACK_DECLARATION)
        val p2 = state.player2

        // P1: アタッカー (BP5000)
        val attacker = TestFixtures.createSpirit("fc_att", cardNo = "26RSD03-005", normalCores = 1) // モラモーラ Lv1 BP5000
        p1.field.add(attacker)

        // P2: ブロッカー (BP2000)
        val blocker = TestFixtures.createSpirit("fc_blk", cardNo = "26RSD03-002", normalCores = 1) // パッファー Lv1 BP2000
        p2.field.add(blocker)

        val messages = mutableListOf<String>()

        // 1. P1 アタック宣言
        val attackAction = enumerateActions(state).first { it.type == "ATTACK" }
        applyAction(state, attackAction, messages)
        assertEquals(Step.FLASH_TIMING, state.step, "第1フラッシュへ")

        // 2. 第1フラッシュ: P2パス ➔ P1パス
        val p2Pass1 = enumerateActions(state).first { it.type == "PASS_FLASH" }
        applyAction(state, p2Pass1, messages)
        val p1Pass1 = enumerateActions(state).first { it.type == "PASS_FLASH" }
        applyAction(state, p1Pass1, messages)

        assertEquals(Step.BLOCK_DECLARATION, state.step, "第1フラッシュ2連続パスでブロック宣言ステップへ")

        // 3. P2 ブロック宣言
        val blockAction = enumerateActions(state).first { it.type == "BLOCK" }
        applyAction(state, blockAction, messages)

        // 4. 総合ルール 8-1-4: 即BP比べにならず、ブロック後フラッシュタイミング (第2フラッシュ) へ移行！
        assertEquals(Step.FLASH_TIMING, state.step, "ブロック後は第2フラッシュタイミングへ進むこと")
        assertEquals(blocker.instanceId, state.blockingCardId, "blockingCardIdが保持されていること")
        assertEquals(2, state.choosingPlayerId, "第2フラッシュも防御側 (P2) から優先権開始")

        // 5. 第2フラッシュ: P2パス ➔ P1へ優先権
        val p2Pass2 = enumerateActions(state).first { it.type == "PASS_FLASH" }
        applyAction(state, p2Pass2, messages)
        assertEquals(Step.FLASH_TIMING, state.step)
        assertEquals(1, state.choosingPlayerId, "P2パス後、攻撃側 (P1) へ優先権移動")

        // 6. P1パス (2連続パス) ➔ バトル解決 (BP勝負) が実行され、BPの低いブロッカーが破壊される！
        val p1Pass2 = enumerateActions(state).first { it.type == "PASS_FLASH" }
        applyAction(state, p1Pass2, messages)

        assertTrue(messages.any { it.contains("バトル解決") }, "バトル解決メッセージが出力されていること")
        assertTrue(messages.any { it.contains("パッファー") && it.contains("破壊") }, "パッファー破壊メッセージが出力されていること")
        assertNull(state.blockingCardId, "バトル解決後は blockingCardId がクリアされること")
        assertNull(state.attackingCardId, "バトル解決後は attackingCardId がクリアされること")
        assertEquals(Step.ATTACK_DECLARATION, state.step, "バトル解決後は次のアタック宣言ステップへ復帰すること")
        assertTrue(p2.field.isEmpty(), "BP2000のパッファーがBP5000のモラモーラに破壊されてフィールドから消えていること")
        assertEquals(1, p2.trashCards.size, "破壊されたパッファーはトラッシュへ")
    }

    /**
     * 総合ルール 8-1-4-1: ブロックしない (PASS_BLOCK) 場合は第2フラッシュは発生せず、
     * 直接ライフダメージ処理を行ってアタック宣言ステップへ戻る。
     */
    @Test
    fun testPassBlockSkipsSecondFlashTiming() {
        val (state, p1) = TestFixtures.createTestState(step = Step.ATTACK_DECLARATION)
        val p2 = state.player2
        val initialLife = p2.life

        val attacker = TestFixtures.createSpirit("fc_att", cardNo = "26RSD03-005", normalCores = 1)
        p1.field.add(attacker)

        val messages = mutableListOf<String>()

        // アタック宣言 ➔ 第1フラッシュ (両者パス) ➔ ブロック宣言
        applyAction(state, enumerateActions(state).first { it.type == "ATTACK" }, messages)
        applyAction(state, enumerateActions(state).first { it.type == "PASS_FLASH" }, messages)
        applyAction(state, enumerateActions(state).first { it.type == "PASS_FLASH" }, messages)

        assertEquals(Step.BLOCK_DECLARATION, state.step)

        // ブロックしない (スルー)
        val passBlock = enumerateActions(state).first { it.type == "PASS_BLOCK" }
        applyAction(state, passBlock, messages)

        // 第2フラッシュなしで直接ライフが減り、アタック宣言ステップへ復帰
        assertEquals(initialLife - 1, p2.life)
        assertEquals(Step.ATTACK_DECLARATION, state.step)
        assertNull(state.attackingCardId)
        assertNull(state.blockingCardId)
    }
}
