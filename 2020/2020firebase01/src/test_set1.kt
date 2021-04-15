import com.google.api.core.ApiFuture
import com.google.cloud.firestore.*
import java.util.*
import kotlin.random.Random

val rnd = Random.Default

fun main() {
    val db = FirestoreOptions.getDefaultInstance().getService()

    test_set_1(db)
    test_set_2(db)
    test_set_3(db)

}

fun test_set_1(db: Firestore) {
    val start = Date().time
    for (i in 0..99) {
        val docRef: DocumentReference = db.collection("users2").document()
        val data: MutableMap<String, Any> = HashMap()
        data["first"] = "Ada"
        data["last"] = "Lovelace"
        data["born"] =rnd.nextInt()
        val result: ApiFuture<WriteResult> = docRef.set(data)
        result.get() //処理終了待
    }

    val period = Date().time - start
    println("set() 直列 100[query]: ${period / 100.0} [ms/doc]")
}


fun test_set_2(db: Firestore) {
    val start = Date().time
    (0 until 100).map {
        val docRef: DocumentReference = db.collection("users2").document()
        val data: MutableMap<String, Any> = HashMap()
        data["first"] = "Ada"
        data["last"] = "Lovelace"
        data["born"] =rnd.nextInt()
        docRef.set(data)
    }.map {
        it.get() //処理終了待
    }

    val period = Date().time - start
    println("set() 非同期 100[query]: ${period / 100.0} [ms/doc]")
}

fun test_set_3(db: Firestore) {
    val start = Date().time
    (0 until 500).map {
        val docRef: DocumentReference = db.collection("users2").document()
        val data: MutableMap<String, Any> = HashMap()
        data["first"] = "Ada"
        data["last"] = "Lovelace"
        data["born"] =rnd.nextInt()
        docRef.set(data)
    }.map {
        it.get() //処理終了待
    }

    val period = Date().time - start
    println("set() 非同期 500[query]: ${period / 500.0} [ms/doc]")
}
