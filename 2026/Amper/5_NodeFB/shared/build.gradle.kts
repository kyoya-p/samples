plugins {
    kotlin("multiplatform")
}

kotlin {
    js(IR) {
        nodejs()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
