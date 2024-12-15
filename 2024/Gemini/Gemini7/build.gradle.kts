plugins{
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://company/com/maven2")
    }
    mavenLocal()
    flatDir {
        dirs("libs")
    }
}


dependencies {
    implementation("com.google.cloud:google-cloud-aiplatform:3.5.0")
    implementation("com.google.protobuf:protobuf-java-util:3.25.1")
    implementation("io.grpc:grpc-protobuf:1.59.0")
}

