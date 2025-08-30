import com.charleskorn.kaml.Yaml
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

val yaml = Yaml.default

@Serializable
data class Card(
    val id: String,
    val name: String,
    val url: String,
    val text: String,
    val html: String,
)

suspend fun main() = with(SystemFileSystem) {
    if (!exists(Path("data.out"))) {
        sink(Path("data.out")).buffered().use { out ->
            listCards().collectIndexed { i, card ->
                val c = yaml.encodeToString(mapOf(card.id to card))
                out.writeString("$c\n")
                println("$i ${card.id} ${card.name}")
            }
        }
    }
}

fun String.parseCard() = Ksoup.parse(this).select("li span.cardno").mapNotNull { it.parent() }.asFlow()
fun Element.toCard(): Card = Card(
    id = selectFirst("span.cardno")?.text().toString(),
    name = selectFirst("a")?.attr("title").toString(),
    url = selectFirst("a")?.attr("href").toString(),
    text = text(),
    html = html(),
)

suspend fun listCards() = flow {
    val client = HttpClient(CIO)
    val usrlBase = "https://batspi.com"
    val initUrl = "$usrlBase/index.php?%E3%82%AB%E3%83%BC%E3%83%89%E6%83%85%E5%A0%B1%E7%B5%9E%E8%BE%BC%E3%81%BF"
    client.get(initUrl).bodyAsText().parseCard().collect { emit(it) }

    for (i in 1..399) {
        val url = "$usrlBase/index.php?cmd=listselect&sel=&rowid=30457&pcnt2=$i"
        println(url)
        val s = client.get(url).bodyAsText().parseCard().onEach { emit(it) }.count()
        if (s == 0) break
    }
}.mapNotNull { it.toCard() }
