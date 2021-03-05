plugins {
    kotlin("js") version "1.4.31"
    kotlin("plugin.serialization") version "1.4.31"
    id("com.github.node-gradle.node") version "3.0.0-rc5"
}

group = "org.example"
version = "1.0-SNAPSHOT"

val serializationVersion = "1.0.0-RC"
val coroutineVersion = "1.4.3"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("io.ktor:ktor-client-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-nodejs:0.0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-android
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    implementation("dev.gitlive:firebase-auth:1.2.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-auth
    implementation("dev.gitlive:firebase-firestore:1.2.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore

    implementation(npm("firebase", "8.2.6")) // https://www.npmjs.com/package/firebase
    implementation(npm("net-snmp", "2.10.1"))
    implementation(npm("global-agent", "2.1.12")) // https://www.npmjs.com/package/global-agent
}

kotlin {
    js {
        nodejs {}
        binaries.executable()
        useCommonJs() // import NodeJS.set で必要
    }
}