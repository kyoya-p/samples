plugins {
    kotlin("jvm") version "1.4.30"
    id("com.apollographql.apollo").version("2.5.4") // https://plugins.gradle.org/plugin/com.apollographql.apollo
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(group = "org.eclipse.paho", name = "org.eclipse.paho.client.mqttv3", version = "1.2.5")
    implementation("com.apollographql.apollo:apollo-runtime:2.5.3")
    implementation("android.arch.work:work-runtime:1.0.1") // https://mvnrepository.com/artifact/com.apollographql.apollo/apollo-runtime
    implementation("io.ktor:ktor-client-cio:1.5.1") //https://mvnrepository.com/artifact/io.ktor/ktor-client-cio
}
