plugins {
    application
    java
    //kotlin("jvm") version "1.6.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"
application.mainClass.set("org.apache.xalan.xslt.Process")

repositories {
    mavenCentral()
}

dependencies {
    //implementation(kotlin("stdlib"))
    implementation("org.apache.xmlgraphics:fop:2.6") // https://mvnrepository.com/artifact/org.apache.xmlgraphics/fop
    //implementation("xalan:xalan:2.7.2") // https://mvnrepository.com/artifact/xalan/xalan
}

tasks.register("unzipAddrBook") {
    doFirst {
        copy {
            from(zipTree("address.ods"))
            into("build/unzip")
        }
    }
}
