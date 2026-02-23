import kotlinx.cinterop.*
import platform.windows.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path as IoPath

fun main(args: Array<String>) = runBlocking {
    val interval = args.getOrNull(0)?.toLongOrNull()?.seconds ?: 10.seconds
    val totalDuration = args.getOrNull(1)?.toLongOrNull()?.seconds ?: 60.seconds
    val maxImages = args.getOrNull(2)?.toIntOrNull() ?: 100

    val outputDir = ".screenshot"
    if (!FileSystem.SYSTEM.exists(IoPath(outputDir))) {
        FileSystem.SYSTEM.createDirectories(IoPath(outputDir))
    }

    val startTime = Clock.System.now()
    val endTime = startTime + totalDuration

    println("Starting Native ScreenCapt (Win32 API)...")
    
    while (isActive && Clock.System.now() < endTime) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val timestamp = now.toString().split(".")[0].replace("-", "").replace(":", "").replace("T", "-")
        val fileName = "$outputDir/screenshot-$timestamp.bmp"

        if (captureScreen(fileName)) {
            println("[$now] Captured: $fileName")
        } else {
            System.err.println("Capture failed")
        }

        delay(interval)
    }
}

fun captureScreen(fileName: String): Boolean = memScoped {
    val hScreen = GetDC(null) ?: return false
    val hDC = CreateCompatibleDC(hScreen) ?: return false
    
    val width = GetSystemMetrics(SM_CXSCREEN)
    val height = GetSystemMetrics(SM_CYSCREEN)
    
    val hBitmap = CreateCompatibleBitmap(hScreen, width, height) ?: return false
    val hOldObj = SelectObject(hDC, hBitmap)
    
    BitBlt(hDC, 0, 0, width, height, hScreen, 0, 0, SRCCOPY)
    
    val result = saveBitmap(hBitmap, hDC, width, height, fileName)
    
    SelectObject(hDC, hOldObj)
    DeleteObject(hBitmap)
    DeleteDC(hDC)
    ReleaseDC(null, hScreen)
    
    return result
}

fun saveBitmap(hBitmap: HBITMAP?, hDC: HDC?, width: Int, height: Int, fileName: String): Boolean = memScoped {
    val bitCount: Short = 24
    val widthBytes = ((width * bitCount + 31) / 32) * 4
    val imageSize = widthBytes * height
    
    val bmi = alloc<BITMAPINFO>()
    bmi.bmiHeader.biSize = sizeOf<BITMAPINFOHEADER>().toUInt()
    bmi.bmiHeader.biWidth = width
    bmi.bmiHeader.biHeight = height
    bmi.bmiHeader.biPlanes = 1
    bmi.bmiHeader.biBitCount = bitCount
    bmi.bmiHeader.biCompression = BI_RGB.toUInt()
    bmi.bmiHeader.biSizeImage = imageSize.toUInt()

    val pPixels = nativeHeap.allocArray<ByteVar>(imageSize)
    
    if (GetDIBits(hDC, hBitmap, 0, height.toUInt(), pPixels, bmi.ptr, DIB_RGB_COLORS) == 0) {
        nativeHeap.free(pPixels)
        return false
    }

    val bfh = alloc<BITMAPFILEHEADER>()
    bfh.bfType = 0x4D42.toUShort() // 'BM'
    bfh.bfSize = (sizeOf<BITMAPFILEHEADER>() + sizeOf<BITMAPINFOHEADER>() + imageSize).toUInt()
    bfh.bfOffBits = (sizeOf<BITMAPFILEHEADER>() + sizeOf<BITMAPINFOHEADER>()).toUInt()

    val file = fopen(fileName, "wb") ?: return false
    try {
        fwrite(bfh.ptr, sizeOf<BITMAPFILEHEADER>().toULong(), 1, file)
        fwrite(bmi.ptr, sizeOf<BITMAPINFOHEADER>().toULong(), 1, file)
        fwrite(pPixels, imageSize.toULong(), 1, file)
    } finally {
        fclose(file)
        nativeHeap.free(pPixels)
    }
    
    return true
}
