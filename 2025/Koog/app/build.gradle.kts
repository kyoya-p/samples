plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
}

dependencies {
    implementation("ai.koog:koog-agents:0.2.1")
    implementation(project(":utils"))
}

application.mainClass = "demo3.Main3Kt"

