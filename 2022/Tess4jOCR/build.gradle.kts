import org.gradle.api.internal.file.archive.ZipFileTree
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.zip.ZipFile

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

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val urlTess4j = "https://sourceforge.net/projects/tess4j/files/latest/download"
val downloadTess by tasks.registering {
    download.run {
        src(urlTess4j)
        dest("$buildDir/libs/tess-src.zip")
    }
    copy {
        from(zipTree("$buildDir/libs/tess-src.zip"))
        into("$buildDir/tess")
    }
}
