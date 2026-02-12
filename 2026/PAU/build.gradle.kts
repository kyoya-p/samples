plugins {
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false

    id("org.owasp.dependencycheck") version "12.1.8" // https://plugins.gradle.org/plugin/org.owasp.dependencycheck
    id("org.cyclonedx.bom") version "2.2.0"
}

group = "jp.wjg.shokkaa"
version = "1.0.9"

tasks.withType<org.cyclonedx.gradle.CycloneDxTask> {
    includeConfigs.set(listOf("jvmRuntimeClasspath"))
    skipConfigs.set(listOf("commonMainImplementation", "commonTestImplementation", "metadataCompileClasspath"))
}

