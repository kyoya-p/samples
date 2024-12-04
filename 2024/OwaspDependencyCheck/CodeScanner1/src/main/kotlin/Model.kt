import kotlinx.serialization.Serializable

@Serializable
data class Bom(
    val bomFormat: String,
    val specVersion: String,
    val serialNumber: String,
    val version: Int,
    val metadata: Metadata,
    val components: List<Component>,
    val dependencies: List<Dependency>
)

@Serializable
data class Metadata(
    val timestamp: String,
    val tools: List<Tool>,
    val component: Component
)

@Serializable
data class Tool(
    val vendor: String,
    val name: String,
    val version: String
)

@Serializable
data class Component(
    val type: String,
    val `bom-ref`: String? = null,
    val publisher: String? = null,
    val group: String? = null,
    val name: String? = null,
    val version: String? = null,
    val description: String? = null,
    val hashes: List<Hash>? = null,
    val licenses: List<License>? = null,
    val purl: String? = null,
    val modified: Boolean? = null,
    val externalReferences: List<ExternalReference>? = null,
)

@Serializable
data class Hash(
    val alg: String,
    val content: String
)

@Serializable
data class License(
    val license: LicenseData
)


@Serializable
data class LicenseData(
    val id: String? = null,
    val name: String? = null,
    val text: LicenseText? = null,
    val url: String? = null
)

@Serializable
data class LicenseText(
    val contentType: String,
    val encoding: String,
    val content: String
)

@Serializable
data class ExternalReference(
    val type: String,
    val url: String,
)

@Serializable
data class Dependency(
    val ref: String,
    val dependsOn: List<String>,
)