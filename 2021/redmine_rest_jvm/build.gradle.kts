plugins {
    kotlin("jvm") version "1.5.31"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("io.ktor:ktor-client-core-jvm:1.6.4")  // https://mvnrepository.com/artifact/io.ktor/ktor-client-core-jvm
    implementation("io.ktor:ktor-client-cio-jvm:1.6.4") // https://mvnrepository.com/artifact/io.ktor/ktor-client-cio-jvm

}