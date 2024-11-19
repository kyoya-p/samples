package jp.wjg.shokkaa.container

import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    override suspend fun updateStatus() = flow {
        repeat(10) {
            val cli = listOf("wsl", "--user", "root", "ctr", "i", "ls", "-q")
            val imgs = ProcessBuilder(cli).start().inputStream.reader().readLines()
            emit(
                CtStatus(
                    images = imgs.map { Image(id = it) },
                    containers = getContainers(),
                    tasks = getTasks(),
                )
            )
            delay(10000)
        }
    }

    override suspend fun pullImage(id: String) = suspendCoroutine {
        val cli = listOf("wsl", "--user", "root", "ctr", "i", "pull", id)
        ProcessBuilder(cli).start().inputStream.reader().readLines()
        it.resume(getStatus())
    }

    override suspend fun removeImage(id: String) = suspendCoroutine { c ->
        val cli = listOf("wsl", "--user", "root", "ctr", "i", "rm", id)
        ProcessBuilder(cli).start().inputStream.reader().readLines()
        c.resume(getStatus())
    }

    override suspend fun runContainer(imgId: String, cntnrId: String, args: List<String>) = suspendCoroutine { c ->
        val cli = listOf("wsl", "--user", "root", "ctr", "run", imgId, cntnrId) + args
        ProcessBuilder(cli).start().inputStream.reader().readLines()
        c.resume(getStatus())
    }

    override suspend fun removeContainer(ctrId: String) = suspendCoroutine { c ->
        val cli = listOf("wsl", "--user", "root", "ctr", "c", "rm", ctrId)
        ProcessBuilder(cli).start().inputStream.reader().readLines()
        c.resume(getStatus())
    }

    override suspend fun execTask(ctrId: String, args: List<String>) = suspendCoroutine { c ->
        val cli = listOf("wsl", "--user", "root", "ctr", "t", "exec", ctrId) + args
        ProcessBuilder(cli).start().inputStream.reader().readLines()
        c.resume(getStatus())
    }

    override suspend fun killTask(id: String, signal: Int) = suspendCoroutine { c ->
        val cli = listOf("wsl", "--user", "root", "ctr", "t", "kill", "-s", "$signal")
        ProcessBuilder(cli).start().inputStream.reader().readLines()
        c.resume(getStatus())
    }

    override suspend fun process(args: List<String>) = suspendCoroutine { c ->
        val p = ProcessBuilder(args).start()!!
        c.resume(p.inputStream.reader().readText())
    }

    fun ctr(args: List<String>) = ProcessBuilder(args).start().inputStream.reader().readLines()
    fun getImages() = ctr(listOf("wsl", "--user", "root", "ctr", "i", "ls", "-q")).map { Image(it.trim()) }
    fun getContainers() = ctr(listOf("wsl", "--user", "root", "ctr", "c", "ls"))
        .drop(1).map { it.split(Regex("\\s+")) }.map { Container(it[0], it[1]) }

    fun getTasks() = ctr(listOf("wsl", "--user", "root", "ctr", "t", "ls")).drop(1).map { it.split(Regex("\\s+")) }
        .map { Task(it[0], it[1], it[2]) }

    fun getStatus() = CtStatus(images = getImages(), containers = getContainers(), tasks = getTasks())
}
