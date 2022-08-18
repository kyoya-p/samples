plugins {
    kotlin("jvm") version "1.7.10"
    id("org.owasp.dependencycheck") version "7.1.1"
}

group = "jp.wjg.sokkaa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


dependencies {
    // チェックサンプルモジュール
    // ※脆弱性を含むので検証以外に使用不可
    //implementation("org.apache.tomcat:tomcat-catalina:8.5.4") // https://mvnrepository.com/artifact/org.apache.tomcat/tomcat-catalina/8.5.4
    //implementation("org.apache.logging.log4j:log4j-core:2.14.0")  // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core/2.14.0
    //implementation("org.apache.activemq:activemq-all:5.15.10") // https://mvnrepository.com/artifact/org.apache.activemq/activemq-all
}

dependencyCheck {
//    scanSet = listOf(File("c:/srdm"))
}
