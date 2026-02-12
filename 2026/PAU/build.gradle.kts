plugins {
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false

    id("org.owasp.dependencycheck") version "12.1.8" // https://plugins.gradle.org/plugin/org.owasp.dependencycheck
    id("org.cyclonedx.bom") version "2.2.0"
}

group = "jp.wjg.shokkaa"
version = "1.0.9"

tasks.withType<org.cyclonedx.gradle.CycloneDxTask> {
    includeConfigs.set(listOf("jvmRuntimeClasspath"))
    skipConfigs.set(listOf("commonMainImplementation", "commonTestImplementation", "metadataCompileClasspath"))
}

tasks.register("generateSbomMd") {
    dependsOn("cyclonedxBom")
    
    val bomFile = layout.buildDirectory.file("reports/bom.json")
    val outputFile = layout.projectDirectory.file("SBOM_FULL.md")
    
    inputs.file(bomFile)
    outputs.file(outputFile)

    doLast {
        val bomFileObj = bomFile.get().asFile
        val outputFileObj = outputFile.asFile
        
        if (!bomFileObj.exists()) {
            println("BOM file missing: ${bomFileObj.absolutePath}")
            return@doLast
        }

        val jsonText = bomFileObj.readText()
        
        fun findValue(key: String, text: String): String? {
            val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            return pattern.find(text)?.groupValues?.get(1)
        }

        val name = findValue("name", jsonText) ?: "pau"
        val version = findValue("version", jsonText) ?: "1.0.9"
        val group = findValue("group", jsonText) ?: "jp.wjg.shokkaa"
        val timestamp = findValue("timestamp", jsonText) ?: ""

        val sb = StringBuilder()
        sb.append("# SBOM Full Report - $name\n\n")
        sb.append("- **Version**: $version\n")
        sb.append("- **Group**: $group\n")
        sb.append("- **Generated at**: $timestamp\n\n")
        
        try {
            val slurper = groovy.json.JsonSlurper()
            val json = slurper.parseText(jsonText) as Map<*, *>
            val components = json["components"] as? List<Map<*, *>> ?: emptyList()
            
            sb.append("## Component List (${components.size} items)\n\n")
            sb.append("| Group | Name | Version | License |\n")
            sb.append("| :--- | :--- | :--- | :--- |\n")

            val sortedComponents = components.sortedWith(compareBy({ it["group"]?.toString() ?: "" }, { it["name"]?.toString() ?: "" }))
            for (comp in sortedComponents) {
                val g = comp["group"] ?: "-"
                val n = comp["name"] ?: "-"
                val v = comp["version"] ?: "-"
                val licenses = comp["licenses"] as? List<Map<*, *>>
                val licNames = licenses?.mapNotNull {
                    val lic = it["license"] as? Map<*, *>
                    lic?.get("id")?.toString() ?: lic?.get("name")?.toString()
                } ?: emptyList()
                val licStr = if (licNames.isEmpty()) "-" else licNames.joinToString(", ")
                sb.append("| $g | $n | $v | $licStr |\n")
            }
        } catch (e: Exception) {
            sb.append("\n> Warning: Detailed component list could not be generated due to an internal error.\n")
        }

        outputFileObj.writeText(sb.toString())
        println("Successfully generated SBOM_FULL.md")
    }
}

