import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    //id("io.freefair.compress.7z") version "6.4.3"  // https://plugins.gradle.org/plugin/io.freefair.compress.7z
    id("de.undercouch.download") version "5.0.5"  // https://github.com/michel-kraemer/gradle-download-task
}

group = "jp.wjg.shokkaa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


dependencies {
    implementation("org.bytedeco:javacv:1.5.7") // https://mvnrepository.com/artifact/org.bytedeco/javacv
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val uriOpenCV = "https://sourceforge.net/projects/opencvlibrary/files/4.5.5/opencv-4.5.5-vc14_vc15.exe/download"

val downloadOpenCV = tasks.register("downloadOpenCV", de.undercouch.gradle.tasks.download.Download::class) {
    src(uriOpenCV)
    dest("$buildDir/libs/opencv.exe")
}

val extractOpenCV by tasks.registering {
    doLast {
        exec {
            commandLine("$buildDir/libs/opencv.exe", "-o$buildDir", "-y")
        }
        copy {
            from(fileTree("$buildDir/opencv/build/java/x64/"))
            into("$projectDir")
        }
    }
    dependsOn(downloadOpenCV)
}

