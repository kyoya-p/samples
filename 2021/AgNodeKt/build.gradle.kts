plugins {
    id("org.jetbrains.kotlin.js") version "1.4.31"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("io.ktor:ktor-server-js:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-nodejs:0.0.7")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-nodejs

}

kotlin {
    js {
        nodejs {
        }
        binaries.executable()
    }
}