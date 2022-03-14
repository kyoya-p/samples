plugins {
//    kotlin("jvm") version "1.3.71"
    kotlin("jvm") version "1.6.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    //implementation("junit:junit:4.13.2")
    //testImplementation(kotlin("test"))
}
//tasks.test { useJUnitPlatform() }
