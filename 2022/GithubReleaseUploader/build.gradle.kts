plugins {
    //java
    `maven-publish`
}

version = "1.1.15856.20220524-test5"

repositories {
    mavenCentral()
}

group = "jp.wjp.co.sharp.dvm" // Test用
//group = "jp.co.sharp.dvm" // 本番用

publishing {
    publications {
        create<MavenPublication>("maven") {
            //from(components["java"]) // Javaの成果物をアップロードする場合
            artifact("publication/agent.zip") { // 特定のファイルをアップロードする場合
                //classifier = "uniagent" // リポジトリ上のファイル名に追加される
                extension = "zip"  // リポジトリ上のファイルの拡張子
                // リポジトリにアップロードされるファイル名: "$group/${rootProject.name}-$version-$classifire.$extention"
            }
        }
        repositories {
            maven {
                // usage:
                // set MAVEN_USER=...
                // set MAVEN_PASSWORD=...
                // gradlew publishMavenPublicationToScmavenRepository
                name = "scmaven"
                url = uri("https://nexus3.sharpb2bcloud.com/repository/maven-releases")
                isAllowInsecureProtocol = true // 非セキュア(httpsでない)の場合

                credentials {
                    username = System.getenv("MAVEN_USER")  // example: MAVEN_USER=admin
                    password = System.getenv("MAVEN_PASSWORD") // example: MAVEN_PASSWORD=secret-word
                    println("User: $username / Password: $password")
                    println("Version: $version")
                }
            }
        }
    }
}
