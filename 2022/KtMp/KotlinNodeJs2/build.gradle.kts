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

    implementation(npm("@firebase/app", "0.7.21"))
    implementation(npm("@firebase/firestore", "3.4.8"))
    implementation(npm("@types/node", "12.20.48"))

    testImplementation(kotlin("test"))
}

kotlin {
    js(LEGACY) {
        binaries.executable()
        nodejs {

        }
    }
}