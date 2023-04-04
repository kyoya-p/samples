import java.io.File

fun main(args: Array<String>) {
    File("samples").walk().filter { it.isFile }.forEach { f ->
        println(f.path)
        sequence { f.useLines {it.forEach { yield(it) } } }.filter { it.contains("ARMM") }.forEach {
            println(it)
        }
    }
}