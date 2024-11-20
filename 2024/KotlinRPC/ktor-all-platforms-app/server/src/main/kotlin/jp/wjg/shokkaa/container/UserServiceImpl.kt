package jp.wjg.shokkaa.container

import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
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
    override suspend fun getStatus() = CtStatus(images = getImages(), containers = getContainers(), tasks = getTasks())

    var lastStatus: CtStatus? = null
    override suspend fun updateStatus() = flow {
        while (isActive) {
            val s = getStatus()
            if (s != lastStatus) {
                lastStatus = s
                emit(s)
            }
            delay(10000)
        }
    }

    override suspend fun pullImage(id: String) =
        ctr(listOf("wsl", "--user", "root", "ctr", "i", "pull", id)).let { getStatus() }

    override suspend fun removeImage(id: String) =
        ctr(listOf("wsl", "--user", "root", "ctr", "i", "rm", id)).let { getStatus() }

    override suspend fun runContainer(imgId: String, ctrId: String, args: List<String>) =
        ctr(listOf("wsl", "--user", "root", "ctr", "run", imgId, ctrId) + args).let { getStatus() }

    override suspend fun removeContainer(ctrId: String) =
        ctr(listOf("wsl", "--user", "root", "ctr", "c", "rm", ctrId)).let { getStatus() }

    override suspend fun startTask(ctrId: String, args: List<String>) =
        ctr(listOf("wsl", "--user", "root", "ctr", "t", "start", ctrId) + args).let { getStatus() }

    override suspend fun execTask(ctrId: String, args: List<String>) =
        ctr(listOf("wsl", "--user", "root", "ctr", "t", "exec", ctrId) + args).let { getStatus() }

    override suspend fun killTask(ctrId: String,  signal: Int): CtStatus = ctr(
        listOf("wsl", "--user", "root", "ctr", "t", "kill", "-s", "$signal", ctrId)
    ).let { getStatus() }

    override suspend fun process(args: List<String>) = suspendCoroutine { c ->
        val p = ProcessBuilder(args).start()!!
        c.resume(p.inputStream.reader().readText())
    }

    data class ProcessResult(val exitCode: Int, val stdout: List<String>)

    suspend fun ctr(args: List<String>) = suspendCoroutine {
        val p = ProcessBuilder(args).start()
        val stdout = p.inputStream.reader().readLines()
        val rc = p.waitFor()
        it.resume(ProcessResult(rc, stdout))
    }

    suspend fun getImages() =
        ctr(listOf("wsl", "--user", "root", "ctr", "i", "ls", "-q")).stdout.map { Image(it.trim()) }

    suspend fun getContainers() = ctr(listOf("wsl", "--user", "root", "ctr", "c", "ls"))
        .stdout.drop(1).map { it.split(Regex("\\s+")) }.map { Container(it[0], it[1]) }

    suspend fun getTasks() =
        ctr(listOf("wsl", "--user", "root", "ctr", "t", "ls")).stdout.drop(1).map { it.split(Regex("\\s+")) }
            .map { Task(it[0], it[1], it[2]) }
}
