package jp.wjg.shokkaa.container

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext

class UserServiceImpl(override val coroutineContext: CoroutineContext) : UserService {
    override suspend fun ctr(vararg args: String) = coroutineScope {
        val osWin = System.getProperty("os.name").contains("Windows", ignoreCase = true)
        val commandPrefix = if (osWin) listOf("wsl", "--user", "root", "ctr") else listOf("ctr")
        println("ctr [${args.joinToString(",")}]")
        val process = ProcessBuilder(commandPrefix + args).start()
        val stdout = async { process.inputStream.bufferedReader().readLines() }
        val stderr = async { process.errorStream.bufferedReader().readLines() }
        val exitCode = process.waitFor()
        ProcessResult(exitCode, stdout.await(), stderr.await())
    }
}

