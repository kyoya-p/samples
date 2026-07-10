plugins {
    kotlin("jvm") version "2.4.0"
    id("de.infix.testBalloon") version "1.0.1-K2.4.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("de.infix.testBalloon:testBalloon-framework-core:1.0.1-K2.4.0")
    implementation("io.kotest:kotest-assertions-core:5.9.1")
    implementation(kotlin("test"))
}
