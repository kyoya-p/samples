plugins {
    id("org.owasp.dependencycheck") version "11.1.0" // https://plugins.gradle.org/plugin/org.owasp.dependencycheck
}

dependencyCheck {
    format = "ALL"
//    failBuildOnCVSS = 8.5f
//    scanSet = listOf(projectDir.resolve("c:/"))
    scanSet = listOf(File("c:\\"))
//    nvd { apiKey = System.getenv("NVD_API_KEY") }
}
