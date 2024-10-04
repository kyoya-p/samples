// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
const firebaseConfig = {
    apiKey: process.env.GOOGLE_API_KEY,
    authDomain: "sample-firebase-ai-app-d6f72.firebaseapp.com",
    projectId: "sample-firebase-ai-app-d6f72",
    storageBucket: "sample-firebase-ai-app-d6f72.appspot.com",
    messagingSenderId: "582856142072",
    appId: "1:582856142072:web:281be3068de8ef720c1f0d"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
