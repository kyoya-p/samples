import kotlin.reflect.KClass
import kotlin.reflect.full.*

fun AppMain() {
    refl_ListSubClasses()
}

open class A
class A1 : A()
class A2 : A()

fun refl_ListSubClasses() {
    val classA: KClass<A> = A::class
    println("isData: ${classA.isData}")
    println("isSealed: ${classA.isSealed}")
    println("isInner: ${classA.isInner}")
    println("isAbstract: ${classA.isAbstract}")
    println("isCompanion: ${classA.isCompanion}")
    println("isFinal: ${classA.isFinal}")
    println("isOpen: ${classA.isOpen}")
    println("isFun: ${classA.isFun}")
    println("isValue: ${classA.isValue}")
    println("sealedSubclasses: ${classA.sealedSubclasses}")
    println("supertypes: ${classA.supertypes}")
    println("constructors: ${classA.constructors}")
    println("nestedClasses: ${classA.nestedClasses}")
    println("declaredMemberProperties: ${classA.declaredMemberProperties}")
    println("declaredFunctions: ${classA.declaredFunctions}")
    println("A functions: ${classA.functions}")
    println("A memberFunctions: ${classA.memberFunctions}")

    getAllSubclasses(classA).forEach {
        println(it)
    }
}

fun findSubclasses(clazz: KClass<*>) {
    val subclasses = mutableSetOf<KClass<out T>>()
    clazz.nestedClasses.forEach { nested ->
        findSubclasses(nested)
    }
    // 直接のサブクラスを追加
    if (clazz.isSubclassOf(kClass)) {
        @Suppress("UNCHECKED_CAST")
        subclasses.add(clazz as KClass<out T>)
    }
    // クラスローダーからクラス情報を取得して、サブクラスを探索
    val classLoader = Thread.currentThread().contextClassLoader
    val allClasses = classLoader.definedClasses
    allClasses.forEach { definedClass ->
        val definedKClass = definedClass.kotlin
        if (definedKClass.isSubclassOf(clazz)){
            findSubclasses(definedKClass)
        }
    }
}
