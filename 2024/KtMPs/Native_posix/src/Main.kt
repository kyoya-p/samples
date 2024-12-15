import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv
import platform.posix.setenv
import platform.posix.system

@OptIn(ExperimentalForeignApi::class)
fun main() {
    setenv("ENV1", "VAL1", 1/*Overwrite*/)
    val pwd = getenv("ENV1")?.toKString() ?: "UNK"
    println("ENV1:$pwd")
    system("echo ${'$'}ENV1")

    for (i in listOf(-1, 0, 1, 2, 255, 256)) {
        val rc = system("exit $i")
        println("exec:'exit $i', RC: $rc, exit:${rc / 256}")
    }
}

/*
 Refer:
 - https://ja.manpages.org/

 */
