import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.6.21"
    id("de.undercouch.download") version "5.0.5"  // https://github.com/michel-kraemer/gradle-download-task
}

group = "jp.wjg.shokkaa"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("OpenCVKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-cio:2.0.2") // https://mvnrepository.com/artifact/io.ktor/ktor-client-cio
    implementation("org.bytedeco:javacv:1.5.7") // https://mvnrepository.com/artifact/org.bytedeco/javacv
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val downloadOpenCvLibs by tasks.registering {
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

