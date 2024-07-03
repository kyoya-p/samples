plugins {
    kotlin("jvm") version "1.8.22"
    id("io.ktor.plugin") version "2.3.1"
}

group = "samples"
version = "0.0.1"
application {
    mainClass.set("RproxyKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

sourceSets["main"].resources.srcDirs("resources")

dependencies {
    val ktor_version = "2.3.1"
    val kotlin_version = "1.8.22"
    val logback_version = "1.2.11"

//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")

    implementation("com.squareup.okio:okio:3.3.0")
    implementation("ch.qos.logback:logback-classic:$logback_version")

//    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}