plugins {
    kotlin("jvm") version "2.2.0"
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
}
