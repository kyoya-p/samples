package bstools.model

import kotlin.random.Random

/**
 * ゲーム初期化ロジック。GameServer と Playmats の双方が同じ初期化を使えるよう共有する
 * (以前は Playmats にだけ存在し、GameServer は似て非なる初期化を独自に持っていた)。
 */

/** デッキファイルが読み込めない場合のフォールバック用デッキ (26RSD03 のスピリットの一部、公式カードデータ) */
fun createKogyoDeckCards(): List<CardData> =
    listOf("26RSD03-001", "26RSD03-002", "26RSD03-004", "26RSD03-X01", "26RSD03-X02").map { CardLoader.loadCard(it) }

/** 総合ルール 6-1〜6-2 に基づくゲーム開始処理 (セットアップ + 先攻1ターン目の開始ステップ群) */
fun createInitialGameState(
    deck1Name: String = "deck-kogyo.yaml",
    deck2Name: String = "deck-kogyo.yaml",
    seed: Long = 42L,
    format: GameFormat = GameFormat.STANDARD
): GameState {
    val d1Cards = CardLoader.loadDeck(deck1Name).ifEmpty { createKogyoDeckCards() }.toMutableList()
    val d2Cards = CardLoader.loadDeck(deck2Name).ifEmpty { createKogyoDeckCards() }.toMutableList()

    val rng = Random(seed)
    d1Cards.shuffle(rng)
    d2Cards.shuffle(rng)

    val p1Hand = d1Cards.take(INITIAL_HAND_SIZE).toMutableList()
    val p2Hand = d2Cards.take(INITIAL_HAND_SIZE).toMutableList()
    val p1Deck = d1Cards.drop(INITIAL_HAND_SIZE).toMutableList()
    val p2Deck = d2Cards.drop(INITIAL_HAND_SIZE).toMutableList()

    val state = GameState(
        turn = 1,
        activePlayerId = 1,
        step = Step.START,
        player1 = PlayerState(
            playerId = 1,
            name = if (deck1Name.contains("fara")) "Player 1 (ファラ)" else "Player 1 (甲魚)",
            life = 5,
            reserve = Cores(3, 1),
            trash = Cores(0, 0),
            deckCount = p1Deck.size,
            hand = p1Hand,
            deck = p1Deck
        ),
        player2 = PlayerState(
            playerId = 2,
            name = if (deck2Name.contains("fara")) "Player 2 (ファラ)" else "Player 2 (甲魚)",
            life = 5,
            reserve = Cores(3, 1),
            trash = Cores(0, 0),
            deckCount = p2Deck.size,
            hand = p2Hand,
            deck = p2Deck
        ),
        format = format,
        seed = seed
    )
    // 先攻1ターン目の開始処理 (スタート→ドロー→リフレッシュ→メイン。コアステップは無し)
    beginTurn(state, mutableListOf())
    return state
}
