import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) = runBlocking {
    // 引数のパース (デフォルト値: 10秒間隔, 1分実行, 100枚)
    val interval = args.getOrNull(0)?.toIntOrNull()?.seconds ?: 10.seconds
    val totalDuration = args.getOrNull(1)?.toIntOrNull()?.seconds ?: 60.seconds
    val maxImages = args.getOrNull(2)?.toIntOrNull() ?: 100
    
    val outputDir = Path(".screenshot")
    val ffmpegPath = Path("../ffmpeg/bin/ffmpeg.exe")

    val fs = FileSystem.SYSTEM
    val startTime = Clock.System.now()
    val endTime = startTime + totalDuration

    if (!fs.exists(outputDir)) {
        fs.createDirectories(outputDir)
    }

    if (!fs.exists(ffmpegPath)) {
        System.err.println("Error: ffmpeg not found at $ffmpegPath")
        return@runBlocking
    }

    println("--- ScreenCapt Logger ---")
    println("Interval: $interval, Total Duration: $totalDuration, Max Images: $maxImages")
    println("Start Time: ${startTime.toLocalDateTime(TimeZone.currentSystemDefault())}")
    println("End Time  : ${endTime.toLocalDateTime(TimeZone.currentSystemDefault())}")

    while (isActive && Clock.System.now() < endTime) {
        try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val timestamp = now.toString()
                .substringBefore(".")
                .replace("-", "")
                .replace(":", "")
                .replace("T", "-")
            
            val outputFile = Path(outputDir.toString(), "screenshot-$timestamp.jpg")
            
            captureWithFfmpeg(ffmpegPath, outputFile)
            cleanupOldImages(fs, outputDir, maxImages)
        } catch (e: Exception) {
            System.err.println("Error: ${e.message}")
        }
        
        val remaining = endTime - Clock.System.now()
        if (remaining > interval) {
            delay(interval)
        } else if (remaining > 0.seconds) {
            delay(remaining)
        }
    }
    
    println("ScreenCapt finished (Total duration $totalDuration elapsed).")
}

fun captureWithFfmpeg(ffmpeg: Path, output: Path) {
    val pb = ProcessBuilder(
        ffmpeg.toString(),
        "-y",
        "-f", "gdigrab",
        "-i", "desktop",
        "-frames:v", "1",
        output.toString()
    )
    pb.redirectError(ProcessBuilder.Redirect.DISCARD)
    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)
    
    val process = pb.start()
    val finished = process.waitFor(5, TimeUnit.SECONDS)
    
    if (finished && process.exitValue() == 0) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        println("[$now] Captured: $output")
    } else {
        process.destroyForcibly()
        System.err.println("FFmpeg capture failed")
    }
}

fun cleanupOldImages(fs: FileSystem, outputDir: Path, maxImages: Int) {
    val images = fs.list(outputDir)
        .filter { it.name.startsWith("screenshot-") && it.name.endsWith(".jpg") }
        .sortedByDescending { fs.metadataOrNull(it)?.lastModifiedAt ?: 0L }
        
    if (images.size > maxImages) {
        images.drop(maxImages).forEach { 
            fs.delete(it)
            println("Deleted old image: ${it.name}")
        }
    }
}
