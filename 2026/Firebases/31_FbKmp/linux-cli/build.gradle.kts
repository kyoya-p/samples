
plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64 {
        binaries.executable {
            entryPoint = "com.example.main"
        }
    }

    sourceSets {
        val linuxX64Main by getting {
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}
