plugins {
    kotlin("jvm") version "1.9.21"
    application
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
}
