plugins {
    //application
    kotlin("multiplatform") version "1.4.32" // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-maven-plugin
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://kotlin.bintray.com/kotlinx/")
}

kotlin {
    // https://kotlinlang.org/docs/mpp-set-up-targets.html
    mingwX64("mingwX64") {
        binaries { executable() }
        compilations.getByName("main") {
            val libcurl by cinterops.creating {
                defFile = File(projectDir, "posix/cinterop/libcurl.def")
                includeDirs.headerFilterOnly("curl/include/curl")
            }
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("stdlib-common"))
            }
            enableEndorsedLibs= true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
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