plugins {
    kotlin("js") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
}

group = "me.kyoya"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.6.21")
    //implementation("org.jetbrains.kotlin:kotlin-serialization-js:1.6.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.6.2")
    implementation(npm("net-snmp", "3.8.1", generateExternals = true)) // https://www.npmjs.com/package/net-snmp
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-js:1.6.2")
}

kotlin {
    js(LEGACY) {
        binaries.executable()
        nodejs {

        }
    }
}