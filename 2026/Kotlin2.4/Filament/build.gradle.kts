plugins {
    kotlin("jvm") version "2.4.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("practicalfilament.demo.MainKt")
}

tasks.register<JavaExec>("runGui") {
    group = "application"
    description = "Runs the Filament Interactive Desktop GUI Application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("practicalfilament.demo.DesktopApp")
}

tasks.test {
    useJUnitPlatform()
}
