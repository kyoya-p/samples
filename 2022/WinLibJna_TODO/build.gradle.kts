plugins {
    kotlin("jvm") version "1.6.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("net.java.dev.jna:jna:5.10.0") // https://mvnrepository.com/artifact/net.java.dev.jna/jna
    implementation("net.java.dev.jna:jna-platform:5.10.0")

}