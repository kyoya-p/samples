plugins {
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"
    id("org.jetbrains.kotlinx.kover") version "0.4.1"
    `maven-publish`
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")

    implementation("org.snmp4j:snmp4j:3.6.0")
    implementation("com.charleskorn.kaml:kaml:0.37.0") // https://github.com/charleskorn/kaml/releases/latest

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    val myGroupId = "jp.live-on.shokkaa"
    val myArtifactId = "snmp4jutils"
    val myVersion = "1.1"

    publications {
        create<MavenPublication>("maven") {
            groupId = myGroupId
            artifactId = myArtifactId
            version = myVersion
            from(components["kotlin"])
        }
    }
}

kover {
    isEnabled = true                        // false to disable instrumentation of all test tasks in all modules
    coverageEngine.set(kotlinx.kover.api.CoverageEngine.INTELLIJ) // change instrumentation agent and reporter
    intellijEngineVersion.set("1.0.614")    // change version of IntelliJ agent and reporter
    jacocoEngineVersion.set("0.8.7")        // change version of JaCoCo agent and reporter
    generateReportOnCheck.set(true)         // false to do not execute `koverReport` task before `check` task
}

tasks.test {
    extensions.configure(kotlinx.kover.api.KoverTaskExtension::class) {
        isEnabled = true
        binaryReportFile.set(file("$buildDir/custom/result.bin"))
        includes = listOf("com\\.example\\..*")
        excludes = listOf("com\\.example\\.subpackage\\..*")
    }
}
