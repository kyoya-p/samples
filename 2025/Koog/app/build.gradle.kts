plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
}

dependencies {
    implementation("ai.koog:koog-agents:0.3.0") // https://mvnrepository.com/artifact/ai.koog/koog-agents
    implementation("org.jetbrains.kotlinx:kotlinx-io:0.1.16") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-io/
}

application.mainClass = "demo3.Main1_SimpleKt"

