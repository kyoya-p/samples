package gen_3

import com.charleskorn.kaml.YamlNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenApi(
    val openapi: String? = null,
    val info: YamlNode? = null,
    val servers: List<Server>? = null,
    val paths: Map<String, PathItem>? = null,
    val components: YamlNode? = null
)

@Serializable
data class Info(
    val title: String? = null,
    val version: String? = null,
    val description: String? = null
)

@Serializable
data class Server(
    val url: String,
    val description: String? = null
)

@Serializable
data class PathItem(
    val get: Operation? = null,
    val put: Operation? = null,
    val post: Operation? = null,
    val delete: Operation? = null,
    val patch: Operation? = null
)

@Serializable
data class Operation(
    val description: String? = null,
    val summary: String? = null,
    val parameters: List<YamlNode>? = null,
    val responses: Map<String, YamlNode>? = null,
    val requestBody: RequestBody? = null,
    val deprecated: Boolean? = null,
    val security: List<YamlNode?>? = null,
    val servers: List<Server?>? = null
)

@Serializable
data class Parameter(
    val name: String? = null,
    @SerialName("in") val location: ParameterLocation? = null, // enumを使用
    val description: String? = null,
    val required: Boolean? = null,
    val schema: Schema? = null
)

@Serializable
data class RequestBody(
    val content: Map<String, MediaType>? = null
)

@Serializable
data class Response(
    val description: String? = null,
    val headers: Map<String, Header?>? = null,
    val content: Map<String, MediaType?>? = null,
    val links: Map<String, Link?>? = null
)

@Serializable
data class Header(
    val schema: Schema? = null,
    val description: String? = null,
    val required: Boolean? = null
)

@Serializable
data class Link(
    val operationId: String? = null
)

@Serializable
data class MediaType(
    val schema: Schema? = null,
    val example: String? = null,
    val examples: Map<String, Example?>? = null,
    val encoding: Map<String, Encoding?>? = null
)

@Serializable
data class Example(
    val value: String? = null
)

@Serializable
data class Encoding(
    val contentType: String? = null
)

@Serializable
data class Schema(
    val type: SchemaType? = null, // enumを使用
    val items: Schema? = null,
    val properties: Map<String, Schema?>? = null,
    val description: String? = null,
    val enum: List<String?>? = null,
    val format: String? = null,
    val required: List<String?>? = null,
    val default: String? = null,
    val additionalProperties: Schema? = null,
    val allOf: List<Schema?>? = null,
    val oneOf: List<Schema?>? = null,
    val anyOf: List<Schema?>? = null,
    val not: Schema? = null,
    val readOnly: Boolean? = null,
    val writeOnly: Boolean? = null,
    val nullable: Boolean? = null,
    val deprecated: Boolean? = null,
    @SerialName("${'$'}ref") val ref: String? = null
)


@Serializable
data class SecurityRequirement(
    val scope: List<String?>? = null
)

@Serializable
data class Components(
    val schemas: Map<String, Schema?>? = null
)

// enum定義
@Serializable
enum class ParameterLocation {
    query, header, path, cookie
}

@Serializable
enum class SchemaType {
    @SerialName("string")
    STRING,

    @SerialName("number")
    NUMBER,

    @SerialName("integer")
    INTEGER,

    @SerialName("boolean")
    BOOLEAN,

    @SerialName("array")
    ARRAY,

    @SerialName("object")
    OBJECT
}