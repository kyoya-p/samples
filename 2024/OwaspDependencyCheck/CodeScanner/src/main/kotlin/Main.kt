import com.security.dependencycheck.DependencyCheckParser
import com.security.sourceretriever.SourceCodeRetriever
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Please provide the path to the dependency-check-report.json file")
        return
    }

    val reportPath = args[0]
    val outputDir = args.getOrNull(1) ?: "downloaded-sources"

    try {
        val parser = DependencyCheckParser(reportPath)
        val dependencies = parser.parseDependencies()
        
        println("Found ${dependencies.size} dependencies with vulnerabilities")
        
        val retriever = SourceCodeRetriever(outputDir)
        dependencies.forEach { dependency ->
            println("Retrieving source for: ${dependency.name}")
            retriever.retrieveSource(dependency)
        }
        
        println("Source code retrieval completed. Check $outputDir directory")
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}