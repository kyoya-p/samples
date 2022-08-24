import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable


val url = "http://172.29.240.236:8090/srdm/projects.json?key=dbce860f7ef8cb6e183f97edd545190e37599f64"

@Serializable
data class Parent(val id: Int, val name: String)

@Serializable
data class Project(
    val id: Int,
    val name: String,
    val identifier: String,
    val description: String,
    val parent: Parent?=null,
    val status: Int,
    val is_public: Boolean=false,
    val created_on: String,
    val updated_on: String,
)

@Serializable
data class ProjectsRes(
    val projects: List<Project>,
    val total_count: Int,
    val offset: Int,
    val limit: Int,
)

suspend fun main() {

    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = defaultSerializer()
        }
    }


    val r2: String = client.get(url)
    println(r2)
    val r: ProjectsRes = client.get(url)
    println(r)
}