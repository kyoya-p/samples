package mibtool.snmp4jWrapper

import java.math.BigInteger
import java.net.InetAddress

fun main() {
    val start = "250.253.254.255"
    val end = "::1:ffff:255.255.255.255"
    scanIpRange(start.toInetAddr(), end.toInetAddr()).take(10).forEach {
        print(it.javaClass.canonicalName)
        println(it)
    }
}

fun String.toInetAddr() = InetAddress.getByName(this)!!
fun InetAddress.toBigInt() = BigInteger(address)
fun BigInteger.toInetAddr() = (ByteArray(16) + toByteArray()).takeLast(if (toByteArray().size <= 4) 4 else 16).toByteArray().let {
    InetAddress.getByAddress(it)!!
}

fun scanIpRange(start: InetAddress, end: InetAddress) = generateSequence(start.toBigInt()) { it + BigInteger.ONE }.takeWhile { it <= end.toBigInt() }.map { it.toInetAddr() }
fun scanIpRange(start: String, end: String) = scanIpRange(InetAddress.getByName(start), InetAddress.getByName(end))
