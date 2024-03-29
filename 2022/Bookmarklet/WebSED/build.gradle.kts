plugins {
    kotlin("js") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "me.kyoya"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    //maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:1.0-M1-1.4.0-rc")

    // https://github.com/SheetJS/sheetjs
    implementation("org.webjars.bowergithub.sheetjs:sheetjs:0.17.5") // https://mvnrepository.com/artifact/org.webjars.bowergithub.sheetjs/sheetjs

    //testImplementation(kotlin("test"))
    //implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
}

tasks["build"].doLast {
    copy {
        from("$buildDir/distributions")
        into("$projectDir/dist")
    }
}

tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.INHERIT }

