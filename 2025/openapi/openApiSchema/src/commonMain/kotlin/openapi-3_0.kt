// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json     = Json { allowStructuredMapKeys = true }
// val topLevel = json.parse(TopLevel.serializer(), jsonString)

package quicktype.openapi_3_0

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable
data class TopLevel(
    val id: String,

    @SerialName("\$schema")
    val schema: String,

    val description: String,
    val type: String,
    val required: List<String>,
    val properties: TopLevelProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean,
    val definitions: Definitions
)

@Serializable
data class Definitions(
    @SerialName("Reference")
    val reference: Reference,

    @SerialName("Info")
    val info: Info,

    @SerialName("Contact")
    val contact: Contact,

    @SerialName("License")
    val license: License,

    @SerialName("Server")
    val server: Server,

    @SerialName("ServerVariable")
    val serverVariable: ServerVariable,

    @SerialName("Components")
    val components: ComponentsClass,

    @SerialName("Schema")
    val schema: Schema,

    @SerialName("Discriminator")
    val discriminator: Discriminator,

    @SerialName("XML")
    val xml: XML,

    @SerialName("Response")
    val response: Response,

    @SerialName("MediaType")
    val mediaType: MediaType,

    @SerialName("Example")
    val example: Example,

    @SerialName("Header")
    val header: Header,

    @SerialName("Paths")
    val paths: Paths,

    @SerialName("PathItem")
    val pathItem: PathItem,

    @SerialName("Operation")
    val operation: Operation,

    @SerialName("Responses")
    val responses: Responses,

    @SerialName("SecurityRequirement")
    val securityRequirement: SecurityRequirement,

    @SerialName("Tag")
    val tag: Tag,

    @SerialName("ExternalDocumentation")
    val externalDocumentation: ExternalDocumentation,

    @SerialName("ExampleXORExamples")
    val exampleXORExamples: ExampleXORExamples,

    @SerialName("SchemaXORContent")
    val schemaXORContent: SchemaXORContent,

    @SerialName("Parameter")
    val parameter: ParameterClass,

    @SerialName("PathParameter")
    val pathParameter: PathParameter,

    @SerialName("QueryParameter")
    val queryParameter: Parameter,

    @SerialName("HeaderParameter")
    val headerParameter: Parameter,

    @SerialName("CookieParameter")
    val cookieParameter: Parameter,

    @SerialName("RequestBody")
    val requestBody: RequestBody,

    @SerialName("SecurityScheme")
    val securityScheme: SecurityScheme,

    @SerialName("APIKeySecurityScheme")
    val apiKeySecurityScheme: APIKeySecurityScheme,

    @SerialName("HTTPSecurityScheme")
    val httpSecurityScheme: HTTPSecurityScheme,

    @SerialName("OAuth2SecurityScheme")
    val oAuth2SecurityScheme: OAuth2SecurityScheme,

    @SerialName("OpenIdConnectSecurityScheme")
    val openIDConnectSecurityScheme: OpenIDConnectSecurityScheme,

    @SerialName("OAuthFlows")
    val oAuthFlows: OAuthFlows,

    @SerialName("ImplicitOAuthFlow")
    val implicitOAuthFlow: Flow,

    @SerialName("PasswordOAuthFlow")
    val passwordOAuthFlow: Flow,

    @SerialName("ClientCredentialsFlow")
    val clientCredentialsFlow: Flow,

    @SerialName("AuthorizationCodeOAuthFlow")
    val authorizationCodeOAuthFlow: Flow,

    @SerialName("Link")
    val link: Link,

    @SerialName("Callback")
    val callback: Callback,

    @SerialName("Encoding")
    val encoding: Encoding
)

@Serializable
data class APIKeySecurityScheme(
    val type: String,
    val required: List<String>,
    val properties: APIKeySecuritySchemeProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class APIKeySecuritySchemePatternProperties(
    @SerialName("^x-")
    val x: X
)

@Serializable
class X()

@Serializable
data class APIKeySecuritySchemeProperties(
    val type: TypeClass,
    val name: Description,

    @SerialName("in")
    val propertiesIn: TypeClass,

    val description: Description
)

@Serializable
data class Description(
    val type: Type
)

@Serializable
enum class Type(val value: String) {
    @SerialName("number")
    Number("number"),

    @SerialName("boolean")
    TypeBoolean("boolean"),

    @SerialName("string")
    TypeString("string");
}

@Serializable
data class TypeClass(
    val type: Type,
    val enum: List<String>
)

@Serializable
data class Flow(
    val type: String,
    val required: List<String>,
    val properties: AuthorizationCodeOAuthFlowProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class AuthorizationCodeOAuthFlowProperties(
    @SerialName("authorizationUrl")
    val authorizationURL: AuthorizationURL? = null,

    @SerialName("tokenUrl")
    val tokenURL: AuthorizationURL? = null,

    @SerialName("refreshUrl")
    val refreshURL: AuthorizationURL,

    val scopes: Scopes
)

@Serializable
data class AuthorizationURL(
    val type: Type,
    val format: Format
)

@Serializable
enum class Format(val value: String) {
    @SerialName("email")
    Email("email"),

    @SerialName("regex")
    Regex("regex"),

    @SerialName("uri")
    URI("uri"),

    @SerialName("uri-reference")
    URIReference("uri-reference");
}

@Serializable
data class Scopes(
    val type: String,
    val additionalProperties: Description
)

@Serializable
data class Callback(
    val type: String,
    val additionalProperties: Components,
    val patternProperties: APIKeySecuritySchemePatternProperties
)

@Serializable
data class Components(
    @SerialName("\$ref")
    val ref: String
)

@Serializable
data class ComponentsClass(
    val type: String,
    val properties: ComponentsProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class ComponentsProperties(
    val schemas: Callbacks,
    val responses: Callbacks,
    val parameters: Callbacks,
    val examples: Callbacks,
    val requestBodies: Callbacks,
    val headers: Callbacks,
    val securitySchemes: Callbacks,
    val links: Callbacks,
    val callbacks: Callbacks
)

@Serializable
data class Callbacks(
    val type: String,
    val patternProperties: CallbacksPatternProperties
)

@Serializable
data class CallbacksPatternProperties(
    @SerialName("^[a-zA-Z0-9\\.\\-_]+\$")
    val aZAZ09_: SecurityScheme
)

@Serializable
data class SecurityScheme(
    val oneOf: List<Components>
)

@Serializable
data class Contact(
    val type: String,
    val properties: ContactProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class ContactProperties(
    val name: Description,
    val url: AuthorizationURL,
    val email: AuthorizationURL
)

@Serializable
data class Parameter(
    val description: String,
    val properties: CookieParameterProperties
)

@Serializable
data class CookieParameterProperties(
    @SerialName("in")
    val propertiesIn: PurpleIn,

    val style: PurpleStyle
)

@Serializable
data class PurpleIn(
    val enum: List<String>
)

@Serializable
data class PurpleStyle(
    val enum: List<String>,
    val default: String
)

@Serializable
data class Discriminator(
    val type: String,
    val required: List<String>,
    val properties: DiscriminatorProperties
)

@Serializable
data class DiscriminatorProperties(
    val propertyName: Description,
    val mapping: Scopes
)

@Serializable
data class Encoding(
    val type: String,
    val properties: EncodingProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class EncodingProperties(
    val contentType: Description,
    val headers: Headers,
    val style: TypeClass,
    val explode: Description,
    val allowReserved: AllowReserved
)

@Serializable
data class AllowReserved(
    val type: Type,
    val default: Boolean
)

@Serializable
data class Headers(
    val type: String,
    val additionalProperties: SecurityScheme
)

@Serializable
data class Example(
    val type: String,
    val properties: ExampleProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class ExampleProperties(
    val summary: Description,
    val description: Description,
    val value: X,
    val externalValue: AuthorizationURL
)

@Serializable
data class ExampleXORExamples(
    val description: String,
    val not: ExampleXORExamplesNot
)

@Serializable
data class ExampleXORExamplesNot(
    val required: List<String>
)

@Serializable
data class ExternalDocumentation(
    val type: String,
    val required: List<String>,
    val properties: ExternalDocumentationProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class ExternalDocumentationProperties(
    val description: Description,
    val url: AuthorizationURL
)

@Serializable
data class Header(
    val type: String,
    val properties: HeaderProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean,
    val allOf: List<Components>
)

@Serializable
data class HeaderProperties(
    val description: Description,
    val required: AllowReserved,
    val deprecated: AllowReserved,
    val allowEmptyValue: AllowReserved,
    val style: FluffyStyle,
    val explode: Description,
    val allowReserved: AllowReserved,
    val schema: SecurityScheme,
    val content: Content,
    val example: X,
    val examples: Headers
)

@Serializable
data class Content(
    val type: String,
    val additionalProperties: Components,
    val minProperties: Long,
    val maxProperties: Long
)

@Serializable
data class FluffyStyle(
    val type: Type,
    val enum: List<String>,
    val default: String
)

@Serializable
data class HTTPSecurityScheme(
    val type: String,
    val required: List<String>,
    val properties: HTTPSecuritySchemeProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean,
    val oneOf: List<HTTPSecuritySchemeOneOf>
)

@Serializable
data class HTTPSecuritySchemeOneOf(
    val description: String,
    val properties: OneOfProperties,
    val not: ExampleXORExamplesNot? = null
)

@Serializable
data class OneOfProperties(
    val scheme: Scheme
)

@Serializable
data class Scheme(
    val type: Type? = null,
    val pattern: String? = null,
    val not: Openapi? = null
)

@Serializable
data class Openapi(
    val type: Type,
    val pattern: String
)

@Serializable
data class HTTPSecuritySchemeProperties(
    val scheme: Description,
    val bearerFormat: Description,
    val description: Description,
    val type: TypeClass
)

@Serializable
data class Info(
    val type: String,
    val required: List<String>,
    val properties: InfoProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class InfoProperties(
    val title: String? = null,
    val description: String?=null,
    val termsOfService: AuthorizationURL?=null,
    val contact: Components?=null,
    val license: Components?=null,
    val version: String?=null,
)

@Serializable
data class License(
    val type: String,
    val required: List<String>,
    val properties: LicenseProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class LicenseProperties(
    val name: Description,
    val url: AuthorizationURL
)

@Serializable
data class Link(
    val type: String,
    val properties: LinkProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean,
    val not: LinkNot
)

@Serializable
data class LinkNot(
    val description: String,
    val required: List<String>
)

@Serializable
data class LinkProperties(
    @SerialName("operationId")
    val operationID: Description,

    val operationRef: AuthorizationURL,
    val parameters: PurpleParameters,
    val requestBody: X,
    val description: Description,
    val server: Components
)

@Serializable
data class PurpleParameters(
    val type: String,
    val additionalProperties: X
)

@Serializable
data class MediaType(
    val type: String,
    val properties: MediaTypeProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean,
    val allOf: List<Components>
)

@Serializable
data class MediaTypeProperties(
    val schema: SecurityScheme,
    val example: X,
    val examples: Headers,
    val encoding: EncodingClass
)

@Serializable
data class EncodingClass(
    val type: String,
    val additionalProperties: Components
)

@Serializable
data class OAuth2SecurityScheme(
    val type: String,
    val required: List<String>,
    val properties: OAuth2SecuritySchemeProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class OAuth2SecuritySchemeProperties(
    val type: TypeClass,
    val flows: Components,
    val description: Description
)

@Serializable
data class OAuthFlows(
    val type: String,
    val properties: OAuthFlowsProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class OAuthFlowsProperties(
    val implicit: Components,
    val password: Components,
    val clientCredentials: Components,
    val authorizationCode: Components
)

@Serializable
data class OpenIDConnectSecurityScheme(
    val type: String,
    val required: List<String>,
    val properties: OpenIDConnectSecuritySchemeProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class OpenIDConnectSecuritySchemeProperties(
    val type: TypeClass,

    @SerialName("openIdConnectUrl")
    val openIDConnectURL: AuthorizationURL,

    val description: Description
)

@Serializable
data class Operation(
    val type: String,
    val required: List<String>,
    val properties: OperationProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class OperationProperties(
    val tags: AdditionalProperties,
    val summary: Description,
    val description: Description,
    val externalDocs: Components,

    @SerialName("operationId")
    val operationID: Description,

    val parameters: FluffyParameters,
    val requestBody: SecurityScheme,
    val responses: Components,
    val callbacks: Headers,
    val deprecated: AllowReserved,
    val security: Security,
    val servers: Security
)

@Serializable
data class FluffyParameters(
    val type: String,
    val items: SecurityScheme,
    val uniqueItems: Boolean
)

@Serializable
data class Security(
    val type: String,
    val items: Components
)

@Serializable
data class AdditionalProperties(
    val type: String,
    val items: Description
)

@Serializable
data class ParameterClass(
    val type: String,
    val properties: ParameterProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean,
    val required: List<String>,
    val allOf: List<Components>,
    val oneOf: List<Components>
)

@Serializable
data class ParameterProperties(
    val name: Description,

    @SerialName("in")
    val propertiesIn: Description,

    val description: Description,
    val required: AllowReserved,
    val deprecated: AllowReserved,
    val allowEmptyValue: AllowReserved,
    val style: Description,
    val explode: Description,
    val allowReserved: AllowReserved,
    val schema: SecurityScheme,
    val content: Content,
    val example: X,
    val examples: Headers
)

@Serializable
data class PathItem(
    val type: String,
    val properties: PathItemProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class PathItemProperties(
    @SerialName("\$ref")
    val ref: Description,

    val summary: Description,
    val description: Description,
    val get: Components,
    val put: Components,
    val post: Components,
    val delete: Components,
    val options: Components,
    val head: Components,
    val patch: Components,
    val trace: Components,
    val servers: Security,
    val parameters: FluffyParameters
)

@Serializable
data class PathParameter(
    val description: String,
    val required: List<String>,
    val properties: PathParameterProperties
)

@Serializable
data class PathParameterProperties(
    @SerialName("in")
    val propertiesIn: PurpleIn,

    val style: PurpleStyle,
    val required: PurpleRequired
)

@Serializable
data class PurpleRequired(
    val enum: List<Boolean>
)

@Serializable
data class Paths(
    val type: String,
    val patternProperties: PathsPatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class PathsPatternProperties(
    @SerialName("^\\/")
    val empty: Components,

    @SerialName("^x-")
    val x: X
)

@Serializable
data class Reference(
    val type: String,
    val required: List<String>,
    val patternProperties: ReferencePatternProperties
)

@Serializable
data class ReferencePatternProperties(
    @SerialName("^\\\$ref\$")
    val ref: AuthorizationURL
)

@Serializable
data class RequestBody(
    val type: String,
    val required: List<String>,
    val properties: RequestBodyProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class RequestBodyProperties(
    val description: Description,
    val content: EncodingClass,
    val required: AllowReserved
)

@Serializable
data class Response(
    val type: String,
    val required: List<String>,
    val properties: ResponseProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class ResponseProperties(
    val description: Description,
    val headers: Headers,
    val content: EncodingClass,
    val links: Headers
)

@Serializable
data class Responses(
    val type: String,
    val properties: ResponsesProperties,
    val patternProperties: ResponsesPatternProperties,
    val minProperties: Long,
    val additionalProperties: Boolean
)

@Serializable
data class ResponsesPatternProperties(
    @SerialName("^[1-5](?:\\d{2}|XX)\$")
    val the15D2XX: SecurityScheme,

    @SerialName("^x-")
    val x: X
)

@Serializable
data class ResponsesProperties(
    val default: SecurityScheme
)

@Serializable
data class Schema(
    val type: String,
    val properties: SchemaProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class SchemaProperties(
    val title: Description,
    val multipleOf: MultipleOf,
    val maximum: Description,
    val exclusiveMaximum: AllowReserved,
    val minimum: Description,
    val exclusiveMinimum: AllowReserved,
    val maxLength: Max,
    val minLength: Min,
    val pattern: AuthorizationURL,
    val maxItems: Max,
    val minItems: Min,
    val uniqueItems: AllowReserved,
    val maxProperties: Max,
    val minProperties: Min,
    val required: FluffyRequired,
    val enum: EnumClass,
    val type: TypeClass,
    val not: SecurityScheme,
    val allOf: Of,
    val oneOf: Of,
    val anyOf: Of,
    val items: SecurityScheme,
    val properties: Headers,
    val additionalProperties: PurpleAdditionalProperties,
    val description: Description,
    val format: Description,
    val default: X,
    val nullable: AllowReserved,
    val discriminator: Components,
    val readOnly: AllowReserved,
    val writeOnly: AllowReserved,
    val example: X,
    val externalDocs: Components,
    val deprecated: AllowReserved,
    val xml: Components
)

@Serializable
data class PurpleAdditionalProperties(
    val oneOf: List<AdditionalPropertiesOneOf>,
    val default: Boolean
)

@Serializable
data class AdditionalPropertiesOneOf(
    @SerialName("\$ref")
    val ref: String? = null,

    val type: Type? = null
)

@Serializable
data class Of(
    val type: String,
    val items: SecurityScheme
)

@Serializable
data class EnumClass(
    val type: String,
    val items: X,
    val minItems: Long,
    val uniqueItems: Boolean
)

@Serializable
data class Max(
    val type: String,
    val minimum: Long
)

@Serializable
data class Min(
    val type: String,
    val minimum: Long,
    val default: Long
)

@Serializable
data class MultipleOf(
    val type: Type,
    val minimum: Long,
    val exclusiveMinimum: Boolean
)

@Serializable
data class FluffyRequired(
    val type: String,
    val items: Description,
    val minItems: Long,
    val uniqueItems: Boolean
)

@Serializable
data class SchemaXORContent(
    val description: String,
    val not: ExampleXORExamplesNot,
    val oneOf: List<SchemaXORContentOneOf>
)

@Serializable
data class SchemaXORContentOneOf(
    val required: List<String>,
    val description: String? = null,
    val allOf: List<AllOf>? = null
)

@Serializable
data class AllOf(
    val not: ExampleXORExamplesNot
)

@Serializable
data class SecurityRequirement(
    val type: String,
    val additionalProperties: AdditionalProperties
)

@Serializable
data class Server(
    val type: String,
    val required: List<String>,
    val properties: ServerProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class ServerProperties(
    val url: Description,
    val description: Description,
    val variables: EncodingClass
)

@Serializable
data class ServerVariable(
    val type: String,
    val required: List<String>,
    val properties: ServerVariableProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class ServerVariableProperties(
    val enum: AdditionalProperties,
    val default: Description,
    val description: Description
)

@Serializable
data class Tag(
    val type: String,
    val required: List<String>,
    val properties: TagProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class TagProperties(
    val name: Description,
    val description: Description,
    val externalDocs: Components
)

@Serializable
data class XML(
    val type: String,
    val properties: XMLProperties,
    val patternProperties: APIKeySecuritySchemePatternProperties,
    val additionalProperties: Boolean
)

@Serializable
data class XMLProperties(
    val name: Description,
    val namespace: AuthorizationURL,
    val prefix: Description,
    val attribute: AllowReserved,
    val wrapped: AllowReserved
)

@Serializable
data class TopLevelProperties(
    val openapi: String,
    val info: InfoProperties,
    val description: String,
    val externalDocs: Components,
    val servers: Security,
    val security: Security,
    val tags: Tags,
    val paths: Components,
    val components: Components
)

@Serializable
data class Tags(
    val type: String,
    val items: Components,
    val uniqueItems: Boolean
)
