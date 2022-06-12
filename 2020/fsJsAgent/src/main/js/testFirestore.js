var firebase = require("firebase/app");
require("firebase/auth");
require("firebase/firestore");

firebase.initializeApp({
        apiKey: "AIzaSyDrO7W7Sb6RCpHTsY3GaP-zODRP_HtY4nI",
        authDomain: "road-to-iot.firebaseapp.com",
        projectId: "road-to-iot"
    });

firebase.auth().signInWithEmailAndPassword("kyoya.p4@gmail.com", "kyoyap4").catch(function(error) {
  var errorCode = error.code;
  var errorMessage = error.message;
  console.log(errorCode);
  console.log(errorMessage);
});

firebase.auth().onAuthStateChanged(function(user) {
  if (user) {
    console.log("Signed-in: "+user.email);
    dbOperation()
  } else {
    console.log("Not signed-in");
  }
});

function dbOperation() {
    var db = firebase.firestore();
    var docRef = db.collection("device").doc("agent1");

    docRef.get().then(function(doc) {
        if (doc.exists) {
            console.log("Document data:", doc.data());
            var data=doc.data()
            console.log("data.deviceId:", data.deviceId);
            console.log("data.deviceId:", data["deviceId"]);
        } else {
            console.log("No such document!");
        }
    }).catch(function(error) {
        console.log("Error getting document:", error);
    });
}
