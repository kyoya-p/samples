val Card_ユニコーンモード = Card(
    cardType = CardType.SPIRIT,
    cardName = "ユニコーンモード",
    cardSymbols = listOf(Symbol(listOf(SymbolColor.RED), 1)),
    cardCost = 5,
    cardReduction = listOf(Symbol(listOf(SymbolColor.RED), 3))
)

val Card_デストロイモード = Card(
    cardType = CardType.SPIRIT,
    cardName = "デストロイモード",
    cardSymbols = listOf(Symbol(listOf(SymbolColor.RED), 2)),
    cardCost = 8,
    cardReduction = listOf(Symbol(listOf(SymbolColor.RED), 4))
)

val Card_契約イザイザ = Card(
    cardType = CardType.SPIRIT,
    cardName = "契イザ",
    cardSymbols = listOf(
        Symbol(listOf(SymbolColor.PURPLE, SymbolColor.GOD), 1),
        Symbol(listOf(SymbolColor.GREEN, SymbolColor.GOD), 1)
    ),
    cardCost = 4,
    cardReduction = listOf(
        Symbol(listOf(SymbolColor.PURPLE), 1),
        Symbol(listOf(SymbolColor.GREEN), 1)
    )
)

val Deck1 = Deck(
    fixed = 1, // Assuming fixed cards are still 1 for now, though the rule update didn't specify.
    cards = listOf<Card>(
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
        Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ,
        Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ,
        Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ,
        Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ, Card_契約イザイザ,
        Card_契約イザイザ, Card_契約イザイザ // Fill the rest to 40 cards (6+6+28 = 40)
    )
)