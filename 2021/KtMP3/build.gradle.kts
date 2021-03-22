plugins {
    kotlin("multiplatform") version "1.4.31"

}

group = "me.kyoya"
version = "1.0-SNAPSHOT"
val ktor_version = "1.5.2"
val coroutine_version = "1.4.3"

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
                //implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutine_version") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
                implementation("io.ktor:ktor-client-core:$ktor_version") // https://jp.ktor.work/clients/http-client/multiplatform.html
                //implementation("io.ktor:ktor-client-curl:$ktor_version")
                implementation("io.ktor:ktor-client-curl-mingwx64:$ktor_version") // https://mvnrepository.com/artifact/io.ktor/ktor-client-curl-mingwx64
            }
        }
        val nativeTest by getting
    }
}
