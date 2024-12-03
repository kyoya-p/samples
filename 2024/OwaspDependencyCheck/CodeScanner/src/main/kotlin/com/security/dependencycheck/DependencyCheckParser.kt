package com.security.dependencycheck

import com.security.model.Dependency
import com.security.model.Vulnerability
import org.json.JSONObject
import java.io.File

class DependencyCheckParser(private val reportPath: String) {
    
    fun parseDependencies(): List<Dependency> {
        val jsonContent = File(reportPath).readText()
        val jsonReport = JSONObject(jsonContent)
        val dependencies = mutableListOf<Dependency>()
        
        val dependenciesArray = jsonReport.getJSONArray("dependencies")
        
        for (i in 0 until dependenciesArray.length()) {
            val dep = dependenciesArray.getJSONObject(i)
            if (dep.has("vulnerabilities")) {
                dependencies.add(parseDependency(dep))
            }
        }
        
        return dependencies
    }
    
    private fun parseDependency(depJson: JSONObject): Dependency {
        val vulnerabilities = mutableListOf<Vulnerability>()
        
        if (depJson.has("vulnerabilities")) {
            val vulnArray = depJson.getJSONArray("vulnerabilities")
            for (i in 0 until vulnArray.length()) {
                val vuln = vulnArray.getJSONObject(i)
                vulnerabilities.add(
                    Vulnerability(
                        name = vuln.getString("name"),
                        severity = vuln.getString("severity"),
                        description = vuln.getString("description")
                    )
                )
            }
        }
        
        return Dependency(
            name = depJson.getString("fileName"),
            version = depJson.optString("version", "unknown"),
            groupId = depJson.optString("groupId", null),
            artifactId = depJson.optString("artifactId", null),
            vulnerabilities = vulnerabilities
        )
    }
}