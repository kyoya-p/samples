plugins {
    kotlin("jvm") version "1.6.0"
    `maven-publish`
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
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

    repositories {
        maven {
            url = uri("http://192.168.3.14:8081/repository/repos")
            isAllowInsecureProtocol = true // 非セキュアの場合
            credentials {
                username = "admin" // System.getenv("MAVEN_USER")
                password = "8e1c50c5-ef52-4942-8030-d65ac39e1dd4" // System.getenv("MAVEN_TOKEN")
            }
        }

        maven {
            name = "GitHub" // nameを明示した場合、publishMavenPublicationToGitHubRepository などが使える
            url = uri("https://kyoya-p.github.io/samples/myrepos")
            credentials {
                username = System.getenv("GITHUB_USER")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
