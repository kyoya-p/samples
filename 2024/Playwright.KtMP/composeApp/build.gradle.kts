import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
//    alias(libs.plugins.ktor)
}

repositories { mavenCentral() }
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
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

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(projects.shared)

//            val ktor_version="2.3.10" // https://mvnrepository.com/artifact/io.ktor/ktor-server-core
//            implementation("io.ktor:ktor-client-core-js:$ktor_version")
//            implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
//            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
        }

    }
}

compose.experimental {
    web.application {}
}