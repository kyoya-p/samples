
/*
https://www.apollographql.com/docs/android/essentials/get-started-kotlin/

 */
plugins {
    //java
    kotlin("jvm") version "1.4.30"
    id("com.apollographql.apollo") version "2.5.3" // https://plugins.gradle.org/plugin/com.apollographql.apollo
}

apollo {
    generateKotlinModels.set(true)
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.apollographql.apollo:apollo-runtime:2.5.3") // https://mvnrepository.com/artifact/com.apollographql.apollo/apollo-runtime
    implementation("com.apollographql.apollo:apollo-coroutines-support:2.5.3") // https://mvnrepository.com/artifact/com.apollographql.apollo/apollo-coroutines-support
    //testCompile("junit", "junit", "4.12")
}


