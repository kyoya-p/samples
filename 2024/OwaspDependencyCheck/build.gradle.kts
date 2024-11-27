plugins{
    id("java")
    id("org.owasp.dependencycheck") version "11.1.0"
    // https://plugins.gradle.org/plugin/org.owasp.dependencycheck
}

repositories {
    mavenCentral()
}

dependencies {
    // チェックサンプルモジュール
    // ※脆弱性を含むので検証以外に使用不可
//    implementation("org.apache.tomcat:tomcat-catalina:8.5.4") // https://mvnrepository.com/artifact/org.apache.tomcat/tomcat-catalina/8.5.4
//    implementation("org.apache.logging.log4j:log4j-core:2.14.0")  // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core/2.14.0
    implementation("log4j:log4j:1.2.17")
//    implementation("org.apache.activemq:activemq-all:5.15.10") // https://mvnrepository.com/artifact/org.apache.activemq/activemq-all
}


dependencyCheck {
//    format = "ALL"
//    failBuildOnCVSS = 8.5f
//    scanSet = listOf(projectDir.resolve("c:/"))
    scanSet = listOf(File("c:\\"))
    nvd { apiKey = System.getenv("NVD_API_KEY") }
}
