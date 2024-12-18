plugins {
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
//    id("io.ktor.plugin") version "3.0.2" // https://plugins.gradle.org/plugin/io.ktor.plugin
    id("org.openapi.generator") version "7.10.0" // https://plugins.gradle.org/plugin/org.openapi.generator
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val buildDir = projectDir.resolve("build")

kotlin {
    js {
        nodejs { }
        binaries.executable()
    }
    jvm()
    sourceSets {
        val ktor_version = "3.0.2"
        val commonMain by getting {
            dependencies {
//                compileOnly("io.swagger.core.v3:swagger-annotations:2.2.4")
//                compileOnly("io.swagger.core.v3:swagger-models:2.2.4")
//                implementation("io.swagger.codegen.v3:swagger-codegen-generators:2.2.4")
                implementation("io.swagger:swagger-codegen:3.0.0-rc1")

//                implementation("io.ktor:ktor-server-swagger:$ktor_version")
//                implementation("io.ktor:ktor-server-openapi:$ktor_version")
//                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
            }
        }
        commonMain.kotlin.srcDir("$buildDir/generated/src/main")

        jvmMain {
            dependencies {
                implementation("io.ktor:ktor-client-cio:$ktor_version")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktor_version")
            }
        }
    }
}

val taskGenerateApi = task<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateClientApi") {
    group = "openapi tools"
    generatorName.set("kotlin")
    inputSpec.set("$rootDir/apis/gemini.yaml")
    outputDir.set("$rootDir/build/generated")
    apiPackage.set("org.example.gemini.api") // 生成されるAPIのパッケージ名
    modelPackage.set("org.example.gemini.model") // 生成されるモデルのパッケージ名
    configOptions.set(
        mapOf(
            "dateLibrary" to "kotlinx-datetime",
//            "serializationLibrary" to "kotlinx_serialization", //[BUG]この行があると誤った@Serialisableアノテーションが生成される
            "useCoroutines" to "true",
            "library" to "multiplatform"
        )
    )
}

tasks.named("compileKotlinJvm") {
    dependsOn(taskGenerateApi)
}
