import org.junit.jupiter.api.Test
import kotlin.reflect.full.*
import kotlin.time.ExperimentalTime


@ExperimentalTime
@Suppress("NonAsciiCharacters", "ClassName", "SpellCheckingInspection")
// Kotlin 1.6~
class `T22-refrection` {
    @Test
    fun t1_name() {
        println("simpleName=${A::class.simpleName}")
        println("qualifiedName=${A::class.qualifiedName}")
    }

    @Test
    fun t2_classed() {
        println("nestedClasses=${A::class.nestedClasses}")
    }

    @Test
    fun t3_funcs() {
        println("functions=${A::class.functions}")
        println("memberFunctions=${A::class.memberFunctions}")
        println("declaredFunctions=${A::class.declaredFunctions}")
        println("declaredMemberFunctions=${A::class.declaredMemberFunctions}")
        println("declaredMemberExtensionFunctions=${A::class.declaredMemberExtensionFunctions}")
        println("memberExtensionFunctions=${A::class.memberExtensionFunctions}")
        println("staticFunctions=${A::class.staticFunctions}")
    }

    @Test
    fun t4_props() {
        println("memberProperties=${A::class.memberProperties}")
        println("staticProperties=${A::class.staticProperties}")
        println("declaredMemberProperties=${A::class.declaredMemberProperties}")
        println("memberExtensionProperties=${A::class.memberExtensionProperties}")

    }

    @Test
    fun t5_companionObject() {
        println(A::class.companionObject?.members)
        println(A::class.companionObject?.declaredFunctions)
        println(A::class.supertypes)
    }

    @Test
    fun t6_misc() {
        println(Any::class)
    }


    @Suppress("unused")
    fun A.extFuncOfA() {}
    @Suppress("unused")
    val A.extPropOfA: Int get() = 1

    @Suppress("ClassName", "unused")
    class A {
        class sub1A
        class sub2A

        fun funOfA() {}
        val propOfA: Int = 1

        companion object {
            fun staticFunOfA() {}
        }
    }
}
