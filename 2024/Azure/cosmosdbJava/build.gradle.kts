plugins {
    kotlin("jvm") version "1.9.21"
    application
    kotlin("plugin.serialization") version "2.0.0"
}
repositories{
    mavenCentral()
}
application{
    mainClass="MainKt"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.mongodb:mongo-java-driver:3.12.14") // https://mvnrepository.com/artifact/org.mongodb/mongo-java-driver
    implementation("com.squareup.okio:okio:3.9.0") // https://mvnrepository.com/artifact/com.squareup.okio/okio
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
