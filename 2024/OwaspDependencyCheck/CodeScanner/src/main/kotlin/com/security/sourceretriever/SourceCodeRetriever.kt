package com.security.sourceretriever

import com.security.model.Dependency
import org.jsoup.Jsoup
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

class SourceCodeRetriever(private val outputDir: String) {
    
    init {
        Files.createDirectories(Paths.get(outputDir))
    }
    
    fun retrieveSource(dependency: Dependency) {
        if (dependency.groupId != null && dependency.artifactId != null) {
            retrieveMavenSource(dependency)
        } else {
            retrieveGithubSource(dependency)
        }
    }
    
    private fun retrieveMavenSource(dependency: Dependency) {
        val mavenRepoUrl = "https://repo1.maven.org/maven2"
        val groupPath = dependency.groupId!!.replace('.', '/')
        val artifactPath = dependency.artifactId!!
        val version = dependency.version
        
        val sourceJarUrl = "$mavenRepoUrl/$groupPath/$artifactPath/$version/$artifactPath-$version-sources.jar"
        
        try {
            downloadFile(
                sourceJarUrl,
                "$outputDir/${dependency.name}-sources.jar"
            )
        } catch (e: Exception) {
            println("Failed to download source for ${dependency.name}: ${e.message}")
        }
    }
    
    private fun retrieveGithubSource(dependency: Dependency) {
        // Search GitHub for the repository
        val searchUrl = "https://api.github.com/search/repositories?q=${dependency.name}"
        
        try {
            val response = Jsoup.connect(searchUrl)
                .ignoreContentType(true)
                .execute()
            
            val json = org.json.JSONObject(response.body())
            val items = json.getJSONArray("items")
            
            if (items.length() > 0) {
                val repo = items.getJSONObject(0)
                val cloneUrl = repo.getString("clone_url")
                
                println("Found GitHub repository: $cloneUrl")
                // Note: Actual cloning would require Git implementation
                // For now, we'll just save the repository URL
                File("$outputDir/${dependency.name}-github-url.txt")
                    .writeText(cloneUrl)
            }
        } catch (e: Exception) {
            println("Failed to find GitHub repository for ${dependency.name}: ${e.message}")
        }
    }
    
    private fun downloadFile(url: String, outputPath: String) {
        URL(url).openStream().use { input ->
            File(outputPath).outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}