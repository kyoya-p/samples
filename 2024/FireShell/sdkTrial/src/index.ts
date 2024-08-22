// Import the functions you need from the SDKs you need
import { log } from "console";
import { FirebaseApp, initializeApp } from "firebase/app";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

import { getAuth,signInWithEmailAndPassword } from 'firebase/auth'
import { getFirestore, collection, getDocs } from 'firebase/firestore'

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyBg5ssUSPQlEKxZ6zoBrg-hwhoMzwWLQPQ",
  authDomain: "riot-7a79a.firebaseapp.com",
  projectId: "riot-7a79a",
  storageBucket: "riot-7a79a.appspot.com",
  messagingSenderId: "749774078339",
  appId: "1:749774078339:web:9d60dff9671ab8e9ad76b6"
}

// Initialize Firebase
const app = initializeApp(firebaseConfig)
const auth = getAuth(app)
const db = getFirestore(app)

function main() {
  const email=process.env["USERID"] || process.exit("Not exist: environment variable 'USERID'")
  const password=process.env["PASSWORD"] || process.exit("Not exist: environment variable 'PASSWORD'")

  auth.onAuthStateChanged((user)=>{
    if(user==null){signInWithEmailAndPassword(auth,email,password)}
    else{
      console.log(user)
    }
   })
}

main()
