
plugins {
    kotlin("multiplatform") version "2.0.0"
//    kotlin("plugin.serialization") version "2.0.0"
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        nodejs { }
        binaries.executable()
//        browser { }
    }

    sourceSets {
        jsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core

            implementation("dev.gitlive:firebase-firestore:1.13.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore
            implementation(npm("child_process", "1.0.2"))
            implementation(npm("typescript", "5.5.3"))
            implementation(npm("@types/node", "20.14.10"))

//            implementation(npm("execa", "9.3.0"))
        }
    }
}
//tasks.withType<KotlinJsCompile>().configureEach {
//    kotlinOptions {
//        target = "es2015"
//    }
//}
