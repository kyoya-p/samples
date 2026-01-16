kotlin {
    sourceSets {
        findByName("jsMain")?.dependencies {
            implementation(npm("firebase", "10.7.1"))
        }
    }
}
