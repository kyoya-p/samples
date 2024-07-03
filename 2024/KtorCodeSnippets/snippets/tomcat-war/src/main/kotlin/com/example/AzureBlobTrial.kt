import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import java.io.FileWriter
import java.time.Instant.now


val cc =System.getenv("AZURE_BLOB_CONNECTION")!!

val blobServiceClient: BlobServiceClient = BlobServiceClientBuilder()
    .connectionString(cc)
    .buildClient()
val blobContainerClient = blobServiceClient.createBlobContainer("container${now().toEpochMilli()}")
val blobClient = blobContainerClient.getBlobClient("uploaded.txt")

fun main() = upload()

fun upload() {
    val fileName = "dummy.txt"
    val writer = FileWriter(fileName)
    repeat(200 * 1024 * 1024) { writer.write("0") }
    writer.close()
    blobClient.uploadFromFile(fileName)
}

