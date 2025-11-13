plugins {
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false

    id("org.owasp.dependencycheck") version "12.1.8" // https://plugins.gradle.org/plugin/org.owasp.dependencycheck
}