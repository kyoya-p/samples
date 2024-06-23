plugins {
    kotlin("multiplatform") version "2.0.0"
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        browser {}
        binaries.executable()
    }
}
