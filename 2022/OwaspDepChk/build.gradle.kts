import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.owasp.dependencycheck") version "7.1.1"
}

group = "jp.wjg.sokkaa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.logging.log4j:log4j-core:2.14.0")
    implementation("org.apache.activemq:activemq-all:5.15.10") // https://mvnrepository.com/artifact/org.apache.activemq/activemq-all
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}