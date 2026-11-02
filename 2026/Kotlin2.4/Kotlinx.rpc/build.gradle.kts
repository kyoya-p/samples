plugins {
    kotlin("multiplatform") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    id("org.jetbrains.kotlinx.rpc.plugin") version "0.11.0-grpc-189"
}

repositories {
    mavenCentral()
    maven("https://redirector.kotlinlang.org/maven/kxrpc-grpc")
}

val rpcVersion = "0.11.0-grpc-189"

kotlin {
    jvmToolchain(21)
    jvm()

    // Supported Native targets for kotlinx.rpc gRPC
    linuxX64()
    macosArm64()
    iosArm64()

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-rpc-grpc-server:$rpcVersion")
            api("org.jetbrains.kotlinx:kotlinx-rpc-grpc-client:$rpcVersion")
            api("org.jetbrains.kotlinx:kotlinx-rpc-grpc-marshaller-kotlinx-serialization:$rpcVersion")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
        }
        jvmMain.dependencies {
            runtimeOnly("io.grpc:grpc-netty-shaded:1.81.0")
        }
    }

    compilerOptions {
        optIn.addAll(
            "kotlinx.rpc.internal.utils.ExperimentalRpcApi",
            "kotlinx.rpc.internal.utils.InternalRpcApi"
        )
    }
}

val mainOutput = kotlin.targets.getByName("jvm").compilations.getByName("main").output.allOutputs
val jvmClasspath = configurations.getByName("jvmRuntimeClasspath")

tasks.register<JavaExec>("runServer") {
    classpath = mainOutput + jvmClasspath
    mainClass.set("chat.server.ServerAppKt")
}

tasks.register<JavaExec>("runClient") {
    classpath = mainOutput + jvmClasspath
    mainClass.set("chat.client.ClientAppKt")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runClientAuto") {
    classpath = mainOutput + jvmClasspath
    mainClass.set("chat.client.ClientAppKt")
    args = listOf("--auto")
}
