plugins {
    kotlin("jvm") version "1.6.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1") // https://mvnrepository.com/artifact/io.ktor/ktor-client-apache
    implementation("io.ktor:ktor-client-cio:1.6.5")
    implementation("io.ktor:ktor-client-apache:1.6.5")
    implementation("io.ktor:ktor-network-tls-certificates:1.6.5")
}

tasks.test {
    useJUnitPlatform()
}
