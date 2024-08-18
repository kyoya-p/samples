plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js {
        moduleName = "libJs"
        browser { }
        binaries.executable()
    }
    sourceSets {    }
}


