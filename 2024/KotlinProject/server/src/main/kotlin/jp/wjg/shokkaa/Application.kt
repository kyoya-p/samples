package jp.wjg.shokkaa

import SERVER_PORT
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import jakarta.servlet.ServletException
import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import java.util.*


fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    routing {
        get("/") {
//            call.respondText("Ktor: ${Greeting().greet()}")
            val servlet = HelloServlet()
            val request = call.request as HttpServletRequest
            val response = call.response as HttpServletResponse
            servlet.doGet(request, response)
        }
    }
}

class HelloServlet : HttpServlet() {
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.writer.write("Hello, world!")
    }
}


@WebServlet("/MyDate")
class MyDate : HttpServlet() {
    var youbi: Array<String> = arrayOf("日", "月", "火", "水", "木", "金", "土")

    @Throws(ServletException::class, IOException::class)
    override fun doGet(
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        response.contentType = "text/html; charset=UTF-8"
        val out = response.writer
        out.println("<html><head></head><body>")

        val cal = Calendar.getInstance()
        out.printf(
            "%d年%d月%d日%s曜日%d時%d分%d秒%n",
            cal[Calendar.YEAR],
            cal[Calendar.MONTH] + 1,
            cal[Calendar.DAY_OF_MONTH],
            youbi[cal[Calendar.DAY_OF_WEEK] - 1],
            cal[Calendar.HOUR_OF_DAY],
            cal[Calendar.MINUTE],
            cal[Calendar.SECOND]
        )

        out.println("</body></html>")
        out.close()
    }
}