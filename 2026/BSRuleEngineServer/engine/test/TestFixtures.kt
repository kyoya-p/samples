package bstools.model

/**
 * Common test fixtures and helpers for Battle Spirits Standard Rule Engine verification.
 * Enables clean, memory-only GameState construction for unit and integration testing.
 */
object TestFixtures {

    /**
     * Create a standard test GameState with customizable reserve cores.
     */
    fun createTestState(
        p1ReserveNormal: Int = 4,
        p1ReserveSoul: Int = 1,
        p2ReserveNormal: Int = 3,
        p2ReserveSoul: Int = 1,
        step: Step = Step.MAIN,
        activePlayerId: Int = 1
    ): Pair<GameState, PlayerState> {
        val p1 = PlayerState(
            playerId = 1,
            name = "Player 1",
            life = 5,
            reserve = Cores(normal = p1ReserveNormal, soul = p1ReserveSoul),
            trash = Cores(normal = 0, soul = 0),
            hand = mutableListOf(),
            deck = mutableListOf(),
            field = mutableListOf(),
            trashCards = mutableListOf()
        )
        val p2 = PlayerState(
            playerId = 2,
            name = "Player 2",
            life = 5,
            reserve = Cores(normal = p2ReserveNormal, soul = p2ReserveSoul),
            trash = Cores(normal = 0, soul = 0),
            hand = mutableListOf(),
            deck = mutableListOf(),
            field = mutableListOf(),
            trashCards = mutableListOf()
        )
        val state = GameState(
            turn = 1,
            activePlayerId = activePlayerId,
            step = step,
            player1 = p1,
            player2 = p2,
            format = GameFormat.STANDARD
        )
        return Pair(state, if (activePlayerId == 1) p1 else p2)
    }

    /**
     * Create a Spirit FieldCard from OFFICIAL_CARDS (defaults to 26RSD03-001 "フラッティ").
     */
    fun createSpirit(
        instanceId: String,
        cardNo: String = "26RSD03-001",
        normalCores: Int = 1,
        soulCores: Int = 0,
        isExhausted: Boolean = false
    ): FieldCard {
        val card = OFFICIAL_CARDS[cardNo] ?: error("Card $cardNo not found in OFFICIAL_CARDS")
        return FieldCard(
            instanceId = instanceId,
            cardNo = card.cardNo,
            name = card.name,
            category = card.category,
            colors = card.colors,
            cores = Cores(normal = normalCores, soul = soulCores),
            isExhausted = isExhausted,
            lvCosts = card.lvCosts,
            baseSymbols = card.symbols,
            systems = card.systems,
            lvBps = card.lvBps,
            seq = 1,
            cost = card.cost,
            shinkai = card.shinkai
        )
    }

    /**
     * Create a Nexus FieldCard from OFFICIAL_CARDS (defaults to 26RSD03-010 "最奥：緑淵深").
     */
    fun createNexus(
        instanceId: String,
        cardNo: String = "26RSD03-010",
        normalCores: Int = 0,
        soulCores: Int = 0,
        isExhausted: Boolean = false
    ): FieldCard {
        val card = OFFICIAL_CARDS[cardNo] ?: error("Card $cardNo not found in OFFICIAL_CARDS")
        return FieldCard(
            instanceId = instanceId,
            cardNo = card.cardNo,
            name = card.name,
            category = card.category,
            colors = card.colors,
            cores = Cores(normal = normalCores, soul = soulCores),
            isExhausted = isExhausted,
            lvCosts = card.lvCosts,
            baseSymbols = card.symbols,
            systems = card.systems,
            lvBps = card.lvBps,
            seq = 2,
            cost = card.cost,
            shinkai = card.shinkai
        )
    }

    /**
     * Create a custom FieldCard with explicit LvCosts and category (e.g. for edge case testing).
     */
    fun createCustomFieldCard(
        instanceId: String,
        name: String,
        category: CardCategory,
        lvCosts: List<Int>,
        normalCores: Int = 0,
        soulCores: Int = 0,
        colors: List<CardColor> = listOf(CardColor.GREEN),
        shinkai: Boolean = false
    ): FieldCard {
        return FieldCard(
            instanceId = instanceId,
            cardNo = "CUSTOM_$instanceId",
            name = name,
            category = category,
            colors = colors,
            cores = Cores(normal = normalCores, soul = soulCores),
            isExhausted = false,
            lvCosts = lvCosts,
            baseSymbols = colors,
            systems = listOf("テスト系統"),
            lvBps = listOf(3000),
            seq = 99,
            cost = 3,
            shinkai = shinkai
        )
    }
}
