package kotlinx.rpc.sample

import io.ktor.util.*
import jp.wjg.shokkaa.container.*
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
    override suspend fun status() = flow {
        repeat(10) {
            val cli = listOf("wsl", "--user", "root", "ctr", "i", "ls", "-q")
            val imgs = ProcessBuilder(cli).start().inputStream.reader().readLines()
            println("[[$imgs]]")
            emit(
                CtStatus(
                    images = imgs.map { Image(id = it) },
                    containers = listOf()
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

    override suspend fun process(args: List<String>) = suspendCoroutine { c ->
        val p = ProcessBuilder(args).start()!!
        c.resume(p.inputStream.reader().readText())
    }

    fun ctr(args: List<String>) = ProcessBuilder(args).start().inputStream.reader().readLines()
    fun getImages() = ctr(listOf("wsl", "--user", "root", "ctr", "i", "ls", "-q")).map { Image(it.trim()) }
    fun getContainers() = ctr(listOf("wsl", "--user", "root", "ctr", "c", "ls")).map {
//        val c = it.drop(1).trim().split(" ")
//        println("[[$c]]")
        Container(id = it, imageId = it)
    }

    fun getStatus() = CtStatus(images = getImages(), containers = getContainers())
}
