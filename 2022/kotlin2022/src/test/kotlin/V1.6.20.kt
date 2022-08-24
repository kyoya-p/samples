@file:Suppress("UNUSED_VARIABLE", "UseExpressionBody")

import org.junit.jupiter.api.Test
import java.io.File


@Suppress("ClassName", "NonAsciiCharacters", "TestFunctionName")
class V1_6_20 {
    @Test
    fun nullを許容しないジェネリクス型() { // Kotlin 1.6.20~
        fun <T> nullable(x: T): T = x
        fun <T> nonnullable(x: T & Any): T & Any = x

        val n1: String = nullable("NonNull")
        val n2: Nothing? = nullable(null)

        val nn1: String = nonnullable("NonNull")
        // val nn2 :Nothing? = nonnullable(null) // Compile Error
    }

    interface OldEdge<N> {
        fun nodes(): Pair<N, N>
    }

    fun interface Edge<N> {
        fun nodes(): Pair<N, N> // メソッドは1個だけ
    }

    @Test
    fun 関数インタフェースと実装() {
        // val oldEdge = OldEdge { "node1" to "node2" } // Compile Error: コンストラクタがない
        val oldEdge = object : OldEdge<String> {
            override fun nodes() = "node1" to "node2"
        }
        println(oldEdge.nodes())

        val edge = Edge { "node1" to "node2" } // こう書ける

        println(edge.nodes())
    }


    // コンテキストレシーバ
    // 1.6.20ではコンパイルオプション -Xcontext-receivers と、
    // languageVersion = "1.7" が必要(build.gradle.kts参照)
    fun interface CurrentDirectoryContext {
        fun cwd(): File
    }

    context(CurrentDirectoryContext)
    fun ls() = if (cwd().isDirectory()) cwd().listFiles()!!.mapNotNull { it }.asSequence() else sequenceOf()

    context(CurrentDirectoryContext)
    fun <R> cd(dir: File, op: CurrentDirectoryContext.() -> R) = CurrentDirectoryContext { dir }.op()

    @Test
    fun コンテキストレシーバ() {
        with(CurrentDirectoryContext { File(".") }) {
            ls().forEach {
                cd(it) {// Context: カレントディレクトリがサブディレクトリになってる
                    println(cwd().absolutePath)
                }
            }
        }
    }

    @Test
    fun 複数のコンテキストレシーバ() {
        with(A()) { // context A
            //f()  // compile error: No required context receiver found
            with(B()) { // context A+B
                f()
            }
        }
    }

    class A {
        fun a(): String = "A"
    }

    class B {
        fun b(): String = "B"
    }

    context(A, B)
    fun f() {
        println(a())
        println(b())
    }
}

