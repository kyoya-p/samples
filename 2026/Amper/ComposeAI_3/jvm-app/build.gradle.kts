plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm {
        mainRun {
            mainClass.set("MainKt")
        }
    }

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.desktop.currentOs)
            implementation("androidx.lifecycle:lifecycle-runtime:2.8.7")
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
            implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
        }

        // Amper directory mapping
        jvmMain {
            kotlin.srcDir("src")
            resources.srcDir("resources")
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "ComposeAI_3"
            packageVersion = "1.0.0"
            
            windows {
                menu = true
                iconFile.set(project.file("icon.ico"))
            }
        }
    }
}
