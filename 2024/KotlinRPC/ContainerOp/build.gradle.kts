plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.kotlinx.rpc) apply false
    alias(libs.plugins.compose.compiler) apply false

    id("com.palantir.docker") version "0.36.0" // https://github.com/palantir/gradle-docker
}

// TODO
docker {
    name = project.name
    tag("myRegistry", "my.registry.com/username/my-app:version")
    setDockerfile(File("Dockerfile"))
}
