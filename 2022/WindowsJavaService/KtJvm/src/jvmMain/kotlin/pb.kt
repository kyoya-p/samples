fun main(){
    val pb=ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", "mkdir", "c:\\temp\\aaa")
    println("start '${pb.command()}'.")
    pb.start()
}