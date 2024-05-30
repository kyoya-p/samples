plugins {
    id("org.owasp.dependencycheck") version "9.2.0"  // https://plugins.gradle.org/plugin/org.owasp.dependencycheck
}

repositories {
    mavenCentral()
}

//dependencies {
//    // チェックサンプルモジュール
//    // ※脆弱性を含むので検証以外に使用不可
//    //implementation("org.apache.tomcat:tomcat-catalina:8.5.4") // https://mvnrepository.com/artifact/org.apache.tomcat/tomcat-catalina/8.5.4
//    //implementation("org.apache.logging.log4j:log4j-core:2.14.0")  // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core/2.14.0
//    //implementation("org.apache.activemq:activemq-all:5.15.10") // https://mvnrepository.com/artifact/org.apache.activemq/activemq-all
//}

dependencyCheck {
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL
//    scanSet = listOf(File("\\\\wsl\$\\Ubuntu-24.04\\home\\sharp\\works\\x2"))
    scanSet = listOf(File("~/works/x2/16803.src_agent_linux/"))
//    scanSet = listOf(File("./samples"))
}
