package main

fun main() {
    val (n, q) = readLine()!!.split(" ").map { it.toInt() }
    val r = ('A'..'Z').take(n).sortedWith(object : Comparator<Char> {
        override fun compare(p0: Char?, p1: Char?): Int {
            println("? $p0 $p1")
            return if (readLine() == ">") -1 else 1
        }
    }).joinToString("")
    println("! $r")
}
