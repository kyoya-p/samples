pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://packages.jetbrains.team/maven/p/amper/amper")
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

plugins {
    id("org.jetbrains.amper.settings.plugin").version("0.7.0-dev-2746")
    // https://mvnrepository.com/artifact/org.jetbrains.amper.settings.plugin/org.jetbrains.amper.settings.plugin.gradle.plugin
}

include("shared")
include("android-app")
include( "ios-app")
include("jvm-app")
