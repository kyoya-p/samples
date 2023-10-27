import com.azure.core.http.policy.HttpLogDetailLevel
import com.azure.core.management.AzureEnvironment
import com.azure.core.management.Region
import com.azure.core.management.profile.AzureProfile
import com.azure.identity.ClientSecretCredentialBuilder
import com.azure.resourcemanager.AzureResourceManager
import com.azure.resourcemanager.storage.models.StorageAccountSkuType


fun main(args: Array<String>) {
    // az ad sp create-for-rbac コマンドの結果を回収
    val clientId = System.getenv("AZURE_APP_ID") // "appId":
    val clientSecret = System.getenv("AZURE_PASSWORD")  // "password":
    val tenantId = System.getenv("AZURE_TENANT") // "tenant":
    val subscriptionId = System.getenv("AZURE_SUBSCRIPTION") // subscription

    val creds = ClientSecretCredentialBuilder()
        .tenantId(tenantId)
        .clientId(clientId)
        .clientSecret(clientSecret)
        .build()

    val profile = AzureProfile(AzureEnvironment.AZURE)

    val azureResourceManager = AzureResourceManager
        .configure()
        .withLogLevel(HttpLogDetailLevel.BASIC)
        .authenticate(creds, profile)
        .withSubscription(subscriptionId)

    val storageAccountName = "app231027"
    val rgName = "rgx"
    val account = azureResourceManager.storageAccounts().define(storageAccountName)
        .withRegion(Region.US_WEST)
        .withNewResourceGroup(rgName)
        .withSku(StorageAccountSkuType.STANDARD_LRS)
        .create()
}
