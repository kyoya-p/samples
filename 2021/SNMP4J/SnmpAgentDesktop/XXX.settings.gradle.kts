pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    
}
rootProject.name = "SnmpAgentDesktop"

include("Snmp4jUtils_KtJvm", "SnmpAgentDesktop")
