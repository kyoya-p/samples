import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    id("de.undercouch.download") version "5.0.5"  // https://github.com/michel-kraemer/gradle-download-task
}

group = "jp.wjg.shokkaa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.sourceforge.tess4j:tess4j:5.2.1") // https://mvnrepository.com/artifact/net.sourceforge.tess4j/tess4j
    implementation("org.slf4j:slf4j-simple:1.7.36") // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val downloadTess by tasks.registering {
    doLast {
        download.run {
            src("https://sourceforge.net/projects/tess4j/files/latest/download")
            dest("$buildDir/libs/tess-src.zip")
        }
        copy {
            from(zipTree("$buildDir/libs/tess-src.zip"))
            into("$buildDir/tess")
        }
        download.run {
            src("https://github.com/tesseract-ocr/tessdata/raw/main/jpn.traineddata")
            dest("$buildDir/tess/Tess4J/tessdata/")
        }
        download.run {
            src("https://pbs.twimg.com/media/FRaETtQVsAAoX4S?format=jpg&name=large")
            dest("$buildDir/samples/s1.jpg")
        }
    }
}

val ocrRun by tasks.registering {
    doLast {
//        exec {
//            environment("TESSDATA_PREFIX" to "build/tess/Tess4J/tessdata")
//            commandLine("java.exe","TessMainKt")
//        }
        javaexec {
            environment("TESSDATA_PREFIX" to "$buildDir/tess/Tess4J/tessdata")
            mainClass.set("TessMainKt")
        }
    }
    dependsOn(tasks.build)
}