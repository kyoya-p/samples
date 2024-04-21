import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

class ModelsTest {
    fun <T> T.assert1(v: T): T = also { assertTrue { this == v } }

    @Test
    fun model1() {
        val reqLogin: Request.LoginReq = Request.LoginReq("shokkaa", "***")
        val reqTerm: Request.Terminate = Request.Terminate
        val x: Request = reqLogin
        val y: Request = reqTerm
         Json.encodeToString(x).also(::println)
        val x1 = Json.encodeToString(x).assert1("""{"type":"Request.LoginReq","user":"shokkaa","password":"***"}""")
        val y1 = Json.encodeToString(y).assert1("""{"type":"Request.Terminate"}""")
        Json.decodeFromString<Request>(x1).assert1(Request.LoginReq(user = "shokkaa", password = "***"))
        Json.decodeFromString<Request>(y1).assert1(Request.Terminate)
        runCatching {
            Json.decodeFromString<Request>("""{"user":"shokkaa","password":"***"}""")
        }.onSuccess { throw Exception("Expected Failure") }
        Json.decodeFromString<Request.LoginReq>("""{"user":"shokkaa","password":"***"}""")
            .assert1(Request.LoginReq(user = "shokkaa", password = "***"))
    }
}