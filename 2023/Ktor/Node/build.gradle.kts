plugins {
    kotlin("js") version "1.8.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
}

val ktor_version = "2.3.0"
val kotlin_version = "1.8.20"

dependencies {
    implementation("io.ktor:ktor-server-js:$ktor_version")
    implementation("io.ktor:ktor-server-core-js:$ktor_version")
//    implementation("io.ktor:ktor-server-content-negotiation-js:$ktor_version")
//    implementation("io.ktor:ktor-server-cio:$ktor_version")

//    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
//    implementation("ch.qos.logback:logback-classic:$logback_version")

//    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-nodejs:0.0.7")
}

kotlin {
    js {
        binaries.executable()
        nodejs {

        }
    }
}