plugins {
    kotlin("js") version "1.6.21"
}

group = "me.kyoya"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.6.2")
    implementation(npm("net-snmp", "3.8.1")) // https://www.npmjs.com/package/net-snmp
}

kotlin {
    js(LEGACY) {
        binaries.executable()
        nodejs {

        }
    }
}