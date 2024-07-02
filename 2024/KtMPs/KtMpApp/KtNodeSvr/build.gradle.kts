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
        browser {}
    }
    sourceSets {
        jsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
            implementation("dev.gitlive:firebase-firestore:1.13.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore
        }
    }
}

tasks["compileKotlinJs"].doFirst {
    copy {
        from("src/jsMain/kotlin/secret.kt.tmpl")
        into("src/jsMain/kotlin/")
        rename { "secret.kt" }
        val secret = System.getenv("SECRET") ?: throw Exception("'export SECRET=xxx..' before.")
        filter { it.replace("$[SECRET]", secret) }
    }
}
