import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import org.jline.terminal.Terminal
import org.jline.utils.NonBlockingReader

class JvmInputProvider : InputProvider {
    private var terminal: Terminal? = null
    private var fallbackMode = false

    init {
        try {
            // Force strict configuration
            terminal = TerminalBuilder.builder()
                .system(true)
                .dumb(false)
                .ffm(false)
                .jansi(true)
                .jna(false) // Try disabling JNA to rely on pure Jansi if extracting dll works
                .build()
            
            terminal?.puts(InfoCmp.Capability.key_mouse)
            // Enable SGR (1006) which is safer for modern terminals, and Basic (1000) as fallback
            print("\u001B[?1000h\u001B[?1006h")
            terminal?.enterRawMode()
        } catch (e: Exception) {
            println("Warning: Failed to initialize system terminal. (${e.message})")
            fallbackMode = true
        }
    }

    override suspend fun nextCommand(): String? {
        if (fallbackMode) {
             return withContext(Dispatchers.IO) { readlnOrNull() }
        }
        
        val t = terminal
        if (t == null) return null
        val r = t.reader()
        
        return withContext(Dispatchers.IO) {
            while (true) {
                val c = r.read()
            if (c == -1) return@withContext null 

            if (c == 27) { // ESC
                val next1 = r.read()
                if (next1 == '['.code) {
                    val next2 = r.read()
                    
                    // SGR Mode: ESC [ < b ; x ; y M (or m)
                    if (next2 == '<'.code) {
                        val buf = StringBuilder()
                        while(true) {
                            val ch = r.read()
                            if (ch == 'M'.code || ch == 'm'.code) {
                                buf.append(ch.toChar())
                                break
                            }
                            buf.append(ch.toChar())
                        }
                        val sgrParts = buf.toString().dropLast(1).split(";")
                        if (sgrParts.size >= 3) {
                            val b = sgrParts[0].toIntOrNull() ?: 0
                            val x = sgrParts[1].toIntOrNull() ?: 0
                            val y = sgrParts[2].toIntOrNull() ?: 0
                            val isRelease = buf.endsWith("m")
                            
                            // 0 = Left Click
                            if (b == 0 && !isRelease) {
                                processClick(x, y)?.let { return@withContext it }
                                return@withContext "DEBUG SGR Click: $x, $y"
                            }
                        }
                    }
                    // X10 Mode: ESC [ M b x y
                    else if (next2 == 'M'.code) {
                        val b = r.read() - 32
                        val x = r.read() - 32
                        val y = r.read() - 32
                        val button = b and 3
                        if (button == 0) {
                            processClick(x, y)?.let { return@withContext it }
                            return@withContext "DEBUG X10 Click: $x, $y"
                        }
                    } 
                }
            } else if (c == 'q'.code) {
                return@withContext "q"
            } else if (c == 'r'.code) {
                 return@withContext "r"
            }
        }
        null
    }
}

    private fun processClick(x: Int, y: Int): String? {
        // Layout:
        // 1: Title
        // 2: Sep
        // 3: Commands
        // 4: Status / Close
        // 5: Last Cmd
        // 6: Sep
        // 7: Header (Name...)
        // 8: Sep
        // 9: Data Start (Index 0)
        
        // Check Close Button (Line 4)
        if (y == 4 && x >= 65) return "q"

        val headerLines = 8
        val listIndex = y - 1 - headerLines
        
        // Check Remove Button (Col 80+)
        if (x >= 80 && listIndex >= 0) {
            return "CLICK_REMOVE $listIndex"
        }
        return null
    }
}

fun main() {
    mainApp(JvmInputProvider())
}
