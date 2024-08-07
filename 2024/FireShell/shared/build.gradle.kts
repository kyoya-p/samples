plugins {
    kotlin("multiplatform") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        nodejs { }
        browser { }
    }
    sourceSets {
        jsMain.dependencies {
            implementation("dev.gitlive:firebase-auth:1.13.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore
            implementation("dev.gitlive:firebase-firestore:1.13.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
        }
    }
}
