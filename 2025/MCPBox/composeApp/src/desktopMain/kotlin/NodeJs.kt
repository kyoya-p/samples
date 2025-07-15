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
import kotlinx.coroutines.withContext
import okio.*
import okio.Path.Companion.toPath
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration.Companion.seconds

val nodeVersion = "22.17.0"
val nodeJsDir = appDir.resolve("nodejs").resolve("node-v$nodeVersion-win-x64")

suspend fun setupNodejsEnvironment() {
    val nodejsZipUrl = "https://nodejs.org/dist/v$nodeVersion/node-v$nodeVersion-win-x64.zip"
    appDir.toFile().mkdirs()
    val zipFile = appDir.resolve("nodejs.zip")

    // Download Node.js (with Proxy)
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
    FileSystem.SYSTEM.unpackZip(zipFile, nodeJsDir.parent!!)
}

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
            sink(fileToWrite).buffer().use { sink ->
                sink.writeAll(source)
                println("Unzipped: $relativeFilePath to $fileToWrite")
            }
        }
    }
}

fun Path.createParentDirectories() {
    parent?.let { parent -> FileSystem.SYSTEM.createDirectories(parent) }
}

fun startNodeProcess(args: List<String>) = ProcessBuilder(args).apply {
    environment().run {
        put(
            "PATH",
            "${nodeJsDir.toFile().absolutePath};${
                nodeJsDir.toFile().resolve("node_modules\\npm\\bin").absolutePath
            };${environment()["PATH"]}"
        )
        put("NODE_DIR", "${nodeJsDir.toFile().absolutePath}")
    }
}.directory(nodeJsDir.toFile()).inheritIO().start()!!
    .also { process -> Runtime.getRuntime().addShutdownHook(Thread { process.destroy() }) }


suspend fun Process.await(
    onOutput: (String) -> Unit = { print(it) },
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
            onOutput("�v���Z�X���I�����܂����B�I���R�[�h: " + exitCode)
        }.onFailure { e ->
            onOutput("waitFor()�ŃG���[���������܂���: " + e.message)
        }
        println("Terminated destroy(): ${it.message}")
    }.getOrElse { -1 }
}

