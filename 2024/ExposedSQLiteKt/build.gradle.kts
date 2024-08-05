plugins {
    kotlin("jvm") version "2.0.0"
    id("com.squareup.sqldelight") version "1.5.5"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.sqldelight:sqlite-driver:1.5.5")
}
kotlin {
    sourceSets.nativeMain.dependencies {
        implementation("app.cash.sqldelight:native-driver:2.0.2")
    }
    sourceSets.jvmMain.dependencies {
        implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
    }
}


application{
    mainClass.set("MainKt")
}
sqldelight {
    database("MyDatabase") {
        packageName = ""
    }
}
