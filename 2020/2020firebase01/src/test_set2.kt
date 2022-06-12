import com.google.api.core.ApiFuture
import com.google.cloud.firestore.*
import java.util.*
import kotlin.random.Random

//val rnd = Random.Default

fun main() {

    test_set2_1()
    test_set2_3()

}

fun test_set2_1() {
    val db = FirestoreOptions.getDefaultInstance().getService()

    val start = Date().time
    for (i in 0..99) {
        val docRef: DocumentReference = db.collection("perfTest").document("UniqDoc")
        val data: MutableMap<String, Any> = HashMap()
        data["first"] = "Ada"
        data["last"] = "Lovelace"
        data["born"] = rnd.nextInt()
        val result: ApiFuture<WriteResult> = docRef.set(data)
        result.get() //処理終了待
    }

    val period = Date().time - start
    println("set() 100[query] 単一doc seq wait: ${period / 100.0} [ms/doc]")
}


fun test_set2_3() {
    val db = FirestoreOptions.getDefaultInstance().getService()

    val start = Date().time
    val ress = (0..99).map {
        val docRef: DocumentReference = db.collection("perfTest").document("UniqDoc")
        val data: MutableMap<String, Any> = HashMap()
        data["first"] = "Ada"
        data["last"] = "Lovelace"
        data["born"] = rnd.nextInt()
        docRef.set(data)
    }

    val period = Date().time - start
    println("set() 100[query] 単一doc para nowait: ${period / 100.0} [ms/doc]")
    ress.forEach { it.get() } //終了待ち
    println("set() 100[query] 単一doc para wait: ${period / 100.0} [ms/doc]")
}

