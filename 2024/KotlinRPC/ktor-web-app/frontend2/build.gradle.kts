/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {
    js(IR) {
        binaries.executable()

        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }

    sourceSets {
        jsMain {
            dependencies {
                implementation(projects.common)

                implementation(libs.kotlin.stdlib.js)
                implementation(libs.ktor.client.js)
                implementation(libs.ktor.client.websockets.js)
                implementation(libs.kotlinx.rpc.krpc.ktor.client)
                implementation(libs.kotlinx.rpc.krpc.serialization.json)

//                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
                implementation("org.jetbrains.kotlinx:kotlinx-html:0.11.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-html
//                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime

                implementation(project.dependencies.platform(libs.kotlin.wrappers.bom))
                implementation(libs.react)
                implementation(libs.react.dom)
                implementation(libs.emotion)
            }
        }
    }
}
