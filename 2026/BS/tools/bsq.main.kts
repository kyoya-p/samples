@file:DependsOn("org.jetbrains.kotlinx:kotlinx-io-core:0.8.1")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
@file:DependsOn("io.ktor:ktor-client-core-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-serialization-kotlinx-json-jvm:3.3.3")
@file:DependsOn("com.fleeksoft.ksoup:ksoup-jvm:0.2.5")
@file:DependsOn("com.charleskorn.kaml:kaml-jvm:0.63.0")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.16")
@file:Import("./bsSearch.main.kts")

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collectIndexed

runBlocking { bsqMain(args) }
suspend fun bsqMain(args: Array<String>) {
    bsSearchMain(
        "世界",
        cardNo = "",
        costMin = 0,
        costMax = 30,
        attr = "",
        category = listOf(),
        system = listOf(),
    ).collectIndexed { index, value ->
        println("$index: $value")
    }
}
