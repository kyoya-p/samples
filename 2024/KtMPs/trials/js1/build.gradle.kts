plugins {
    kotlin("multiplatform") version "1.9.23"
}

//group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    js {
        browser { }
        nodejs {}
        binaries.executable()

    }
//    sourceSets {
//        val jsMain by getting {
//            dependencies {
////                implementation("org.example.myproject:1.1.0")
//            }
//        }
//    }

}

