plugins {
    kotlin("jvm") version "1.4.30"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(group = "org.eclipse.paho", name = "org.eclipse.paho.client.mqttv3", version = "1.2.5")
    implementation(group = "com.graphql-java", name = "graphql-java", version = "2021-02-17T06-17-55-6ffdd2d5")

}
