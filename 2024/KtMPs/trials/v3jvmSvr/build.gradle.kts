plugins {
    application
    kotlin("jvm") version "1.9.23" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
    kotlin("plugin.serialization") version "1.9.23" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
}

application {
//    mainClass.set("io.ktor.server.cio.EngineMain")

    mainClass.set("ApplicationKt")
}

repositories {
    mavenCentral()
//    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
//    implementation("io.ktor:ktor-server-core:$ktor_version")
//    implementation("io.ktor:ktor-server-netty:$ktor_version")
//    implementation("io.ktor:ktor-server-swagger:$ktor_version")
//    implementation("io.ktor:ktor-server-openapi:$ktor_version")
//    implementation("io.ktor:ktor-server-cors:$ktor_version")
//    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
//    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
//    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
//    implementation("io.swagger.codegen.v3:swagger-codegen-generators:$swagger_codegen_version")
//    implementation("ch.qos.logback:logback-classic:$logback_version")
//    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
