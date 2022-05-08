import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.lang.System.*

plugins {
    application
    kotlin("jvm") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "7.1.2" // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
    id("de.undercouch.download") version "5.0.5"  // https://github.com/michel-kraemer/gradle-download-task
}

group = "jp.wjg.shokkaa"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("RtMakeKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.bytedeco:opencv-platform:4.5.1-1.5.5")// https://mvnrepository.com/artifact/org.bytedeco/opencv-platform
    implementation("com.quickbirdstudios:opencv-contrib:3.4.5") // https://mavenlibs.com/maven/dependency/com.quickbirdstudios/opencv-contrib
    implementation("org.jetbrains.kotlin:kotlin-script-util:1.6.21") // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-script-util
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaExec> {
    systemProperty("java.library.path", getProperty("java.library.path"))
}

val downloadFiles by tasks.registering {
    doLast {
        download.run {
            src("https://sourceforge.net/projects/opencvlibrary/files/4.5.5/opencv-4.5.5-vc14_vc15.exe/download")
            dest("$buildDir/libs/opencv.exe")
        }
        exec {
            commandLine("$buildDir/libs/opencv.exe", "-o$buildDir", "-y")
        }
        copy {
            from(fileTree("$buildDir/opencv/build/java/x64/"))
            into("$projectDir")
        }
        copy {
            from(fileTree("$buildDir/opencv/build/x64/vc15/bin"))
            include { it.name.endsWith(".dll") }
            into("$projectDir")
        }
        download.run {
            src("https://pbs.twimg.com/media/FRaETtQVsAAoX4S?format=jpg&name=large")
            dest("$buildDir/samples/s1.jpg")
        }
    }
}

