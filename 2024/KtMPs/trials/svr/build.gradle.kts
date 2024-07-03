val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.9"
    id("org.openapi.generator") version "7.4.0" // https://plugins.gradle.org/plugin/org.openapi.generator
}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("ApplicationKt")
//    val isDevelopment: Boolean = project.ext.has("development")
//    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}
sourceSets.main {
    kotlin.srcDir("$buildDir/openapi/generated/petshop/server/src/main")
}

dependencies {
//    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-cio")
//    implementation("ch.qos.logback:logback-classic:$logback_version")
//    testImplementation("io.ktor:ktor-server-tests-jvm")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}


task<GenerateTask>("generatePetshopServerApi") {
    generatorName.set("kotlin-server") // 用途に応じ指定 https://openapi-generator.tech/docs/generators/
    inputSpec.set("$projectDir/openapi.yaml")
    outputDir.set("$buildDir/openapi/generated/petshop/server")
//    apiPackage.set("com.example.yourapp.openapi.generated.controller")
//    modelPackage.set("com.example.yourapp.openapi.generated.model")
    configOptions.set(mapOf("interfaceOnly" to "true"))
    additionalProperties.set(mapOf("useTags" to "true"))
}