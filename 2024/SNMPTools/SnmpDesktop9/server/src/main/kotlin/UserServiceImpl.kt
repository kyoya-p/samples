package jp.wjg.shokkaa.container

import io.ktor.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import org.snmp4j.fluent.SnmpBuilder
import kotlin.coroutines.CoroutineContext

val snmp = SnmpBuilder().udp().v1().v3().build()!!

class UserServiceImpl(override val coroutineContext: CoroutineContext) : UserService {
    override suspend fun serverType() = System.getProperty("os.name").toLowerCasePreservingASCIIRules()
    override suspend fun ctr(vararg args: String) = coroutineScope {
        val command = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            listOf("wsl", "--user", "root", "ctr") + args
        } else {
            listOf("ctr") + args
        }

        println("ctr [${args.joinToString(",")}]")
        val process = ProcessBuilder(command).start()
        val stdout = async { process.inputStream.bufferedReader().readLines() }
        val stderr = async { process.errorStream.bufferedReader().readLines() }
        val exitCode = process.waitFor()
//        val rc = p.waitFor()
//        val stdout = p.inputStream.reader().readLines()
//        println(stdout.joinToString("\n"))
        ProcessResult(exitCode, stdout.await())
    }

    override suspend fun unicast( request: SnmpRequest) =
        snmpUnicast(req = request, snmp = snmp)


    override suspend fun scan(
        startIp: String,
        endIp: String,
        request: SnmpRequest,
    ): Flow<SnmpResult> {
        TODO("Not yet implemented")
    }
}

