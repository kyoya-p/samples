import java.io.File
import javax.script.ScriptEngineManager

fun main(args: Array<String>) {
    val engine = ScriptEngineManager().getEngineByExtension("kts")
    engine.eval("""println("Hello Kotlin")""")
}
