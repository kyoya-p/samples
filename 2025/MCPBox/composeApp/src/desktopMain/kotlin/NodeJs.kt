package jp.wjg.shokkaa.mcp


import io.ktor.client.*
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.http
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.*
import okio.Path.Companion.toPath
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration.Companion.seconds

val nodeVersion = "22.17.0"
val nodeJsDir = appDir.resolve("nodejs").resolve("node-v$nodeVersion-win-x64")

context(logger: Logger)
suspend fun setupNodejsEnvironment() {
    val nodejsZipUrl = "https://nodejs.org/dist/v$nodeVersion/node-v$nodeVersion-win-x64.zip"
    appDir.toFile().mkdirs()
    val zipFile = appDir.resolve("nodejs.zip")

    logger.log("setupNodejsEnvironment(): loading..", "Application")
    HttpClient(CIO) {
        install(HttpTimeout) { connectTimeoutMillis = 30.seconds.inWholeMilliseconds }
        engine {
            appSettings.httpProxy?.let { if (it.isNotBlank()) proxy = ProxyBuilder.http(it) }
            https {
                trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                }
            }
        }
    }.get(nodejsZipUrl).bodyAsChannel().copyAndClose(zipFile.toFile().writeChannel())
    logger.log("setupNodejsEnvironment(): Unzip start", "Application")
    FileSystem.SYSTEM.unpackZip(zipFile, nodeJsDir.parent!!)
    logger.log("setupNodejsEnvironment(): Unzip completed", "Application")
}

context(logger: Logger)
fun FileSystem.unpackZip(zipFile: Path, dstDir: Path) {
    val zipFileSystem = openZip(zipFile)
    val paths = zipFileSystem.listRecursively("/".toPath())
        .filter { zipFileSystem.metadata(it).isRegularFile }
        .toList()
    paths.forEach { zipFilePath ->
        zipFileSystem.source(zipFilePath).buffer().use { source ->
            val relativeFilePath = zipFilePath.toString().trimStart('/')
            val fileToWrite = dstDir.resolve(relativeFilePath)
            fileToWrite.createParentDirectories()
            sink(fileToWrite).buffer().use { sink -> sink.writeAll(source) }
        }
    }
}

fun Path.createParentDirectories() {
    parent?.let { parent -> FileSystem.SYSTEM.createDirectories(parent) }
}

context(logger: Logger)
fun startNodeProcess(args: List<String>): Process = guard("startNodeProcess()") {
    val command = nodeJsDir.resolve(args[0]).also { println(it) }.toString()
    val args2 = listOf(command) + args.drop(1)
    println(args2)
    ProcessBuilder(args2).apply {
        environment().run {
            val path1 = nodeJsDir
            val path2 = path1.resolve("node_modules\\npm\\bin")
            put("PATH", "$path1;$path2;${environment()["PATH"]}")
            put("NODE_DIR", "$nodeJsDir")
        }
    }.directory(nodeJsDir.toFile()).start()!!
//        .also { process -> Runtime.getRuntime().addShutdownHook(Thread { process.destroy() }) }
}

suspend fun Process.await(
    onOutput: (String) -> Unit = { print("IO: $it") },
): Int = withContext(Dispatchers.Default) {
    runCatching {
        println("PID=${pid()}")
        val stdoutJob = launch { inputStream.bufferedReader().forEachLine { onOutput(it) } }
        val stderrJob = launch { errorStream.bufferedReader().forEachLine { onOutput(it) } }
        val exitCode = this@await.waitFor()
        stdoutJob.join()
        stderrJob.join()
        exitCode
    }.onFailure {
        onOutput("Exception: ${it.message}")
        println("isAlive=$isAlive")
        destroyForcibly()
        runCatching {
            val exitCode = waitFor()
            onOutput("wait():" + exitCode)
        }.onFailure { e ->
            onOutput("wait():" + e.message)
        }
        println("Terminated destroy(): ${it.message}")
    }.getOrElse { -1 }
}

fun main() = runBlocking {
    with(Logger("main") { m, f -> println("$f: $m") }) {
        startNodeProcess(
            listOf(
                "node.exe",
                "node_modules\\npm\\bin\\npx-cli.js",
                "-y",
                "@playwright/mcp@latest"
            )
        ).await()
        Unit
    }
}

suspend fun McpService.runNode() {
    with(Logger("main") { m, f -> println("$f: $m") }) {
        val process = ProcessBuilder(
            "C:\\Users\\kyoya\\.mcpbox\\nodejs\\node-v22.17.0-win-x64\\node.exe",
            "node_modules\\npm\\bin\\npx-cli.js",
            "-y",
            "@playwright/mcp@latest"
        ).directory("C:\\Users\\kyoya\\.mcpbox\\nodejs\\node-v22.17.0-win-x64".toPath().toFile())
            .apply {
                environment().run {

                    val path = nodeJsDir.resolve("node_modules\\npm\\bin").also { println(it) }
                    put(
                        "PATH",
                        "${nodeJsDir.toFile().absolutePath};${
                            nodeJsDir.toFile().resolve("node_modules\\npm\\bin").absolutePath
                        };${environment()["PATH"]}"
                    )
                    put("NODE_DIR", "${nodeJsDir.toFile().absolutePath}")
                }
            }
            .inheritIO()
            .start()!!
        process.await {
            println("OUT: it")
        }
    }
}