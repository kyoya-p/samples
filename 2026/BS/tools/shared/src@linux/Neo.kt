import kotlinx.io.files.Path

actual fun runImport(url: String, login: String, files: List<Path>, cacheDir: Path) {
    println("Error: Direct Neo4j import is currently only supported on the JVM platform.")
}
