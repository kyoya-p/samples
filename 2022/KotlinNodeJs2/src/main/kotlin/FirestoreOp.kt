val firebaseConfig = mapOf(
    "apiKey" to "AIzaSyCiiIwgR3-hqUrIeCCdmudOr2nKwmviSyU",
    "authDomain" to "road-to-iot.firebaseapp.com",
    "databaseURL" to "https://road-to-iot.firebaseio.com",
    "projectId" to "road-to-iot",
    "storageBucket" to "road-to-iot.appspot.com",
    "messagingSenderId" to "307495712434",
    "appId" to "1:307495712434:web:6e83ae7a3698dba6f33bab",
    "measurementId" to "G-1N1NL488BZ"
)

val app = initializeApp(firebaseConfig)
val db = getFirestore(app)
