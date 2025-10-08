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
    fixed = 1,
    cards = listOf<Card>(
        Card_契約イザイザ,
        Card_契約イザイザ,
        Card_契約イザイザ,
        Card_契約イザイザ,
        Card_契約イザイザ,
        Card_契約イザイザ,
        Card_契約イザイザ,
    )
)
