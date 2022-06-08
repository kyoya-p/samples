plugins {
    kotlin("js") version "1.6.21"
}

group = "me.kyoya"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.6.2") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core-js
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.6.2") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core-js
    implementation("org.jetbrains.kotlinx:kotlinx-nodejs:0.0.7")
    implementation(npm("@firebase/firestore", "3.4.9")) // https://www.npmjs.com/package/@firebase/firestore
}

kotlin {
    js(IR) {
        binaries.executable()
        nodejs {

        }
    }
}