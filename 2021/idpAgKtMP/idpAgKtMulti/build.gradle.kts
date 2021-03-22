plugins {
    kotlin("multiplatform") version "1.5.0-M1"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js {
        browser {}
        nodejs()
    }

    mingwX64("native") {
        binaries {
            executable()
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.3.1"
    distributionType = Wrapper.DistributionType.ALL
}

