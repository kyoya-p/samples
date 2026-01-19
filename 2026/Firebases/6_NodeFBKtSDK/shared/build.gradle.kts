plugins {
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        nodejs()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("test"))
                api("dev.gitlive:firebase-firestore:2.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
