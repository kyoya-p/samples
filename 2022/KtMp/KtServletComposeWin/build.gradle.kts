import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
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
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            val ktor_version="2.0.3"
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-server-netty:$ktor_version")
                implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
                implementation("io.ktor:ktor-network-tls-certificates:1.6.7") // TLSサポート
                implementation("io.ktor:ktor-auth:1.6.7") // Basic認証,Digest認証等

                implementation("commons-daemon:commons-daemon:1.3.1") // https://mvnrepository.com/artifact/commons-daemon/commons-daemon
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb,TargetFormat.Exe)
            targetFormats(TargetFormat.Msi)
            packageName = "KtComposeDesktopWin"
            packageVersion = "1.0.0"

            // TODO
            windows {
                // this.exePackageVersion = "1.9.9"
//                this.msiPackageVersion="1.99.0"
                this.perUserInstall = true
            }
        }
    }
}
