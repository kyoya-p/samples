val Card_ユニコーンモード = Card(
    cardType = CardType.SPIRIT,
    cardName = "ユニコーンガンダム［ユニコーンモード］",
    cardSymbols = listOf(Symbol(listOf(SymbolColor.RED), 1)),
    cardCost = 5,
    cardReduction = listOf(ReductionSymbol(SymbolColor.RED, 3)),
    lvCosts = listOf(1, 3, 4)
)

val Card_デストロイモード = Card(
    cardType = CardType.SPIRIT,
    cardName = "ユニコーンガンダム［デストロイモード］",
    cardSymbols = listOf(Symbol(listOf(SymbolColor.RED), 2)),
    cardCost = 8,
    cardReduction = listOf(ReductionSymbol(SymbolColor.RED, 4)),
    lvCosts = listOf(1, 3, 5)
)

val Card_ブレイドラX = Card(
    cardType = CardType.SPIRIT,
    cardName = "ブレイドラX",
    cardSymbols = listOf(Symbol(listOf(SymbolColor.RED), 1)),
    cardCost = 0,
    cardReduction = listOf(ReductionSymbol(SymbolColor.RED, 1)),
    lvCosts = listOf(1, 2, 3)
)

val Deck1 = Deck(
    fixed = 0, // 初期手札の4枚は固定カードとする
    cards = listOf<Card>(
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX, // 初期手札にブレイドラXが来るようにする
        Card_ユニコーンモード,
        Card_ユニコーンモード,
        Card_ユニコーンモード,
        Card_ユニコーンモード,
        Card_ユニコーンモード,
        Card_ユニコーンモード, // 6 Unicorn Mode
        Card_デストロイモード,
        Card_デストロイモード,
        Card_デストロイモード,
        Card_デストロイモード,
        Card_デストロイモード,
        Card_デストロイモード, // 6 Destroy Mode
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
        Card_ブレイドラX,
    )
)