import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class Tests : FunSpec({
    test("最大値を求める") {
        forAll(
            row(1, 2, 2),
            row(5, 3, 5),
            row(-1, -2, -1)
        ) { a, b, expected ->
            maxOf(a, b) shouldBe expected
        }
    }
    test("クラス情報") {
        refl_ListSubClasses()
    }
})

