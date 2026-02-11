plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    mingwX64()
    linuxX64()
    linuxArm64()
    macosArm64()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("src")
        }
        val jvmMain by getting {
            kotlin.srcDir("src@jvm")
        }
        val mingwX64Main by getting {
            kotlin.srcDir("src@mingw")
        }
        val linuxX64Main by getting {
            kotlin.srcDir("src@linux")
        }
        val linuxArm64Main by getting {
            kotlin.srcDir("src@linux")
        }
        val macosArm64Main by getting {
            kotlin.srcDir("src@macos")
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations.getByName("main") {
            cinterops {
                create("ftxui") {
                    defFile(project.file("nativeInterop/cinterop/ftxui.def"))
                    includeDirs(project.file("nativeInterop/include"))
                }
            }
        }
    }
}
