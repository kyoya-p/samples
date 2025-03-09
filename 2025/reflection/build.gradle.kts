plugins {
    kotlin("multiplatform") version "2.1.10"
    id("io.kotest") version "6.0.0.M2" // https://plugins.gradle.org/plugin/io.kotest
}

//group = "org.example"
//version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    jvm()

    sourceSets {
        val coroutine_version = "1.10.1"  // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines
        val kotest_version = "5.9.1"  // https://mvnrepository.com/artifact/io.kotest/kotest-framework-api
//        val commonMain by getting
        jvmMain.dependencies {
//            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutine_version")
            implementation(kotlin("reflect"))
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.kotest:kotest-framework-engine:$kotest_version")
            implementation("io.kotest:kotest-assertions-core:$kotest_version")
            implementation("io.kotest:kotest-property:$kotest_version")
            implementation("io.kotest:kotest-extensions-jvm:$kotest_version")
            implementation("io.kotest:kotest-runner-junit5:$kotest_version")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

