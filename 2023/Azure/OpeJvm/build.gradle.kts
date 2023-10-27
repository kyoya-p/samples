plugins {
    kotlin("jvm") version "1.9.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
// https://mvnrepository.com/artifact/com.microsoft.azure/azure-mgmt-resources
//    implementation("com.microsoft.azure:azure-mgmt-resources:1.41.4")
    implementation("com.azure:azure-identity:1.2.5")

// https://mvnrepository.com/artifact/com.azure.resourcemanager/azure-resourcemanager
    implementation("com.azure.resourcemanager:azure-resourcemanager:2.31.0")
//    implementation("com.azure.resourcemanager:azure-resourcemanager-maintenance:1.0.0")
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}
