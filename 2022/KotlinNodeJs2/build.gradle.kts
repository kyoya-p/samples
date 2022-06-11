plugins {
    kotlin("js") version "1.6.21"
    //id("kotlin2js") version "1.7.0"
}

group = "me.kyoya"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.6.2")
    implementation(npm("net-snmp", "3.8.1")) // https://www.npmjs.com/package/net-snmp
    testImplementation(kotlin("test"))
}

kotlin {
    js(LEGACY) {
        binaries.executable()
        nodejs {

        }
    }
}