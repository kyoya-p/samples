import com.google.api.core.ApiFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.*
import com.google.common.collect.ImmutableMap
import java.util.*

fun main() {
    val db: Firestore = FirestoreOptions.getDefaultInstance().getService()

}

class Quickstart {
    private var db: Firestore

    /**
     * Initialize Firestore using default project ID.
     */
    constructor() {
        // [START fs_initialize]
        val db: Firestore = FirestoreOptions.getDefaultInstance().getService()
        // [END fs_initialize]
        this.db = db
    }

    /**
     * Add named test documents with fields first, last, middle (optional), born.
     *
     * @param docName document name
     */
    @Throws(Exception::class)
    fun addDocument(docName: String?) {
        when (docName) {
            "alovelace" -> {

                // [START fs_add_data_1]
                val docRef: DocumentReference = db.collection("users").document("alovelace")
                // Add document data  with id "alovelace" using a hashmap
                val data: MutableMap<String, Any> = HashMap()
                data["first"] = "Ada"
                data["last"] = "Lovelace"
                data["born"] = 1815
                //asynchronously write data
                val result: ApiFuture<WriteResult> = docRef.set(data)
                // ...
                // result.get() blocks on response
                System.out.println("Update time : " + result.get().getUpdateTime())
            }
            "aturing" -> {

                // [START fs_add_data_2]
                val docRef: DocumentReference = db.collection("users").document("aturing")
                // Add document data with an additional field ("middle")
                val data: MutableMap<String, Any> = HashMap()
                data["first"] = "Alan"
                data["middle"] = "Mathison"
                data["last"] = "Turing"
                data["born"] = 1912
                val result: ApiFuture<WriteResult> = docRef.set(data)
                System.out.println("Update time : " + result.get().getUpdateTime())
            }
            "cbabbage" -> {
                val docRef: DocumentReference = db.collection("users").document("cbabbage")
                val data: Map<String, Any> = ImmutableMap.Builder<String, Any>()
                        .put("first", "Charles")
                        .put("last", "Babbage")
                        .put("born", 1791)
                        .build()
                val result: ApiFuture<WriteResult> = docRef.set(data)
                System.out.println("Update time : " + result.get().getUpdateTime())
            }
            else -> {
            }
        }
    }

    @Throws(Exception::class)
    fun runAQuery() {
        // [START fs_add_query]
        // asynchronously query for all users born before 1900
        val query: ApiFuture<QuerySnapshot> = db.collection("users").whereLessThan("born", 1900).get()
        // ...
        // query.get() blocks on response
        val querySnapshot: QuerySnapshot = query.get()
        val documents: List<QueryDocumentSnapshot> = querySnapshot.getDocuments()
        for (document in documents) {
            System.out.println("User: " + document.getId())
            System.out.println("First: " + document.getString("first"))
            if (document.contains("middle")) {
                System.out.println("Middle: " + document.getString("middle"))
            }
            System.out.println("Last: " + document.getString("last"))
            System.out.println("Born: " + document.getLong("born"))
        }
        // [END fs_add_query]
    }

    @Throws(Exception::class)
    fun retrieveAllDocuments() {
        // [START fs_get_all]
        // asynchronously retrieve all users
        val query: ApiFuture<QuerySnapshot> = db.collection("users").get()
        // ...
        // query.get() blocks on response
        val querySnapshot: QuerySnapshot = query.get()
        val documents: List<QueryDocumentSnapshot> = querySnapshot.getDocuments()
        for (document in documents) {
            System.out.println("User: " + document.getId())
            System.out.println("First: " + document.getString("first"))
            if (document.contains("middle")) {
                System.out.println("Middle: " + document.getString("middle"))
            }
            System.out.println("Last: " + document.getString("last"))
            System.out.println("Born: " + document.getLong("born"))
        }
        // [END fs_get_all]
    }

    @Throws(Exception::class)
    fun run() {
        val docNames = arrayOf("alovelace", "aturing", "cbabbage")

        // Adding document 1
        println("########## Adding document 1 ##########")
        addDocument(docNames[0])

        // Adding document 2
        println("########## Adding document 2 ##########")
        addDocument(docNames[1])

        // Adding document 3
        println("########## Adding document 3 ##########")
        addDocument(docNames[2])

        // retrieve all users born before 1900
        println("########## users born before 1900 ##########")
        runAQuery()

        // retrieve all users
        println("########## All users ##########")
        retrieveAllDocuments()
        println("###################################")
    }

    companion object {
        /**
         * A quick start application to get started with Firestore.
         *
         * @param args firestore-project-id (optional)
         */
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val quickStart = Quickstart()
            quickStart.run()
        }
    }
}