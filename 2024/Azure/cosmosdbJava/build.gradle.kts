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
    implementation("org.mongodb:mongo-java-driver:3.12.14") // https://mvnrepository.com/artifact/org.mongodb/mongo-java-driver
}
