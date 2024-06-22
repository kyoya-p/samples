import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse


//fun main() {
//    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
//        .start(wait = true)
//}
//
//fun Application.module() {
//    routing {
//        get("/") {
////            call.respondText("Ktor: ${Greeting().greet()}")
//            val servlet = HelloServlet()
//            val request = call.request as HttpServletRequest
//            val response = call.response as HttpServletResponse
//            servlet.doGet(request, response)
//        }
//    }
//}

@WebServlet("/echo")
class HelloServlet : HttpServlet() {
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.writer.write("Hello, query='${req.queryString}'")
    }
}

