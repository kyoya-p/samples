plugins {
    kotlin("multiplatform") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(npm("@google/generative-ai","0.19.0")) // https://www.npmjs.com/package/@google/generative-ai
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
        }
    }
}
