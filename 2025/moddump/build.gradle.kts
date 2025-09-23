plugins {
    kotlin("jvm") version "2.0.20"
    application
//    id("com.github.johnrengelman.shadow") version "7.1.2" // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
    id("org.panteleyev.jpackageplugin") version "1.7.6" // https://plugins.gradle.org/plugin/org.panteleyev.jpackageplugin
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.ghgande:j2mod:3.2.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application {
//    mainClass.set("mypackage.MainKt") // package mypackageかつファイル名main.ktのmain()を実行する場合
    mainClass="MainKt"
}


jpackage {
    mainJar.set(tasks.jar.flatMap { it.archiveFile }) // applicationプラグインのjarタスクを利用
    appName = "YourAppName"
    appVersion = "1.0.0"
    vendor = "Your Company"
    destination = layout.buildDirectory.dir("dist")

    // Windows向け設定
    winMenu = true
    winShortcut = true
}

