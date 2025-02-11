// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json     = Json { allowStructuredMapKeys = true }
// val topLevel = json.parse(TopLevel.serializer(), jsonString)

package quicktype.openapi_3_1


import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable
data class TopLevel (
    @SerialName("\$id")
    val id: String,

    @SerialName("\$schema")
    val schema: String,

    val description: String,
    val type: CallbacksType,
    val properties: TopLevelProperties,
    val required: List<String>,
    val anyOf: List<AnyOf>,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean,

    @SerialName("\$defs")
    val defs: TopLevelDefs
)

@Serializable
data class AnyOf (
    val required: List<String>
)

@Serializable
data class TopLevelDefs (
    val info: Info,
    val contact: Contact,
    val license: License,
    val server: Server,

    @SerialName("server-variable")
    val serverVariable: ServerVariable,

    val components: DefsComponents,
    val paths: Contact,

    @SerialName("path-item")
    val pathItem: PathItem,

    val operation: Operation,

    @SerialName("external-documentation")
    val externalDocumentation: ExternalDocumentation,

    val parameter: Parameter,

    @SerialName("parameter-or-reference")
    val parameterOrReference: OrReference,

    @SerialName("request-body")
    val requestBody: RequestBody,

    @SerialName("request-body-or-reference")
    val requestBodyOrReference: OrReference,

    val content: Callbacks,

    @SerialName("media-type")
    val mediaType: MediaType,

    val encoding: Encoding,
    val responses: Responses,
    val response: Response,

    @SerialName("response-or-reference")
    val responseOrReference: OrReference,

    val callbacks: Callbacks,

    @SerialName("callbacks-or-reference")
    val callbacksOrReference: OrReference,

    val example: Example,

    @SerialName("example-or-reference")
    val exampleOrReference: OrReference,

    val link: Link,

    @SerialName("link-or-reference")
    val linkOrReference: OrReference,

    val header: Header,

    @SerialName("header-or-reference")
    val headerOrReference: OrReference,

    val tag: Tag,
    val reference: Reference,
    val schema: DefsSchema,

    @SerialName("security-scheme")
    val securityScheme: SecurityScheme,

    @SerialName("security-scheme-or-reference")
    val securitySchemeOrReference: OrReference,

    @SerialName("oauth-flows")
    val oauthFlows: OauthFlows,

    @SerialName("security-requirement")
    val securityRequirement: SecurityRequirement,

    @SerialName("specification-extensions")
    val specificationExtensions: SpecificationExtensions,

    val examples: Examples,

    @SerialName("map-of-strings")
    val mapOfStrings: MapOfStrings,

    @SerialName("styles-for-form")
    val stylesForForm: StylesForForm
)

@Serializable
data class Callbacks (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,

    @SerialName("\$ref")
    val ref: String? = null,

    val additionalProperties: ComponentsElement,
    val propertyNames: CallbacksPropertyNames? = null
)

@Serializable
data class ComponentsElement (
    @SerialName("\$ref")
    val ref: String
)

@Serializable
data class CallbacksPropertyNames (
    val format: Format
)

@Serializable
enum class Format(val value: String) {
    @SerialName("email") Email("email"),
    @SerialName("media-range") MediaRange("media-range"),
    @SerialName("uri") URI("uri"),
    @SerialName("uri-reference") URIReference("uri-reference");
}

@Serializable
enum class CallbacksType(val value: String) {
    @SerialName("object") Object("object");
}

@Serializable
data class OrReference (
    @SerialName("if")
    val orReferenceIf: CallbacksOrReferenceIf,

    val then: ComponentsElement,

    @SerialName("else")
    val orReferenceElse: ComponentsElement
)

@Serializable
data class CallbacksOrReferenceIf (
    val type: CallbacksType,
    val required: List<String>
)

@Serializable
data class DefsComponents (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: ComponentsProperties,
    val patternProperties: ComponentsPatternProperties,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class ComponentsPatternProperties (
    @SerialName("^(schemas|responses|parameters|examples|requestBodies|headers|securitySchemes|links|callbacks|pathItems)\$")
    val schemasResponsesParametersExamplesRequestBodiesHeadersSecuritySchemesLinksCallbacksPathItems: SchemasResponsesParametersExamplesRequestBodiesHeadersSecuritySchemesLinksCallbacksPathItems
)

@Serializable
data class SchemasResponsesParametersExamplesRequestBodiesHeadersSecuritySchemesLinksCallbacksPathItems (
    @SerialName("\$comment")
    val comment: String,

    val propertyNames: SchemasResponsesParametersExamplesRequestBodiesHeadersSecuritySchemesLinksCallbacksPathItemsPropertyNames
)

@Serializable
data class SchemasResponsesParametersExamplesRequestBodiesHeadersSecuritySchemesLinksCallbacksPathItemsPropertyNames (
    val pattern: String
)

@Serializable
data class ComponentsProperties (
    val schemas: Schemas,
    val responses: Webhooks,
    val parameters: Webhooks,
    val examples: Webhooks,
    val requestBodies: Webhooks,
    val headers: Webhooks,
    val securitySchemes: Webhooks,
    val links: Webhooks,
    val callbacks: Webhooks,
    val pathItems: Webhooks
)

@Serializable
data class Webhooks (
    val type: CallbacksType,
    val additionalProperties: ComponentsElement
)

@Serializable
data class Schemas (
    val type: CallbacksType,
    val additionalProperties: AdditionalPropertiesClass
)

@Serializable
data class AdditionalPropertiesClass (
    @SerialName("\$dynamicRef")
    val dynamicRef: String
)

@Serializable
data class Contact (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: ContactProperties? = null,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean,
    val patternProperties: ContactPatternProperties? = null
)

@Serializable
data class ContactPatternProperties (
    @SerialName("^/")
    val empty: ComponentsElement
)

@Serializable
data class ContactProperties (
    val name: NameClass,
    val url: Email,
    val email: Email
)

@Serializable
data class Email (
    val type: EmailType,
    val format: Format
)

@Serializable
enum class EmailType(val value: String) {
    @SerialName("boolean") TypeBoolean("boolean"),
    @SerialName("string") TypeString("string");
}

@Serializable
data class NameClass (
    val type: EmailType
)

@Serializable
data class Encoding (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: EncodingProperties,
    val allOf: List<ComponentsElement>,
    val unevaluatedProperties: Boolean
)

@Serializable
data class EncodingProperties (
    val contentType: Email,
    val headers: Webhooks,
    val style: PurpleStyle,
    val explode: NameClass,
    val allowReserved: AllowReserved
)

@Serializable
data class AllowReserved (
    val default: Boolean,
    val type: EmailType
)

@Serializable
data class PurpleStyle (
    val default: String,
    val enum: List<String>
)

@Serializable
data class Example (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: ExampleProperties,
    val not: AnyOf,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class ExampleProperties (
    val summary: NameClass,
    val description: NameClass,
    val value: Boolean,
    val externalValue: Email
)

@Serializable
data class Examples (
    val properties: ExamplesProperties
)

@Serializable
data class ExamplesProperties (
    val example: Boolean,
    val examples: Webhooks
)

@Serializable
data class ExternalDocumentation (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: ExternalDocumentationProperties,
    val required: List<String>,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class ExternalDocumentationProperties (
    val description: NameClass,
    val url: Email
)

@Serializable
data class Header (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: HeaderProperties,
    val oneOf: List<AnyOf>,
    val dependentSchemas: HeaderDependentSchemas,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class HeaderDependentSchemas (
    val schema: PurpleSchema
)

@Serializable
data class PurpleSchema (
    val properties: PurpleProperties,

    @SerialName("\$ref")
    val ref: String
)

@Serializable
data class PurpleProperties (
    val style: FluffyStyle,
    val explode: AllowReserved
)

@Serializable
data class FluffyStyle (
    val default: String,
    val const: String
)

@Serializable
data class HeaderProperties (
    val description: NameClass,
    val required: AllowReserved,
    val deprecated: AllowReserved,
    val schema: AdditionalPropertiesClass,
    val content: Content
)

@Serializable
data class Content (
    @SerialName("\$ref")
    val ref: String,

    val minProperties: Long,
    val maxProperties: Long
)

@Serializable
data class Info (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: InfoProperties,
    val required: List<String>,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class InfoProperties (
    val title: NameClass,
    val summary: NameClass,
    val description: NameClass,
    val termsOfService: Email,
    val contact: ComponentsElement,
    val license: ComponentsElement,
    val version: NameClass
)

@Serializable
data class License (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: LicenseProperties,
    val required: List<String>,
    val dependentSchemas: LicenseDependentSchemas,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class LicenseDependentSchemas (
    val identifier: Identifier
)

@Serializable
data class Identifier (
    val not: AnyOf
)

@Serializable
data class LicenseProperties (
    val name: NameClass,
    val identifier: NameClass,
    val url: Email
)

@Serializable
data class Link (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: LinkProperties,
    val oneOf: List<AnyOf>,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class LinkProperties (
    val operationRef: Email,

    @SerialName("operationId")
    val operationID: NameClass,

    val parameters: ComponentsElement,
    val requestBody: Boolean,
    val description: NameClass,
    val body: ComponentsElement
)

@Serializable
data class MapOfStrings (
    val type: CallbacksType,
    val additionalProperties: NameClass
)

@Serializable
data class MediaType (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: MediaTypeProperties,
    val allOf: List<ComponentsElement>,
    val unevaluatedProperties: Boolean
)

@Serializable
data class MediaTypeProperties (
    val schema: AdditionalPropertiesClass,
    val encoding: Webhooks
)

@Serializable
data class OauthFlows (
    val type: CallbacksType,
    val properties: OauthFlowsProperties,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean,

    @SerialName("\$defs")
    val defs: OauthFlowsDefs
)

@Serializable
data class OauthFlowsDefs (
    val implicit: AuthorizationCode,
    val password: AuthorizationCode,

    @SerialName("client-credentials")
    val clientCredentials: AuthorizationCode,

    @SerialName("authorization-code")
    val authorizationCode: AuthorizationCode
)

@Serializable
data class AuthorizationCode (
    val type: CallbacksType,
    val properties: AuthorizationCodeProperties,
    val required: List<String>,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class AuthorizationCodeProperties (
    @SerialName("authorizationUrl")
    val authorizationURL: Email? = null,

    @SerialName("tokenUrl")
    val tokenURL: Email? = null,

    @SerialName("refreshUrl")
    val refreshURL: Email,

    val scopes: ComponentsElement
)

@Serializable
data class OauthFlowsProperties (
    val implicit: ComponentsElement,
    val password: ComponentsElement,
    val clientCredentials: ComponentsElement,
    val authorizationCode: ComponentsElement
)

@Serializable
data class Operation (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: OperationProperties,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class OperationProperties (
    val tags: TagsClass,
    val summary: NameClass,
    val description: NameClass,
    val externalDocs: ComponentsElement,

    @SerialName("operationId")
    val operationID: NameClass,

    val parameters: Security,
    val requestBody: ComponentsElement,
    val responses: ComponentsElement,
    val callbacks: Webhooks,
    val deprecated: AllowReserved,
    val security: Security,
    val servers: Security
)

@Serializable
data class Security (
    val type: String,
    val items: ComponentsElement
)

@Serializable
data class TagsClass (
    val type: String,
    val items: NameClass
)

@Serializable
data class Parameter (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: ParameterProperties,
    val required: List<String>,
    val oneOf: List<AnyOf>,

    @SerialName("if")
    val parameterIf: StylesForCookieIf,

    val then: ParameterThen,
    val dependentSchemas: ParameterDependentSchemas,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class ParameterDependentSchemas (
    val schema: FluffySchema
)

@Serializable
data class FluffySchema (
    val properties: IndecentProperties,
    val allOf: List<ComponentsElement>,

    @SerialName("\$defs")
    val defs: SchemaDefs
)

@Serializable
data class SchemaDefs (
    @SerialName("styles-for-path")
    val stylesForPath: StylesForPath,

    @SerialName("styles-for-header")
    val stylesForHeader: StylesFor,

    @SerialName("styles-for-query")
    val stylesForQuery: StylesForQuery,

    @SerialName("styles-for-cookie")
    val stylesForCookie: StylesFor
)

@Serializable
data class StylesFor (
    @SerialName("if")
    val stylesForIf: StylesForCookieIf,

    val then: StylesForCookieThen
)

@Serializable
data class StylesForCookieIf (
    val properties: FluffyProperties,
    val required: List<String>
)

@Serializable
data class FluffyProperties (
    @SerialName("in")
    val propertiesIn: StyleClass
)

@Serializable
data class StyleClass (
    val const: String
)

@Serializable
data class StylesForCookieThen (
    val properties: TentacledProperties
)

@Serializable
data class TentacledProperties (
    val style: FluffyStyle
)

@Serializable
data class StylesForPath (
    @SerialName("if")
    val stylesForPathIf: StylesForCookieIf,

    val then: StylesForPathThen
)

@Serializable
data class StylesForPathThen (
    val properties: StickyProperties,
    val required: List<String>
)

@Serializable
data class StickyProperties (
    val style: PurpleStyle,
    val required: Required
)

@Serializable
data class Required (
    val const: Boolean
)

@Serializable
data class StylesForQuery (
    @SerialName("if")
    val stylesForQueryIf: StylesForCookieIf,

    val then: StylesForQueryThen
)

@Serializable
data class StylesForQueryThen (
    val properties: IndigoProperties
)

@Serializable
data class IndigoProperties (
    val style: PurpleStyle,
    val allowReserved: AllowReserved
)

@Serializable
data class IndecentProperties (
    val style: NameClass,
    val explode: NameClass
)

@Serializable
data class ParameterProperties (
    val name: NameClass,

    @SerialName("in")
    val propertiesIn: PurpleIn,

    val description: NameClass,
    val required: AllowReserved,
    val deprecated: AllowReserved,
    val schema: AdditionalPropertiesClass,
    val content: Content
)

@Serializable
data class PurpleIn (
    val enum: List<String>
)

@Serializable
data class ParameterThen (
    val properties: HilariousProperties
)

@Serializable
data class HilariousProperties (
    val allowEmptyValue: AllowReserved
)

@Serializable
data class PathItem (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: PathItemProperties,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class PathItemProperties (
    @SerialName("\$ref")
    val ref: Email,

    val summary: NameClass,
    val description: NameClass,
    val servers: Security,
    val parameters: Security,
    val get: ComponentsElement,
    val put: ComponentsElement,
    val post: ComponentsElement,
    val delete: ComponentsElement,
    val options: ComponentsElement,
    val head: ComponentsElement,
    val patch: ComponentsElement,
    val trace: ComponentsElement
)

@Serializable
data class Reference (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: ReferenceProperties
)

@Serializable
data class ReferenceProperties (
    @SerialName("\$ref")
    val ref: Email,

    val summary: NameClass,
    val description: NameClass
)

@Serializable
data class RequestBody (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: RequestBodyProperties,
    val required: List<String>,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class RequestBodyProperties (
    val description: NameClass,
    val content: ComponentsElement,
    val required: AllowReserved
)

@Serializable
data class Response (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: ResponseProperties,
    val required: List<String>,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class ResponseProperties (
    val description: NameClass,
    val headers: Webhooks,
    val content: ComponentsElement,
    val links: Webhooks
)

@Serializable
data class Responses (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: ResponsesProperties,
    val patternProperties: ResponsesPatternProperties,
    val minProperties: Long,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean,

    @SerialName("if")
    val responsesIf: ResponsesIf,

    val then: AnyOf
)

@Serializable
data class ResponsesPatternProperties (
    @SerialName("^[1-5](?:[0-9]{2}|XX)\$")
    val the15092Xx: ComponentsElement
)

@Serializable
data class ResponsesProperties (
    val default: ComponentsElement
)

@Serializable
data class ResponsesIf (
    @SerialName("\$comment")
    val comment: String,

    val patternProperties: IfPatternProperties
)

@Serializable
data class IfPatternProperties (
    @SerialName("^[1-5](?:[0-9]{2}|XX)\$")
    val the15092Xx: Boolean
)

@Serializable
data class DefsSchema (
    @SerialName("\$comment")
    val comment: String,

    @SerialName("\$dynamicAnchor")
    val dynamicAnchor: String,

    val type: List<String>
)

@Serializable
data class SecurityRequirement (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val additionalProperties: TagsClass
)

@Serializable
data class SecurityScheme (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: SecuritySchemeProperties,
    val required: List<String>,
    val allOf: List<ComponentsElement>,
    val unevaluatedProperties: Boolean,

    @SerialName("\$defs")
    val defs: SecuritySchemeDefs
)

@Serializable
data class SecuritySchemeDefs (
    @SerialName("type-apikey")
    val typeApikey: TypeApikey,

    @SerialName("type-http")
    val typeHTTP: TypeHTTP,

    @SerialName("type-http-bearer")
    val typeHTTPBearer: TypeHTTPBearer,

    @SerialName("type-oauth2")
    val typeOauth2: TypeOauth2,

    @SerialName("type-oidc")
    val typeOidc: TypeOidc
)

@Serializable
data class TypeApikey (
    @SerialName("if")
    val typeApikeyIf: TypeApikeyIf,

    val then: TypeApikeyThen
)

@Serializable
data class TypeApikeyThen (
    val properties: CunningProperties,
    val required: List<String>
)

@Serializable
data class CunningProperties (
    val name: NameClass,

    @SerialName("in")
    val propertiesIn: PurpleIn
)

@Serializable
data class TypeApikeyIf (
    val properties: AmbitiousProperties,
    val required: List<String>
)

@Serializable
data class AmbitiousProperties (
    val type: StyleClass
)

@Serializable
data class TypeHTTP (
    @SerialName("if")
    val typeHTTPIf: TypeApikeyIf,

    val then: TypeHTTPThen
)

@Serializable
data class TypeHTTPThen (
    val properties: MagentaProperties,
    val required: List<String>
)

@Serializable
data class MagentaProperties (
    val scheme: NameClass
)

@Serializable
data class TypeHTTPBearer (
    @SerialName("if")
    val typeHTTPBearerIf: TypeHTTPBearerIf,

    val then: TypeHTTPBearerThen
)

@Serializable
data class TypeHTTPBearerThen (
    val properties: MischievousProperties
)

@Serializable
data class MischievousProperties (
    val bearerFormat: NameClass
)

@Serializable
data class TypeHTTPBearerIf (
    val properties: FriskyProperties,
    val required: List<String>
)

@Serializable
data class FriskyProperties (
    val type: StyleClass,
    val scheme: Openapi
)

@Serializable
data class Openapi (
    val type: EmailType,
    val pattern: String
)

@Serializable
data class TypeOauth2 (
    @SerialName("if")
    val typeOauth2If: TypeApikeyIf,

    val then: TypeOauth2Then
)

@Serializable
data class TypeOauth2Then (
    val properties: BraggadociousProperties,
    val required: List<String>
)

@Serializable
data class BraggadociousProperties (
    val flows: ComponentsElement
)

@Serializable
data class TypeOidc (
    @SerialName("if")
    val typeOidcIf: TypeApikeyIf,

    val then: TypeOidcThen
)

@Serializable
data class TypeOidcThen (
    val properties: Properties1,
    val required: List<String>
)

@Serializable
data class Properties1 (
    @SerialName("openIdConnectUrl")
    val openIDConnectURL: Email
)

@Serializable
data class SecuritySchemeProperties (
    val type: PurpleIn,
    val description: NameClass
)

@Serializable
data class Server (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: ServerProperties,
    val required: List<String>,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class ServerProperties (
    val url: NameClass,
    val description: NameClass,
    val variables: Webhooks
)

@Serializable
data class ServerVariable (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: ServerVariableProperties,
    val required: List<String>,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class ServerVariableProperties (
    val enum: EnumClass,
    val default: NameClass,
    val description: NameClass
)

@Serializable
data class EnumClass (
    val type: String,
    val items: NameClass,
    val minItems: Long
)

@Serializable
data class SpecificationExtensions (
    @SerialName("\$comment")
    val comment: String,

    val patternProperties: SpecificationExtensionsPatternProperties
)

@Serializable
data class SpecificationExtensionsPatternProperties (
    @SerialName("^x-")
    val x: Boolean
)

@Serializable
data class StylesForForm (
    @SerialName("if")
    val stylesForFormIf: StylesForFormIf,

    val then: Else,

    @SerialName("else")
    val stylesForFormElse: Else
)

@Serializable
data class Else (
    val properties: ElseProperties
)

@Serializable
data class ElseProperties (
    val explode: Explode
)

@Serializable
data class Explode (
    val default: Boolean
)

@Serializable
data class StylesForFormIf (
    val properties: Properties2,
    val required: List<String>
)

@Serializable
data class Properties2 (
    val style: StyleClass
)

@Serializable
data class Tag (
    @SerialName("\$comment")
    val comment: String,

    val type: CallbacksType,
    val properties: TagProperties,
    val required: List<String>,

    @SerialName("\$ref")
    val ref: String,

    val unevaluatedProperties: Boolean
)

@Serializable
data class TagProperties (
    val name: NameClass,
    val description: NameClass,
    val externalDocs: ComponentsElement
)

@Serializable
data class TopLevelProperties (
    val openapi: Openapi,
    val info: ComponentsElement,
    val jsonSchemaDialect: JSONSchemaDialect,
    val servers: Servers,
    val paths: ComponentsElement,
    val webhooks: Webhooks,
    val components: ComponentsElement,
    val security: Security,
    val tags: Security,
    val externalDocs: ComponentsElement
)

@Serializable
data class JSONSchemaDialect (
    val type: EmailType,
    val format: Format,
    val default: String
)

@Serializable
data class Servers (
    val type: String,
    val items: ComponentsElement,
    val default: List<Default>
)

@Serializable
data class Default (
    val url: String
)
