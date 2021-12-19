plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.5.31"
    id("org.jetbrains.kotlinx.kover") version "0.4.1"
    `maven-publish`
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")

    implementation("org.snmp4j:snmp4j:3.6.2")
    implementation("com.charleskorn.kaml:kaml:0.37.0") // https://github.com/charleskorn/kaml/releases/latest

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")

    // implementation("jp.live-on.shokkaa:snmp4jutils:1.1") // Usage sample


    testImplementation("net.java.dev.jna:jna:5.9.0")
    testImplementation("net.java.dev.jna:jna-platform:5.9.0")

}


tasks.test {
    useJUnitPlatform()
}

publishing {
    val myGroupId = "jp.pgw.shokkaa"
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

    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}
