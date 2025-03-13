plugins {
    kotlin("jvm") version "2.1.10"
//    kotlin("multiplatform") version "2.1.10"
//    id("io.kotest") version "6.0.0.M2" // https://plugins.gradle.org/plugin/io.kotest
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

//group = "smpl.reflection"
version = "1.0-SNAPSHOT" // 必要に応じてバージョンを設定

application { mainClass.set("MainKt") }

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.reflections:reflections:0.10.2") // https://mvnrepository.com/artifact/org.reflections/reflections
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}