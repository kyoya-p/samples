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
val pauLicense = "LGPL-3.0-only"

tasks.cyclonedxBom {
    includeConfigs.set(listOf("jvmRuntimeClasspath"))
    skipConfigs.set(listOf("commonMainImplementation", "commonTestImplementation", "metadataCompileClasspath"))
    projectType.set("application")
    schemaVersion.set("1.6")
}

tasks.register("generateSbomMd") {
    dependsOn("cyclonedxBom")
    
    val bomFile = layout.buildDirectory.file("reports/bom.json")
    val outputFile = layout.projectDirectory.file("SBOM_FULL.md")
    
    // Extract properties to avoid capturing 'project' object in doLast
    val pName = rootProject.name
    val pVersion = version.toString()
    val pGroup = group.toString()
    val pLicense = pauLicense

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
        val slurper = groovy.json.JsonSlurper()
        val json = slurper.parseText(jsonText) as Map<*, *>
        
        val metadata = json["metadata"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val mainComp = metadata["component"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val components = json["components"] as? List<Map<*, *>> ?: emptyList()

        val projectName = (mainComp["name"] ?: pName).toString()
        val projectVersion = (mainComp["version"] ?: pVersion).toString()
        val projectGroup = (mainComp["group"] ?: pGroup).toString()
        
        // Try to get license from metadata, fallback to project definition
        val metadataLicenses = metadata["licenses"] as? List<Map<*, *>>
        val foundLicense = metadataLicenses?.mapNotNull {
            val lic = it["license"] as? Map<*, *>
            lic?.get("id")?.toString() ?: lic?.get("name")?.toString()
        }?.joinToString(", ")
        
        val projectLicense = if (foundLicense.isNullOrEmpty()) pLicense else foundLicense

        val sb = StringBuilder()
        sb.append("# SBOM Full Report - $projectName\n\n")
        sb.append("- **Version**: $projectVersion\n")
        sb.append("- **Group**: $projectGroup\n")
        sb.append("- **License**: $projectLicense\n")
        sb.append("- **Generated at**: ${metadata["timestamp"] ?: ""}\n\n")
        
        sb.append("## Component List (${components.size} items)\n\n")
        sb.append("| Group | Name | Version | License |\n")
        sb.append("| :--- | :--- | :--- | :--- |\n")

        val sortedComponents = components.sortedWith(compareBy({ it["group"]?.toString() ?: "" }, { it["name"]?.toString() ?: "" }))
        for (comp in sortedComponents) {
            val g = comp["group"] ?: "-"
            val n = comp["name"] ?: "-"
            val v = comp["version"] ?: "-"
            
            val isMain = g == projectGroup || g == "pau" || n == projectName
            
            val licenses = comp["licenses"] as? List<Map<*, *>>
            val licNames = licenses?.mapNotNull {
                val lic = it["license"] as? Map<*, *>
                lic?.get("id")?.toString() ?: lic?.get("name")?.toString()
            } ?: emptyList()
            
            val licStr = if (licNames.isNotEmpty()) {
                licNames.joinToString(", ")
            } else if (isMain) {
                projectLicense
            } else {
                "-"
            }
            
            sb.append("| $g | $n | $v | $licStr |\n")
        }

        outputFileObj.writeText(sb.toString())
        println("Successfully generated SBOM_FULL.md")
    }
}
