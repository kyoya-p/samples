

import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import kotlin.reflect.KClass


fun main() {
    val packageName = "sample"
    val refs = Reflections(
        ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(packageName))
            .setScanners(SubTypesScanner(false), TypeAnnotationsScanner())
    )
    val classes = refs.getSubTypesOf(Any::class.java).map { it.kotlin }

    classes.forEach { c ->
        println("${c.supertypes.joinToString()}.${c.simpleName}")
        c.members.forEach { println(it) }
    }

    listClassInfo(sample.A1::class)
}

// リフレクションによりKClass#is~関数のうち引数を与えないものすべて抽出し実行する
inline fun <reified T : Any> listClassInfo(clazz: KClass<T>) {
    KClass::class.members.forEach { m ->
        if (m.name.startsWith("is") && m.parameters.size <= 1) println("${m.name}: ${m.call(clazz)}")
    }

//    すなわち
//    println("isData: ${clazz.isData}")
//    println("isSealed: ${clazz.isSealed}")
//    println("isInner: ${clazz.isInner}")
//    println("isAbstract: ${clazz.isAbstract}")
//    println("isCompanion: ${clazz.isCompanion}")
//    println("isFinal: ${clazz.isFinal}")
//    println("isOpen: ${clazz.isOpen}")
//    println("isFun: ${clazz.isFun}")
//    println("isValue: ${clazz.isValue}")
}
