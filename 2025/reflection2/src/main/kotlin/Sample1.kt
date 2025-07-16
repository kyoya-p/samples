import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberExtensionFunctions
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.superclasses

interface Interface
open class SuperClass
class Target<T>(val v: T?) : SuperClass(), Interface {
    class SubClass

    fun f(x: Int) = x * 2
}

fun <T> Target<T>.extFunc() = println(v)


@Suppress("UNCHECKED_CAST")
fun main() {
    describeClass(Target(if (true) listOf(1) else mapOf(1 to 1)))
}

fun describeClass(target: Any) {
    val kClass = target::class

    val isInstance: Boolean = kClass.isInstance(target) // クラスのインスタンスか判定
    val simpleName: String? = kClass.simpleName // クラス名
    val qualifiedName: String? = kClass.qualifiedName // 詳細クラス名
    val superClass: List<KClass<*>> = kClass.superclasses // 継承するクラス・インタフェース
    val superType: List<KType> = kClass.supertypes // 継承する型
    val nestedClass: Collection<KClass<*>> = kClass.nestedClasses // クラス内で定義されたクラス
    val members: Collection<KCallable<*>> = kClass.members // 呼び出し可能なメンバ(関数と変数)
    val functions: Collection<KFunction<*>> = kClass.memberFunctions // メンバ関数
    val properties: Collection<KProperty1<*, *>> = kClass.memberProperties // メンバ変数
    val constructor: Collection<KFunction<*>> = kClass.constructors // 全コンストラクタ
    val primaryConstructor: KFunction<*>? = kClass.primaryConstructor // プライマリコンストラクタ
    val typeParameters: List<KTypeParameter> = kClass.typeParameters // 型引数

// 型引数の情報
    val kTypeTP1 = kClass.starProjectedType.arguments.also(::println).first().also(::println).type!!
    kTypeTP1.classifier.also(::println)
    val kClassTP1: KClass<*> = kTypeTP1.classifier as KClass<*>
    val kClassifier = kClassTP1.simpleName.also(::println)
}

