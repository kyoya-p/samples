package com.example

import com.example.plugins.configureRouting
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting()
        }
        client.get("/").apply {
            println("URL:${request.url}")
            println("H:${request.headers}")
            println("M:${request.method}")
            println("Body:${request.content}")
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun customTest() {
        customTest(url = "/header_test", httpMethod = HttpMethod.Get) { response ->
            response.apply {
                assertEquals(HttpStatusCode.OK, status)
                // Authorization ヘッダの中身が返却されることを確認
                assertEquals("Bearer token", bodyAsText())
            }
        }
    }
}

fun customTest(
    url: String,
    httpMethod: HttpMethod,
    body: Any? = null,
    assertBlock: suspend (response: HttpResponse) -> Unit
) {
    return runBlocking {
        val testApp = TestApplication {
            application {
                configureRouting()
            }
        }
        try {
            val testClient = testApp.createClient {
                install(DefaultRequest) {
                    header("Authorization", "Bearer token") // ちゃんとした jwt を生成して設定する
                    contentType(ContentType.Application.Json)
                }
                install(ContentNegotiation) {
                    json {
                        // jackson の設定を追記する
                    }
                }
            }
            runBlocking {
                val response = testClient.request(urlString = url) {
                    method = httpMethod
                    setBody(body)
                }
                assertBlock(response)
            }
        } finally {
            testApp.stop()
        }
    }
}
