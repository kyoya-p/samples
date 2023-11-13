import com.azure.core.http.policy.HttpLogDetailLevel
import com.azure.core.management.AzureEnvironment
import com.azure.core.management.Region
import com.azure.core.management.profile.AzureProfile
import com.azure.identity.ClientSecretCredentialBuilder
import com.azure.resourcemanager.AzureResourceManager
import com.azure.resourcemanager.storage.models.StorageAccountSkuType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


fun main(args: Array<String>) {
    val subscriptionId = System.getenv("AZURE_SUBSCRIPTION_ID") // subscription

    class AzureAd(val appId: String, val displayName: String, val password: String,val tenant: String)

    // export AZURE_AD=`az ad sp create-for-rbac` #の結果を回収
    val ad = Json.decodeFromString<AzureAd>(System.getenv("AZURE_AD"))
    val creds = ClientSecretCredentialBuilder()
        .tenantId(ad.tenant)
        .clientId(ad.appId)
        .clientSecret(ad.password)
        .build()

    val profile = AzureProfile(AzureEnvironment.AZURE)

    val azureResourceManager = AzureResourceManager
        .configure()
        .withLogLevel(HttpLogDetailLevel.BASIC)
        .authenticate(creds, profile)
        .withSubscription(subscriptionId)

    val storageAccountName = "app231101"
    val rgName = "rgx"
    val account = azureResourceManager.storageAccounts().define(storageAccountName)
        .withRegion(Region.US_WEST)
        .withNewResourceGroup(rgName)
        .withSku(StorageAccountSkuType.STANDARD_LRS)
        .create()

}
