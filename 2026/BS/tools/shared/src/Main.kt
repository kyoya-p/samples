import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.charleskorn.kaml.Yaml
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands

val defaultCachePath = Path(getEnv("USERPROFILE").ifEmpty { getEnv("HOME") }.ifEmpty { "." }, ".bscards")

class BsCli : CliktCommand(name = "bs-cli") {
    override fun run() = Unit
}

class FetchCards(private val argv: List<String>) : CliktCommand(name = "fetch") {
    override fun help(context: Context) = "バトルスピリッツ カードデータ取得・キャッシュ CLI"
    override val printHelpOnEmptyArgs = true

    init {
        context {
            helpOptionNames = setOf("-h", "--help")
            helpFormatter = { context -> MordantHelpFormatter(context, showDefaultValues = true) }
        }
    }

    val keywords by argument(help = "検索キーワード（フリーワード）").multiple(required = false)
    val force by option("-f", "--force", help = "キャッシュを無視しデータ取得").flag()
    val cacheDir by option("-d", "--cache-dir", help = "キャッシュ先ディレクトリ指定", envvar = "BSCARD_CACHE_DIR")
        .convert { Path(it) }.default(defaultCachePath)
    val cost by option("-c", "--cost", help = "コスト範囲（例: '3-5'、'7'）").default("0-30")
    val attributes by option("-a", "--color", "--attr", help = "属性/色 (例: -a 赤, -a P)。OR検索").multiple()
    val attributesAnd by option("-A", "--color-and", "--attr-and", help = "属性/色 (例: -A R, -A 白)。AND検索")
        .multiple()
    val categories by option("-t", "--type", "--category", help = "カテゴリ (例: -t スピリット,-t S)").multiple()
    val systems by option("-s", "--system", "--family", help = "系統 (例: -s 星竜 -s 勇傑)。OR検索").multiple()
    val systemsAnd by option("-S", "--system-and", "--family-and", help = "系統 (例: -S 星竜 -S 勇傑)。AND検索")
        .multiple()
    val blockIcons by option("-b", "--block", help = "ブロックアイコン (例: -b 7)").multiple()

    override fun run() {
        runBlocking {
            try {
                val (costMin, costMax) = cost.split("-").map { it.trim().toInt() }.let { if (it.size == 1) it + it else it }
                val htmlDir = Path(cacheDir, "html")
                val yamlDir = Path(cacheDir, "yaml")
                if (!SystemFileSystem.exists(htmlDir)) SystemFileSystem.createDirectories(htmlDir)
                if (!SystemFileSystem.exists(yamlDir)) SystemFileSystem.createDirectories(yamlDir)

                fun <T, E> Flow<T>.distinctBy(op: (T) -> E): Flow<T> = flow {
                    val seen = mutableSetOf<E>()
                    collect { value -> if (seen.add(op(value))) emit(value) }
                }
                println("Freewords: $keywords & Cost: $costMin-$costMax ")

                createClient().use { client ->
                    val lastAttrAndIdx = argv.indexOfLast { it == "-A" || it == "--color-and" || it == "--attr-and" }
                    val lastAttrOrIdx = argv.indexOfLast { it == "-a" || it == "--color" || it == "--attr" }
                    val attrMode = if (lastAttrAndIdx > lastAttrOrIdx) "AND" else "OR"

                    val lastSysAndIdx = argv.indexOfLast { it == "-S" || it == "--system-and" || it == "--family-and" }
                    val lastSysOrIdx = argv.indexOfLast { it == "-s" || it == "--system" || it == "--family" }
                    val sysMode = if (lastSysAndIdx > lastSysOrIdx) "AND" else "OR"

                    bsFetchMain(
                        client = client,
                        keywords = keywords.joinToString(" "),
                        costMin = costMin,
                        costMax = costMax,
                        attributes = parseAttributes(attributes + attributesAnd),
                        categories = parseCategories(categories),
                        systems = systems + systemsAnd,
                        blockIcons = blockIcons,
                        attributeSwitch = attrMode,
                        systemSwitch = sysMode,
                    ).distinctBy { it.cardNo }.collectIndexed { ix, searched ->
                        val htmlFn = Path(htmlDir, "${searched.cardNo}.html")
                        val yamlFn = Path(yamlDir, "${searched.cardNo}.yaml")
                        print("$ix: target: ${searched.cardNo} : ")
                        if (force || !SystemFileSystem.exists(yamlFn)) {
                            try {
                                val (card, html) = bsDetail(client, searched.cardNo)
                                SystemFileSystem.sink(htmlFn).buffered().use { it.write(html.encodeToByteArray()) }
                                SystemFileSystem.sink(yamlFn).buffered()
                                    .use { it.write(Yaml.default.encodeToString(Card.serializer(), card).encodeToByteArray()) }
                                println("collected $yamlFn .")
                            } catch (e: Exception) {
                                println("failed to collect detail for ${searched.cardNo}: ${e.message}")
                            }
                        } else println("already exists.")
                    }
                }
            } catch (e: Exception) {
                echo("Error occurred: ${e.message}", err = true)
                e.printStackTrace()
            }
        }
    }
}

fun parseAttributes(inputs: List<String>): List<String> {
    val colorMap = mapOf('R' to "赤", 'P' to "紫", 'G' to "緑", 'W' to "白", 'Y' to "黄", 'B' to "青")
    return inputs.flatMap { input ->
        if (input.all { it.uppercase().first() in colorMap.keys }) {
            input.map { colorMap[it.uppercase().first()]!! }
        } else listOf(input)
    }
}

fun parseCategories(inputs: List<String>): List<String> {
    val categoryMap = mapOf(
        'S' to "スピリット",
        'U' to "アルティメット",
        'B' to "ブレイヴ",
        'N' to "ネクサス",
        'M' to "マジック"
    )
    return inputs.flatMap { input ->
        if (input.length == 1 && input.uppercase().first() in categoryMap.keys) {
            listOf(categoryMap[input.uppercase().first()]!!)
        } else listOf(input)
    }
}

class Simulate : CliktCommand(name = "simulate") {
    override fun help(context: Context) = "バトルスピリッツ ゲームシミュレーション（初期状態表示）"

    override fun run() {
        val deck = mutableListOf(
            "BS75-CX03" to "天雷の契約神インドラ",
            "BS75-051" to "砲雷竜カノンヴァジュドラ",
            "BS75-042" to "幼電竜ビリジュヴァーン",
            "BS75-043" to "幼電竜ヴァジュドラコ",
            "BS75-044" to "雷鬣竜メーンヴァジュラ",
            "BS75-045" to "雷竜ライヴァジュドラ",
            "BS75-046" to "雷竜シマヴァジュドラ",
            "BS75-047" to "電磁竜マグジュヴァーン",
            "BS75-048" to "雷銃竜マガンのヴァジュドラ",
            "BS75-049" to "雷象竜アイラヴァジュドラ"
        )

        println("=== Battle Spirits CLI Simulator ===")
        println("Rule: Life=5, Reserve=3+1(SoulCore), Hand=1(Contract)+3")
        
        val hand = mutableListOf<String>()
        val contract = deck.find { it.first == "BS75-CX03" }
        if (contract != null) {
            hand.add(contract.second)
            deck.remove(contract)
        }
        
        deck.shuffle()
        repeat(3) {
            if (deck.isNotEmpty()) {
                hand.add(deck.removeAt(0).second)
            }
        }

        println("\n[Status]")
        println("Life:    [O][O][O][O][O] (5)")
        println("Reserve: [ ] [ ] [ ] (3) + [S] (Soul Core)")
        println("Trash:   (0)")
        
        println("\n[Hand] (${hand.size} cards)")
        hand.forEachIndexed { index, name ->
            println("${index + 1}: $name")
        }
        
        println("\n[Field]")
        println("| Slot 1 | Slot 2 | Slot 3 | Slot 4 | Slot 5 |")
        println("|  Empty |  Empty |  Empty |  Empty |  Empty |")
        println("======================================")
    }
}

fun main(args: Array<String>) = BsCli().subcommands(FetchCards(args.toList()), Neo(), Simulate()).main(args)
