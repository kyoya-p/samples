plugins {
    application
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1" // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
}

val myGroupId = "jp.wjg.shokkaa"
val myArtifactId = "snmp4jutils"
val myVersion = "1.6.2"

version = myVersion
group = myGroupId

repositories {
    mavenCentral()
}

dependencies {
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines:1.7.3") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json/1.6.2
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
    implementation("org.snmp4j:snmp4j:3.7.8") // https://mvnrepository.com/artifact/org.snmp4j/snmp4j
    implementation("com.charleskorn.kaml:kaml:0.57.0") // https://mvnrepository.com/artifact/com.charleskorn.kaml/kaml

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1") // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    testImplementation("net.java.dev.jna:jna:5.14.0") // https://mvnrepository.com/artifact/net.java.dev.jna/jna
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-test
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
            name = "localrepos"
            url = uri(layout.buildDirectory.dir("repos"))
        }
    }
}
