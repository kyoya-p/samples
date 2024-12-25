plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.kotlinx.rpc) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("com.google.cloud.tools.jib") version "3.4.4"
}

val buildAppWebTask = tasks.create("buildAppWeb") {
    group = "container op"
    dependsOn("server:shadowJar")
    dependsOn("composeApp:wasmJsBrowserDistribution")
    doLast {
        copy {
            into("build/appWeb")
            from(
                "server/build/libs/server-all.jar",
                "composeApp/build/dist/wasmJs/productionExecutable"
            )
        }
    }
}

val runAppWebTask = tasks.create("runAppWeb") {
    group = "container op"
    dependsOn(buildAppWebTask)
    doLast {
        exec {
            this.workingDir = File("build/appWeb")
            commandLine("java", "-jar", "server-all.jar")
        }
    }
}

