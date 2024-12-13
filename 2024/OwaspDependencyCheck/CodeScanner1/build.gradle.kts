plugins {
    kotlin("jvm") version "2.1.0"
    application
    kotlin("plugin.serialization") version "2.1.0" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization

    id("org.owasp.dependencycheck") version "11.1.0" // https://plugins.gradle.org/plugin/org.owasp.dependencycheck
    id("org.cyclonedx.bom") version "1.10.0" // https://plugins.gradle.org/plugin/org.cyclonedx.bom
}

group = "com.security"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // 以下検証用依存モジュール(脆弱性注意)
    implementation("org.apache.logging.log4j:log4j-core:2.14.0")
    implementation("log4j:log4j:1.2.17")
    implementation("org.bouncycastle:bcprov-jdk15on:1.59")
}

application {
    mainClass.set("MainKt")
}

tasks.named("run") {
    dependsOn("cyclonedxBom")
    dependsOn("dependencyCheckAnalyze")
}

//cyclonedxBom {
//    //declaration of the Object from OrganizationalContact
//    var organizationalContact1 = OrganizationalContact()
//    //setting the Name[String], Email[String] and Phone[String] of the Object
//    organizationalContact1.setName("Max_Mustermann")
//    organizationalContact1.setEmail("max.mustermann@test.org")
//    organizationalContact1.setPhone("0000 99999999")
//    //passing Data to the plugin
//    setOrganizationalEntity{oe->
//        oe.name = "Test";
//        oe.urls = listOf("www.test1.com", "www.test2.com")
//        oe.addContact(organizationalContact1)
//    }
//}

