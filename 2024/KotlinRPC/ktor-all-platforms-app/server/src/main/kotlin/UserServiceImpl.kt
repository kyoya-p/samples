package jp.wjg.shokkaa.container

import io.ktor.util.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.streamScoped
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserServiceImpl(override val coroutineContext: CoroutineContext) : UserService {
    override suspend fun hello(user: String, userData: UserData): String {
        return "Nice to meet you $user, how is it in ${userData.address}?"
    }

    override suspend fun subscribeToNews(): Flow<String> {
        return flow {
            repeat(10) {
                delay(300)
                emit("Article number $it")
            }
        }
    }

    override suspend fun serverType() = System.getProperty("os.name").toLowerCasePreservingASCIIRules()

//    override suspend fun getStatus() = CtStatus(images = getImages(), containers = getContainers(), tasks = getTasks())
//
//    var lastStatus: CtStatus? = null
//    override suspend fun updateStatus() = channelFlow {
//        while (true) {
//            println("<<<")
//            val s = getStatus()
////            if (s != lastStatus) {
////                lastStatus = s
//            trySend(s)
////            }
//            delay(5000)
//            println(">>>")
//        }
//    }
//
//    override suspend fun pullImage(id: String) =
//        ctr(listOf("i", "pull", id)).let { getStatus() }
//
//    override suspend fun removeImage(id: String) =
//        ctr(listOf("i", "rm", id)).let { getStatus() }
//
//    override suspend fun runContainer(imgId: String, ctrId: String, args: List<String>) =
//        ctr(listOf("run", imgId, ctrId) + args).let { getStatus() }
//
//    override suspend fun removeContainer(ctrId: String) =
//        ctr(listOf("c", "rm", ctrId)).let { getStatus() }
//
//    override suspend fun startTask(ctrId: String, args: List<String>) =
//        ctr(listOf("t", "start", ctrId) + args).let { getStatus() }
//
//    override suspend fun execTask(ctrId: String, args: List<String>) =
//        ctr(listOf("t", "exec", ctrId) + args).let { getStatus() }
//
//    override suspend fun killTask(ctrId: String, signal: Int): CtStatus = ctr(
//        listOf("t", "kill", "-s", "$signal", ctrId)
//    ).let { getStatus() }
//
//    override suspend fun process(args: List<String>) = suspendCoroutine { c ->
//        val p = ProcessBuilder(args).start()!!
//        c.resume(p.inputStream.reader().readText())
//    }
//
//    suspend fun getImages() =
//        ctr(listOf("i", "ls", "-q")).stdout.map { Image(it.trim()) }
//
//    suspend fun getContainers() = ctr(listOf("c", "ls"))
//        .stdout.drop(1).map { it.split(Regex("\\s+")) }.map { Container(it[0], it[1]) }
//
//    suspend fun getTasks() =
//        ctr(listOf("t", "ls")).stdout.drop(1).map { it.split(Regex("\\s+")) }
//            .map { Task(it[0], it[1], it[2]) }


    override suspend fun ctr(vararg args: String) = suspendCoroutine {
        println("[[${System.getProperty("os.name")}]]")
//        val n = System.getProperty("os.name")!!
//        val osType = when {
//            n.contains("windows", ignoreCase = true) -> "win"
//            else -> "linux"
//        }
        println("ctr [${args.joinToString(",")}]")
        val p = ProcessBuilder(listOf("wsl", "--user", "root", "ctr") + args).start()
        val rc = p.waitFor()
        val stdout = p.inputStream.reader().readLines()
        println(stdout.joinToString("\n"))
        it.resume(ProcessResult(rc, stdout))
    }
}

