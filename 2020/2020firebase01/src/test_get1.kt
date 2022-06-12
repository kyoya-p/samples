import com.google.api.core.ApiFuture
import com.google.cloud.firestore.*
import java.util.*

fun main() {

    test_get_1()
    test_get_1()
    test_get_1()
    test_get_1()

    test_get_2()

    test_get_3()
}

fun test_get_1() {
    val db = FirestoreOptions.getDefaultInstance().getService()
    val start = Date().time

    val query: ApiFuture<QuerySnapshot> = db.collection("users2").limit(500).get()

    val querySnapshot: QuerySnapshot = query.get()
    val documents: List<QueryDocumentSnapshot> = querySnapshot.getDocuments()
    for (document in documents) {
        val id = document.getId()
        val first = document.getString("first")
        val last = document.getString("last")
        val born = document.getLong("born")
    }
    val period = Date().time - start
    val s = documents.size
    println("get() $s[doc]: ${period * 1.0 / s} [ms/doc]")
}

fun test_get_2() {
    val db = FirestoreOptions.getDefaultInstance().getService()
    val start = Date().time

    val query: ApiFuture<QuerySnapshot> = db.collection("users2").limit(1000).get()

    val querySnapshot: QuerySnapshot = query.get()
    val documents: List<QueryDocumentSnapshot> = querySnapshot.getDocuments()
    for (document in documents) {
        val id = document.getId()
        val first = document.getString("first")
        val last = document.getString("last")
        val born = document.getLong("born")
    }

    val period = Date().time - start
    val s = documents.size
    println("get() $s[doc]: ${period * 1.0 / s} [ms/doc]")
}

fun test_get_3() {
    val db = FirestoreOptions.getDefaultInstance().getService()
    val start = Date().time

    val query: ApiFuture<QuerySnapshot> = db.collection("users2").orderBy("born").limit(1000).get()

    val querySnapshot: QuerySnapshot = query.get()
    val documents: List<QueryDocumentSnapshot> = querySnapshot.getDocuments()
    for (document in documents) {
        val id = document.getId()
        val first = document.getString("first")
        val last = document.getString("last")
        val born = document.getLong("born")
    }

    val period = Date().time - start
    val s = documents.size
    println("get() $s[doc] Sorted: ${period * 1.0 / s} [ms/doc]")
}