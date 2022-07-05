// Set/Mapの重複チェックは、hashCodeとequalsで行う

class X0(val id: Int) //メンバ(id)がキーにはならない

class X1(val id: Int) { // これでメンバ(id)がキーとなる
    override fun hashCode() = id
    override fun equals(other: Any?) = id == (other as X1).id
}

class X2(val id: Int) : Comparable<X2> { // Comparableを実装してもダメ
    override fun compareTo(other: X2): Int = id - other.id
}

class X3(val id: Int) // 拡張関数でhashCodeやequalsをつけてもダメ

fun X3.hashCode() = id
fun X3.equals(other: Any?) = id == (other as X3).id

data class X4(val id: Int) // data classとすれば、自動的にhashCodeやequalsを用意してくれる

data class XX0(val v1: List<X0>) //インスタンスごとに固有のhashCodeではキーにはならない(固有インスタンスを参照してキーにするのはアリ)
data class XX4(val v1: List<X4>) //メンバがhashCode値を持てば、SetもhashCode値を持つ

fun main() {
    // Setの重複排除の様子
    val sx0 = setOf(X0(1), X0(2), X0(3), X0(2))
    val sx1 = setOf(X1(1), X1(2), X1(3), X1(2))
    val sx2 = setOf(X2(1), X2(2), X2(3), X2(2))
    val sx3 = setOf(X3(1), X3(2), X3(3), X3(2))
    val sx4 = setOf(X4(1), X4(2), X4(3), X4(2))
    println(sx0)
    println(sx1)
    println(sx2)
    println(sx3)
    println(sx4)

    // 多重コンテナの様子
    val ssi = setOf(setOf(1, 2, 2), setOf(2, 1, 2))
    val ssx0 = setOf(setOf(X0(1), X0(2), X0(2)), setOf(X0(2), X0(1), X0(2)))
    val ssx4 = setOf(setOf(X4(1), X4(2), X4(2)), setOf(X4(2), X4(1), X4(2)))
    println(ssi)
    println(ssx0)
    println(ssx4)

    // 同一性比較
    val lsx4 = listOf(setOf(X4(1), X4(2), X4(2)), setOf(X4(2), X4(1), X4(2)))
    val lsx4_2 = listOf(setOf(X4(1), X4(2), X4(2)), setOf(X4(2), X4(1), X4(2)))
    assert(lsx4 == lsx4_2) //同一
    val lsx0 = listOf(setOf(X0(1), X0(2), X0(2)), setOf(X0(2), X0(1), X0(2)))
    val lsx0_2 = listOf(setOf(X0(1), X0(2), X0(2)), setOf(X0(2), X0(1), X0(2)))
    assert(lsx0 == lsx0_2) //異なる

    //hashCode
    assert(listOf(X0(1)) != listOf(X0(1))) //異なる(hashCodeを持つ要素のリスト)
    assert(listOf(X1(1)) == listOf(X1(1))) //同じ(hashCodeを持つ要素のリスト)

    val x0 = X0(1)
    val x01 = X0(1)
    assert(listOf(x0) == listOf(x0)) //同じ(hashCodeを持つ要素のリスト)
    assert(listOf(x0) != listOf(x01)) //異なる(hashCodeを持つ要素のリスト)
}
