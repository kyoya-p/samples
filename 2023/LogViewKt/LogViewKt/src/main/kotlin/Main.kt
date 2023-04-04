import java.io.File

fun main() = File("samples").walk().filter { it.isFile }.forEach { f ->
     sequence { f.useLines { it.forEach { yield(it) } } }.filter { it.contains("ARMM") }.forEach {
        println("${f.name} $it")
    }
}