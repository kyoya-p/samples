plugins {
    kotlin("multiplatform")
}

kotlin {
    js(IR) {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                implementation(npm("firebase", "10.7.1"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}