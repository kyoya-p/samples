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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.snmp4j:snmp4j:3.5.1")     // https://mvnrepository.com/artifact/org.snmp4j/snmp4j
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}