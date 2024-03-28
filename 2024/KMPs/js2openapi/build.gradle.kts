import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

val ktor_version = "2.3.9"

plugins {
    kotlin("multiplatform") version "1.9.23"
//    id("io.ktor.plugin") version "2.3.9" // https://plugins.gradle.org/plugin/io.ktor.plugin
    id("org.openapi.generator") version "7.4.0" // https://plugins.gradle.org/plugin/org.openapi.generator
}

//group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {}
    js {
        browser { }
        nodejs {}
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                compileOnly("io.swagger.core.v3:swagger-annotations:2.2.4")
                compileOnly("io.swagger.core.v3:swagger-models:2.2.4")
//                implementation("io.swagger.codegen.v3:swagger-codegen-generators:2.2.4")
//                implementation("io.ktor:ktor-server-core:$ktor_version")
//                implementation("io.ktor:ktor-server-cio:$ktor_version")
//                implementation("io.ktor:ktor-server-swagger:$ktor_version")
//                implementation("io.ktor:ktor-server-openapi:$ktor_version")
            }
        }
        val jvmMain by getting {
            dependencies {
                val ktor_version = "2.3.9"
                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-server-cio:$ktor_version")
            }
        }

        commonMain.kotlin.srcDir("$buildDir/openapi/generated/petshop/client/src/main")
        commonMain.kotlin.srcDir("$buildDir/openapi/generated/petshop/server/src/main")
    }
//    sourceSets {
//        val jsMain by getting {
//            dependencies {
////                implementation("org.example.myproject:1.1.0")
//            }
//        }
//    }

}


task<GenerateTask>("generatePetshopApiDoc") {
    generatorName.set("html2")
    inputSpec.set("$projectDir/openapi.yaml")
    outputDir.set("$buildDir/openapi/docs/")
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

task<GenerateTask>("generatePetshopClientApi") {
    generatorName.set("kotlin") // 用途に応じ指定 https://openapi-generator.tech/docs/generators/
    inputSpec.set("$projectDir/openapi.yaml")
    outputDir.set("$buildDir/openapi/generated/petshop/client")
//    apiPackage.set("controller")
//    modelPackage.set("model")
    configOptions.set(mapOf("interfaceOnly" to "true"))
    additionalProperties.set(mapOf("useTags" to "true"))
}


/**
 * Kotlinをコンパイルする前に、generateApiServerタスクを実行
 * 必ずスキーマファイルから最新のコードが生成され、もし変更があったらコンパイル時に失敗して気付けるため
 */
//tasks.compileKotlin {
//    dependsOn("generateApiServer")
//}
