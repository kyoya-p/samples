plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js {
        moduleName = "libJs"
        browser { }
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation("dev.gitlive:firebase-auth:1.13.0")
        }
    }
}


