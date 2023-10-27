plugins {
    kotlin("multiplatform") version "1.9.10"
}

repositories {
    mavenCentral()
}

dependencies {
}

kotlin {
    js {
        nodejs{}
        binaries.executable()
    }
}