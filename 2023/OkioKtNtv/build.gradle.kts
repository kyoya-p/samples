plugins {
    kotlin("multiplatform") version "1.8.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        val nativeMain by getting {
            dependencies {
                // https://square.github.io/okio/3.x/okio/okio/okio/
                implementation("com.squareup.okio:okio:3.3.0") // https://mvnrepository.com/artifact/com.squareup.okio/okio
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
            }
        }
    }
}
