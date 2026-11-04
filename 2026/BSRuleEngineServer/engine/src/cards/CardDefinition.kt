package bstools.model

import bstools.model.effects.CardScript

/**
 * カードマスターデータと個別スクリプトロジックを統合したカード定義インターフェース。
 * 名称、属性(色)、系統、BP、コスト、効果テキスト等の基本情報は [data] に定義される。
 */
interface CardDefinition : CardScript {
    val data: CardData
    override val cardNo: String get() = data.cardNo
}
