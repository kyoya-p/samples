import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    `maven-publish`
}

group = "jp.wjg.shokkaa"
version = "1.0.0"

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
            dependencies {
                implementation(compose.desktop.currentOs)
                //implementation("com.bybutter.compose:compose-jetbrains-theme")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "whiteboard"
            packageVersion = "1.0.0"
        }
    }
}

publishing {
    val myArtifactId = "whiteboard"
    val myVersion = "1.0"

    publications {
        create<MavenPublication>("maven") {
            //groupId = group.toString()
            //artifactId = myArtifactId
            //version = myVersion
            //from(components["kotlin"])
        }
    }
}
