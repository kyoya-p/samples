#!/usr/bin/env kotlin
// AI:編集禁止

@file:DependsOn("io.ktor:ktor-client-core:3.3.3")

import kotlinx.serialization.Serializable


@Serializable
data class SearchCard(
    val cardNo: String,
    val name: String,
    val rarity: String,
    val cost: String,
    val type: String, // S:スピリット、U:アルティメット, B:ブラヴ、N:ネクサス、M:マジック
    val attribute: String, // 全色なら"赤紫緑白黄青"
    val systems: List<String>,
    val imgUrl: String,
)

data class Card(
    val cardNo: String,
    val side: String, // "" or "A" or "B"
    val name: String,
    val rarity: String,
    val cost: Int,
    val reductionSymbols: List<String>,
    val attributes: List<String>,
    val category: String,
    val systems: List<String>,
    val lvInfo: List<LvInfo>,
    val effect: String,
    val imageUrl: String
)

data class LvInfo(val level: Int, val core: Int, val bp: Int?)

