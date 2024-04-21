import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version "1.9.23"
    application
}

version = "1.0.0"
application {
    mainClass.set("MainKt")
}
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(project.projectDir.path)
                    }
                }
            }
        }
        binaries.executable()
    }
    
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json
        }
        commonTest.dependencies{
            implementation(kotlin("test"))
        }
        jvmMain.dependencies {
            implementation("com.microsoft.playwright:playwright:1.43.0")  // https://mvnrepository.com/artifact/com.microsoft.playwright/playwright
            implementation("io.ktor:ktor-server-cio:2.3.10") // https://mvnrepository.com/artifact/io.ktor/ktor-server-cio
        }
    }
}
tasks.test {useJUnitPlatform()}

compose.experimental {
    web.application {}
}