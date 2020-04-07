import org.kohsuke.args4j.*
import java.util.*


// 引数の解析
// http://extra-vision.blogspot.com/2016/11/kotlin.html

class Args {
    @Option(name = "-a", metaVar = "STRING", usage = "address")
    var addr = "127.0.0.1"
    @Option(name = "-p", metaVar = "NUM", usage = "port")
    var port = 161
    @Option(name = "-o", usage = "objectId", metaVar = "STRING")
    var oid: String = ".1"
    @Argument
    var arguments: MutableList<String> = ArrayList()
}

fun main(args: Array<String>) {
    val app = Args()
    val parser = CmdLineParser(app)
    try {
        parser.parseArgument(*args)
    } catch (e: CmdLineException) {
        System.err.println(e)
        System.err.printf("usage: %n\tjava Args4jSample %s%n",
                parser.printExample(OptionHandlerFilter.ALL)
        )
        parser.printUsage(System.err)
    }
    println(app)
}
