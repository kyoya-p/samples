//package demo1

import com.charleskorn.kaml.decodeFromStream
import com.charleskorn.kaml.encodeToStream
import io.ktor.utils.io.streams.*
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import java.time.chrono.JapaneseEra.values

@ExperimentalSerializationApi
fun main(): Unit = with(SystemFileSystem) {
    source(Path("build/data.out")).buffered().use { src ->
        yaml.decodeToSequenc<Map<String, Card>>(src.asInputStream())
        yaml.encodeToStream(m.values, sink(Path("build/data.array")).buffered().asOutputStream())
    }
}
