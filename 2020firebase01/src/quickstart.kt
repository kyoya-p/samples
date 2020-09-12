/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.firestore
// [START fs_include_dependencies]
// [END fs_include_dependencies]
import com.google.api.core.ApiFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.QueryDocumentSnapshot
import com.google.cloud.firestore.QuerySnapshot
import com.google.cloud.firestore.WriteResult
import com.google.common.collect.ImmutableMap
import java.util.*

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions


/**
 * A simple Quick start application demonstrating how to connect to Firestore
 * and add and query documents.
 */
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

    constructor(projectId: String?) {
        // [START fs_initialize_project_id]
        val firestoreOptions: FirestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build()
        val db: Firestore = firestoreOptions.getService()
        // [END fs_initialize_project_id]
        this.db = db
    }

    fun getDb(): Firestore {
        return db
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
            // default project is will be used if project-id argument is not available
            val projectId = if (args.size == 0) null else args[0]
            val quickStart = projectId?.let { Quickstart(it) } ?: Quickstart()
            quickStart.run()
        }
    }
}