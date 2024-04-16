plugins {
    kotlin("jvm") version "1.9.23"
}
repositories { mavenCentral() }
dependencies {
    implementation("com.microsoft.playwright:playwright:1.43.0")  // https://mvnrepository.com/artifact/com.microsoft.playwright/playwright
    testImplementation(kotlin("test"))
}

