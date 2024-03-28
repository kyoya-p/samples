import org.openapitools.client.apis.PetApi
import org.openapitools.client.models.Category
import org.openapitools.client.models.Pet

fun scenario1() {
    println("Hello World!")

    val Cat = Category(name = "Cat")
    val Mouse = Category(name = "Mouse")

    val petApi = PetApi("http://localhost/")
    petApi.addPet(Pet(name = "Tom", photoUrls = listOf(), category = Cat))
    petApi.addPet(Pet(name = "Jerry", photoUrls = listOf(), category = Mouse))

}

