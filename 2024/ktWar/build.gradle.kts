plugins {
    kotlin("jvm") version "1.9.21"
    application
//    id("org.gretty") version "4.0.3"
    id("war")
}

group = "jp.wjg.shokkaa"
version = "1.0.0"
application {
//    mainClass.set("jp.wjg.shokkaa.ApplicationKt")
    mainClass = "ApplicationKt"
}

dependencies {
    implementation("io.ktor:ktor-server-servlet-jakarta:2.3.11")
}

//gretty {
//    httpPort = 8081
//    servletContainer = "tomcat10"
////    logbackConfigFile = "src/main/resources/logback.xml"
//}
