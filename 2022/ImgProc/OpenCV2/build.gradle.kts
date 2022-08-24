import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.6.20"
    id("de.undercouch.download") version "5.0.5"  // https://github.com/michel-kraemer/gradle-download-task
}

group = "jp.wjg.shokkaa"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("OpenCVKt")
}

repositories {
    mavenCentral()
}


dependencies {
    //implementation("org.bytedeco:javacv:1.5.7") // https://mvnrepository.com/artifact/org.bytedeco/javacv
    //  implementation("org.bytedeco:opencv:4.5.5-1.5.7") // https://mvnrepository.com/artifact/org.bytedeco/opencv
    //implementation("org.bytedeco:javacv-platform:1.5.7")// https://mvnrepository.com/artifact/org.bytedeco/javacv-platform
    implementation("org.bytedeco:opencv-platform:4.5.5-1.5.7")// https://mvnrepository.com/artifact/org.bytedeco/opencv-platform
    implementation("com.quickbirdstudios:opencv-contrib:3.4.5") // https://mavenlibs.com/maven/dependency/com.quickbirdstudios/opencv-contrib
    //implementation(files("$buildDir/libs/OpenCV2-1.0-SNAPSHOT.jar"))
    implementation("org.jetbrains.kotlin:kotlin-script-util:1.6.21") // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-script-util
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


val downloadFiles by tasks.registering {
    doLast {
        download.run {
            src("https://sourceforge.net/projects/opencvlibrary/files/4.5.5/opencv-4.5.5-vc14_vc15.exe/download")
            dest("$buildDir/libs/opencv.exe")
        }
        exec {
            commandLine("$buildDir/libs/opencv.exe", "-o$buildDir", "-y")
        }
        copy {
            from(fileTree("$buildDir/opencv/build/java/x64/"))
            into("$projectDir")
        }
        copy {
            from(fileTree("$buildDir/opencv/build/x64/vc15/bin"))
            include { it.name.endsWith(".dll") }
            into("$projectDir")
        }
        download.run {
            src("https://pbs.twimg.com/media/FRaETtQVsAAoX4S?format=jpg&name=large")
            dest("$buildDir/samples/s1.jpg")
        }
    }
}

