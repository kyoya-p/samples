import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("de.inetsoftware.setupbuilder") version "7.2.13"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "11" }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                val ktor_version = "2.1.1" // https://mvnrepository.com/artifact/io.ktor/ktor-server-core
                implementation("io.ktor:ktor-server-cio:$ktor_version")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            targetFormats(TargetFormat.Msi)
            packageName = "KtJvm"
            packageVersion = "1.0.0"
        }
    }
}

setupBuilder {
    vendor = "i-net software"
    application = "SetupBuilder Plugin"
    appIdentifier = "SetupBuilder"
    version = "1.0"
    //licenseFile("license.txt")
    // icons in different sizes for different usage. you can also use a single *.ico or *.icns file
//    icons = listOf("icon16.png", "icon32.png", "icon48.png", "icon128.png")
    bundleJre = "$buildDir/compose/binaries/main/app/KtJvm/runtime"
}

tasks.msi {
    minOS = 6.3 // Windows 8.1, Windows Server 2012 R2

    // files only for the Windows platform
    from("$buildDir/compose/binaries/main/app/KtJvm") {
        include("**")
    }
}
