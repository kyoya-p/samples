@file:JsModule("firebase/firestore")
@file:JsNonModule

package firebase.firestore

import firebase.app.FirebaseApp

external interface Firestore

external fun getFirestore(app: FirebaseApp): Firestore

external fun collection(firestore: Firestore, path: String): dynamic

external fun getDocs(query: dynamic): dynamic // Returns Promise

external fun addDoc(collectionRef: dynamic, data: dynamic): dynamic // Returns Promise<DocumentReference>

