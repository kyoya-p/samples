import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    id("de.undercouch.download") version "5.1.0"  // https://plugins.gradle.org/plugin/de.undercouch.download
}

group = "jp.wjg.shokkaa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.deeplearning4j:deeplearning4j-core:1.0.0-M2")//https://mvnrepository.com/artifact/org.deeplearning4j/deeplearning4j-core
    implementation("org.nd4j:nd4j-native-platform:1.0.0-M2") //https://mvnrepository.com/artifact/org.nd4j/nd4j-native-platform
    implementation("org.slf4j:slf4j-jdk14:1.7.36") //https://mvnrepository.com/artifact/org.slf4j/slf4j-jdk14
    implementation("org.nd4j:nd4j-api:1.0.0-M2") //https://mvnrepository.com/artifact/org.nd4j/nd4j-api
    implementation("jp.wjg.shokkaa:whiteboard-jvm:1.0.0") // local

    //testImplementation(kotlin("test"))
    implementation(kotlin("stdlib-jdk8"))
}

//tasks.test { useJUnitPlatform() }
//tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }


val downloadDataset by tasks.registering {
    doLast {
        val dataFile = "$buildDir/dataset/mnist_png.tar.gz"
        download.run {
            src("https://github.com/myleott/mnist_png/raw/master/mnist_png.tar.gz")
            dest(dataFile)
        }
        copy {
            from(tarTree(file(dataFile)))
            into("$buildDir/dataset")
        }
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}