plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "org.example"
//version = "1.0-SNAPSHOT"
version = ""

repositories {
    mavenCentral()
}

dependencies {
//    implementation("org.apache.logging.log4j:log4j:2.20.0") // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j
    implementation("commons-logging:commons-logging:1.2") // https://mvnrepository.com/artifact/commons-logging/commons-logging
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}