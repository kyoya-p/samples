/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.kotlinx.rpc) apply false
    alias(libs.plugins.compose.compiler) apply false

    id("com.palantir.docker") version "0.36.0" // https://github.com/palantir/gradle-docker
    id("org.owasp.dependencycheck") version "11.1.0" // https://plugins.gradle.org/plugin/org.owasp.dependencycheck
}

dependencyCheck {
    format = "ALL"
    failBuildOnCVSS = 8.5f
//    scanSet = listOf(projectDir.resolve("server/src/main"))
    nvd { apiKey = System.getenv("NVD_API_KEY") }
}

// TODO
docker {
    name = project.name
    tag("myRegistry", "my.registry.com/username/my-app:version")
    setDockerfile(File("Dockerfile"))
}
