import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.net.URL

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
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
            implementation(libs.kotlinx.coroutinesSwing)
        }
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


tasks.register("downloadNode") {
    group = "build"
    description = "Downloads Node.js v22.17.0 for Windows x64"
    val downloadUrl = "https://nodejs.org/dist/v22.17.0/node-v22.17.0-win-x64.zip"
    val destinationDir = file("node-v22.17.0-win-x64")
    val zipFile = file("${destinationDir}/node-v22.17.0-win-x64.zip")

    outputs.upToDateWhen { destinationDir.exists() }

    doLast {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
            println("Downloading Node.js from $downloadUrl to $zipFile")
            URL(downloadUrl).openStream().use { input ->
                copy(input, zipFile.toPath(), file.StandardCopyOption.REPLACE_EXISTING)
            }
            println("Node.js downloaded successfully.")
        } else {
            println("Node.js already downloaded.")
        }
    }
}

tasks.register("extractNode") {
    group = "build"
    description = "Extracts the downloaded Node.js v22.17.0 for Windows x64"
    val destinationDir = file("node-v22.17.0-win-x64")
    val zipFile = file("${destinationDir}/node-v22.17.0-win-x64.zip")
    val extractDir = file("nodejs")

    outputs.dir(extractDir)
    inputs.file(zipFile)

    doLast {
        if (!extractDir.exists()) {
            println("Extracting Node.js from $zipFile to $extractDir")
            try {
                ZipFile(zipFile).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        val outputFile = file("$extractDir/${entry.name}")
                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                java.nio.file.Files.copy(input, outputFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                            }
                        }
                    }
                }
                println("Node.js extracted successfully to $extractDir")
            } catch (e: java.util.zip.ZipException) {
                println("Error during extraction: ${e.message}")
                println("Please ensure the downloaded file is a valid ZIP archive.")
            }
        } else {
            println("Node.js already extracted.")
        }
    }
}

tasks.named("build") {
    dependsOn("downloadNode", "extractNode")
}