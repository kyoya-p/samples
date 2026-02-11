
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
        
        // Nativeターゲット共通のソースセット
        val nativeMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir("src@native")
        }

        val mingwX64Main by getting { dependsOn(nativeMain) }
        val linuxX64Main by getting { dependsOn(nativeMain) }
        val linuxArm64Main by getting { dependsOn(nativeMain) }
        val macosArm64Main by getting { dependsOn(nativeMain) }
        
        val jvmMain by getting {
            kotlin.srcDir("src@jvm")
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
