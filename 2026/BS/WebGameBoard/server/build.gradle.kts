plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared-model"))
                implementation("io.ktor:ktor-server-core:3.0.3")
                implementation("io.ktor:ktor-server-netty:3.0.3")
                // 正しいアーティファクト名に修正、あるいは標準機能を使用
                // implementation("io.ktor:ktor-server-static-content:3.0.3") 
                implementation("io.ktor:ktor-server-content-negotiation:3.0.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
                implementation("ch.qos.logback:logback-classic:1.5.6")
            }
        }
    }
}

tasks.create<JavaExec>("runServer") {
    group = "application"
    mainClass.set("MainKt")
    val jvmMainCompilation = kotlin.jvm().compilations.getByName("main")
    classpath = jvmMainCompilation.output.allOutputs + jvmMainCompilation.runtimeDependencyFiles
    workingDir = rootProject.projectDir
}
