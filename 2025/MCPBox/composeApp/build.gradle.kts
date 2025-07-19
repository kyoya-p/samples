import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization") version "2.2.0"
}



kotlin {
    jvm("desktop")

    sourceSets {
        val ktor_version = "3.2.1"

        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
//            implementation(libs.kotlinx.coroutinesSwing)

            implementation("androidx.datastore:datastore:1.1.7")
            implementation("androidx.datastore:datastore-preferences:1.1.7")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${ktor_version}")
            implementation("io.ktor:ktor-client-cio:${ktor_version}")
            implementation("io.ktor:ktor-client-okhttp:${ktor_version}")
            implementation("io.ktor:ktor-network-tls-certificates:${ktor_version}")
            implementation("ai.koog:koog-agents:0.3.0") // https://mvnrepository.com/artifact/ai.koog/koog-agents
        }
    }
    compilerOptions {
//        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

compose.desktop {
    application {
        mainClass = "jp.wjg.shokkaa.mcp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "jp.wjg.shokkaa.mcp"
            packageVersion = "1.0.0"
        }
    }
}

//val nodeJsVersion = "22.17.0-win-x64"
//val nodeJsDownloadUrl = "https://nodejs.org/dist/v22.17.0/node-v$nodeJsVersion.zip"
//val nodeJsArchivePath = layout.buildDirectory.file("nodejs-v$nodeJsVersion.zip")
//tasks.register("downloadNodeJs") {
//    doLast {
//        println("Downloading Node.js from $nodeJsDownloadUrl to $nodeJsArchivePath")
//        nodeJsArchivePath.get().asFile.parentFile.mkdirs()
////        Url(nodeJsDownloadUrl). { input ->
////            nodeJsArchivePath.get().asFile.outputStream().use { output ->
////                input.copyTo(output)
////            }
////        }
//        println("Node.js downloaded successfully.")
//    }
//}
