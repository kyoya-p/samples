plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("com.microsoft.azure.azurefunctions") version "1.13.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}

azurefunctions {
    subscription = System.getenv("AZ_subscription")
    resourceGroup = "gr1"
    appName = System.getenv("AZ_appName")
//    pricingTier = "" // Default is Consumption plan
    region = "westus"
    setRuntime(closureOf<com.microsoft.azure.gradle.configuration.GradleRuntimeConfig> {
        os("linux")
    })
    setAppSettings(closureOf<MutableMap<String, String>> {
//        put("key", "value")
    })
    setAuth(closureOf<com.microsoft.azure.gradle.auth.GradleAuthConfig> {
        type = "azure_cli"
    })
    // enable local debug
    // localDebug = "transport=dt_socket,server=y,suspend=n,address=5005"
    setDeployment(closureOf<com.microsoft.azure.plugin.functions.gradle.configuration.deploy.Deployment> {
        type = "run_from_blob"
    })
}
