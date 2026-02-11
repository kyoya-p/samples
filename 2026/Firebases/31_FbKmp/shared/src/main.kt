
fun main() {
    var count = 0
    
    runApp {
        count++
        "FTXUI Multiplatform App [Count: $count] - ${getWorld()}"
    }
}
