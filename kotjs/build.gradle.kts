buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.3.72"))
    }
}
plugins {
    kotlin("js") version "1.3.72"
}
dependencies {
    implementation(kotlin("stdlib-js"))
}

kotlin.target {
    nodejs()
    browser()
}

tasks {
    compileKotlinJs {
        kotlinOptions {
            moduleKind = "umd"
        }
    }
}
