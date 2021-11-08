plugins {
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"
    `maven-publish`
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")

    implementation("org.snmp4j:snmp4j:3.5.1")     // https://mvnrepository.com/artifact/org.snmp4j/snmp4j
}

publishing {
    val myGroupId = "jp.live-on.shokkaa"
    val myArtifactId = "snmp4jutils"
    val myVersion = "1.1"

    publications {
        create<MavenPublication>("maven") {
            groupId = myGroupId
            artifactId = myArtifactId
            version = myVersion
            from(components["kotlin"])
        }
    }
}
