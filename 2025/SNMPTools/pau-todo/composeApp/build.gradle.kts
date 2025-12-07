import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization") version "1.9.0" // https://mvnrepository.com/art
    // ifact/org.jetbrains.kotlinx/kotlinx-serialization-json
    id("io.kotest") version "6.0.4"  // https://plugins.gradle.org/plugin/io.kotest
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json

            val ktor_version = "3.3.3" // https://mvnrepository.com/artifact/io.ktor/ktor-client-core
            implementation("io.ktor:ktor-client-cio:$ktor_version")
            implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
            implementation("io.ktor:ktor-serialization-kotlinx-xml:$ktor_version")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.runner.junit5)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.property)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            implementation("org.snmp4j:snmp4j:3.9.6") // https://mvnrepository.com/artifact/org.snmp4j/snmp4j
        }
    }
}


compose.desktop {
    application {
        mainClass = "jp.wjg.shokkaa.snmp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "jp.wjg.shokkaa.snmp"
            packageVersion = "1.0.0"
        }
    }
}
