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
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1") // for serialization
        }
    }
}
