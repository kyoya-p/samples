plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
}

dependencies {
    implementation("ai.koog:koog-agents:0.3.0") // https://mvnrepository.com/artifact/ai.koog/koog-agents
    implementation(project(":utils"))
}

application.mainClass = "demo3.Main1_SimpleKt"

