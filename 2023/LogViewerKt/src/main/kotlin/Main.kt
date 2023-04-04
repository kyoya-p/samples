import java.io.File

fun main(args: Array<String>) {
    File("samples").walk().map{f->
        println(f.path)
        f.read()
    }
}