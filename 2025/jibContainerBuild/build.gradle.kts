plugins {
    kotlin("jvm") version "2.0.21"
    id("com.google.cloud.tools.jib") version "3.4.4"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

jib {
    from { image = "openjdk:21-jdk-slim" }
    to {
        image = "docker.io/kyoyap/devenv:jibtest"
//        tags = setOf("jibtest")
        auth {
            username = System.getenv("DOCKERHUB_USER")
            password = System.getenv("DOCKERHUB_PASSWORD")
        }
    }
}