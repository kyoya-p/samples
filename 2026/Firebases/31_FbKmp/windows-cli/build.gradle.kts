plugins {
    kotlin("multiplatform")
}

kotlin {
    mingwX64 {
        binaries.executable {
            entryPoint = "com.example.main"
        }
    }

    sourceSets {
        val mingwX64Main by getting {
            kotlin.srcDir("src/mingwX64Main/kotlin")
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}
