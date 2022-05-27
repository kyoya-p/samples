plugins {
    application
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.0.0"

    idea
}

val myGroupId = "jp.wjg.shokkaa"
val myArtifactId = "snmp4jutils"
val myVersion = "1.1"

version = myVersion
group = myGroupId

repositories {
    mavenCentral()
    //mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    implementation("org.snmp4j:snmp4j:3.7.0")
    implementation("com.charleskorn.kaml:kaml:0.44.0") // https://github.com/charleskorn/kaml/releases/latest

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("net.java.dev.jna:jna:5.9.0")
    testImplementation("net.java.dev.jna:jna-platform:5.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1-native-mt")
}

application {
    mainClass.set("mypackage.MainKt") // package mypackageかつファイル名main.ktのmain()を実行する場合
}

val compileTestKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    languageVersion = "1.7"
    freeCompilerArgs = listOf("-Xcontext-receivers")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
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
