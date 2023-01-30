import java.io.File

fun main() {
    val pb = ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", "dir", )
    pb.redirectOutput(File("process.txt"))
    pb.redirectErrorStream(true)
    println("start '${pb.command()}'.")
    pb.start()
}