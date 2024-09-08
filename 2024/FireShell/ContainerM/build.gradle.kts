import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
}

repositories {
//    google()
    mavenCentral()
}

//tasks.withType<KotlinCompile> {
//    kotlinOptions.freeCompilerArgs = listOf("-Xcontext-receivers")
//}

kotlin {
    js {
        browser {}
        binaries.executable()

    }
    sourceSets {
        jsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
            implementation("dev.gitlive:firebase-firestore:1.13.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore
            implementation("dev.gitlive:firebase-auth:1.13.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore
            implementation("org.jetbrains.kotlinx:kotlinx-html:0.11.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-html
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
            implementation(project(":shared"))
        }
    }
}

//tasks["compileKotlinJs"].doFirst {
//    copy {
//        from("src/jsMain/kotlin/Properties.kt.tmpl")
//        into("src/jsMain/kotlin/")
//        rename { "Properties.kt" }
//        filter { it.replace("$[APPKEY]",
//            System.getenv("APPKEY")
//                ?: throw Exception("Set the App-key of Firebase in the APPKEY environment variable.")
//        )
//        }
//    }
//}
