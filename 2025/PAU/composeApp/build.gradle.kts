import io.kotest.framework.gradle.tasks.KotestJvmTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization") version "1.9.0" // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json
    id("io.kotest") version "6.0.4"  // https://plugins.gradle.org/plugin/io.kotest
}

kotlin {
    jvm()
//    js { browser(); binaries.executable() }
//    @OptIn(ExperimentalWasmDsl::class)
//    wasmJs { browser(); binaries.executable() }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            val ktor_version = "3.3.1" // https://mvnrepository.com/artifact/io.ktor/ktor-client-core
            implementation("io.ktor:ktor-client-cio:$ktor_version")
            implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
            implementation("io.ktor:ktor-serialization-kotlinx-xml:$ktor_version")

//            implementation("org.jetbrains.kotlin:kotlin-serialization-json:1.9.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("org.snmp4j:snmp4j:3.9.6")
        }
        commonTest.dependencies {
            val kotest_version = "6.0.5" // https://mvnrepository.com/artifact/io.kotest/kotest-framework-engine
            implementation(kotlin("test"))
            implementation("io.kotest:kotest-framework-engine:$kotest_version")
            implementation("io.kotest:kotest-assertions-core:$kotest_version")
            implementation("io.kotest:kotest-runner-junit5:$kotest_version")
            implementation("io.kotest:kotest-extensions-htmlreporter:$kotest_version")
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PAU"
            packageVersion = "1.0.9"
            windows {
                upgradeUuid = "b7c7a509-b6ea-0554-90a5-217cf641e5cd"
                menu = true
                shortcut = true
            }
        }
    }
}
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
