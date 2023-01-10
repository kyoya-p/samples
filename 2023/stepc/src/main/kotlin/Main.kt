import kotlinx.coroutines.runBlocking
import java.io.File

fun main(args: Array<String>): Unit = runBlocking {
    val tgPath = File(args.getOrNull(0) ?: ".")
    val grFiles = args.drop(1).flatMap { File(it).readLines() }
    val lines = tgPath.walk().filter { it.isFile /* && it.extension.lowercase() == "java"*/ }.sumOf { f ->
        f.readLines().size
    }
    println(lines)
}