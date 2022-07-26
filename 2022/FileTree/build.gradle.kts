import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "jp.wjg.shokkaa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    //implementation("com.github.hazendaz.7zip:7zip:22.01") // https://mvnrepository.com/artifact/com.github.hazendaz.7zip/7zip
    implementation("org.apache.commons:commons-compress:1.21") // https://mvnrepository.com/artifact/org.apache.commons/commons-compress
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}