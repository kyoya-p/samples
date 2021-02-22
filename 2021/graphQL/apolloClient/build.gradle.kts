plugins {
    kotlin("multiplatform") version "1.4.30"
    id("com.apollographql.apollo").version("2.5.3") // plugins.gradle.org/plugin/com.apollographql.apollo
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


kotlin {
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    jvm {
        withJava()
    }
    js {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("com.apollographql.apollo:apollo-runtime-kotlin:2.5.3") //https://search.maven.org/artifact/com.apollographql.apollo/apollo-runtime-kotlin

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}