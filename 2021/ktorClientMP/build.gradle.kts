plugins {
    //application
    kotlin("multiplatform") version "1.4.31"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    // https://kotlinlang.org/docs/mpp-set-up-targets.html
    mingwX64("mingwX64") {
        binaries { executable() }
        compilations.getByName("main") {
            val libcurl by cinterops.creating {
                defFile = File(projectDir,"posix/cinterop/libcurl.def")
                includeDirs.headerFilterOnly("curl/include/curl")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
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