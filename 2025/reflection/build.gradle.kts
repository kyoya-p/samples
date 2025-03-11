plugins {
    kotlin("multiplatform") version "2.1.10"
    id("io.kotest") version "6.0.0.M2" // https://plugins.gradle.org/plugin/io.kotest
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(kotlin("reflect"))
            implementation("org.reflections:reflections:0.10.2") // https://mvnrepository.com/artifact/org.reflections/reflections
        }
    }
}

