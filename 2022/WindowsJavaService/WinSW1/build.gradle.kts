import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2" // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
    id("de.undercouch.download") version "5.3.0" //https://plugins.gradle.org/plugin/de.undercouch.download
}
group = "org.example"
version = "1.0-SNAPSHOT"
application { mainClass.set("MainKt") }
repositories { mavenCentral() }

dependencies {
    val ktor_version = "2.1.2" // https://mvnrepository.com/artifact/io.ktor/ktor-server-core
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-cio:$ktor_version")
}

val downloadWinSW by tasks.registering {
    doLast {
        download.run {
            src("https://github.com/winsw/winsw/releases/download/v2.11.0/WinSW.NET4.exe")
server        }
        copy {
            into("$buildDir/libs")
            from("$buildDir/WinSW.NET4.exe")
            rename { "WinSW1.exe" }
        }
        copy {
            into("$buildDir/libs")
            from("$projectDir/src/main/resources/WinSW1.xml")
        }
    }
}

tasks["build"].doLast{
    download.run {
        src("https://github.com/winsw/winsw/releases/download/v2.11.0/WinSW.NET4.exe")
        dest(buildDir)
    }
    copy {
        into("$buildDir/libs")
        from("$buildDir/WinSW.NET4.exe")
        rename { "WinSW1.exe" }
    }
    copy {
        into("$buildDir/libs")
        from("$projectDir/src/main/resources/WinSW1.xml")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

