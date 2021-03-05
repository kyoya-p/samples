plugins {
    kotlin("js") version "1.4.31"
    kotlin("plugin.serialization") version "1.4.31"
}

group = "org.example"
version = "1.0-SNAPSHOT"

val serializationVersion = "1.0.0-RC"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("io.ktor:ktor-client-core:1.5.2")
    //implementation("io.ktor:ktor-client-js:1.5.2")
    //implementation("io.ktor:ktor-servers:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-nodejs:0.0.7")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-nodejs
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")


    implementation("dev.gitlive:firebase-auth:1.2.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-auth
    implementation("dev.gitlive:firebase-firestore:1.2.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore

}

kotlin {
    js {
        nodejs {}
        binaries.executable()
        useCommonJs() // import NodeJS.set で必要
    }
}