@file:Suppress("unused")

package smpl

import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import kotlin.reflect.KClass


fun main() {
    val packageName = "smpl"
    val refs = Reflections(
        ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(packageName))
            .setScanners(SubTypesScanner(false), TypeAnnotationsScanner())
    )
    val classes = refs.getSubTypesOf(Any::class.java).map { it.kotlin }

    classes.forEach { c ->
        println("${c.supertypes.joinToString()}.${c.simpleName}")
        c.members.forEach { println(it) }
    }

    listClassInfo(A1::class)
}

open class A
class A1(val pa11: Int) : A() {
    val pa12 = 1
    fun fa1() {}
}

class A2(val pa21: Int) : A() {
    val pa22 = 1
    fun fa2() {}
}

// KClass クラス自身をリフレクションし KClass.is~関数をすべて実行する
inline fun <reified T : Any> listClassInfo(clazz: KClass<T>) {
    println("Class: ${clazz.qualifiedName}")
    KClass::class.members.forEach { m ->
        if (m.name.startsWith("is")) println("${m.name}: ${m.call(clazz)}")
    }
//    println("isData: ${classA.isData}")
//    println("isSealed: ${classA.isSealed}")
//    println("isInner: ${classA.isInner}")
//    println("isAbstract: ${classA.isAbstract}")
//    println("isCompanion: ${classA.isCompanion}")
//    println("isFinal: ${classA.isFinal}")
//    println("isOpen: ${classA.isOpen}")
//    println("isFun: ${classA.isFun}")
//    println("isValue: ${classA.isValue}")
//    println("sealedSubclasses: ${classA.sealedSubclasses}")
//    println("supertypes: ${classA.supertypes}")
//    println("constructors: ${classA.constructors}")
//    println("nestedClasses: ${classA.nestedClasses}")
//    println("declaredMemberProperties: ${classA.declaredMemberProperties}")
//    println("declaredFunctions: ${classA.declaredFunctions}")
//    println("A functions: ${classA.functions}")
//    println("A memberFunctions: ${classA.memberFunctions}")
}
