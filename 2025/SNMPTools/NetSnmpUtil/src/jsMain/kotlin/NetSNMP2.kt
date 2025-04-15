@file:JsModule("net-snmp")
@file:JsNonModule

package node.net_snmp

//import node.buffer.Buffer // Requires "@types/node"
//import node.events.EventEmitter // Requires "@types/node"
//import node.dgram.Socket as DgramSocket
//import node.net.Socket as NetSocket
//import node.dgram.RemoteInfo
//import kotlin.js.JsError

external class Buffer by List<UByte>()

// --- Constants and Enums ---

/** SNMP Version 1 */
external val Version1: Int

/** SNMP Version 2c */
external val Version2c: Int

/** SNMP Version 3 */
external val Version3: Int

/** SNMP Version constants */
external object Version {
    @JsName("1")
    val v1: Int
    @JsName("2c")
    val v2c: Int
    @JsName("3")
    val v3: Int
}

/** SNMP Error Status codes and names */
external object ErrorStatus {
    val NoError: Int
    val TooBig: Int
    val NoSuchName: Int
    val BadValue: Int
    val ReadOnly: Int
    val GeneralError: Int
    val NoAccess: Int
    val WrongType: Int
    val WrongLength: Int
    val WrongEncoding: Int
    val WrongValue: Int
    val NoCreation: Int
    val InconsistentValue: Int
    val ResourceUnavailable: Int
    val CommitFailed: Int
    val UndoFailed: Int
    val AuthorizationError: Int
    val NotWritable: Int
    val InconsistentName: Int

    // Reverse mapping (code to name)
    @JsName("0")
    val NoError_STRING: String
    @JsName("1")
    val TooBig_STRING: String
    @JsName("2")
    val NoSuchName_STRING: String
    @JsName("3")
    val BadValue_STRING: String
    @JsName("4")
    val ReadOnly_STRING: String
    @JsName("5")
    val GeneralError_STRING: String
    @JsName("6")
    val NoAccess_STRING: String
    @JsName("7")
    val WrongType_STRING: String
    @JsName("8")
    val WrongLength_STRING: String
    @JsName("9")
    val WrongEncoding_STRING: String
    @JsName("10")
    val WrongValue_STRING: String
    @JsName("11")
    val NoCreation_STRING: String
    @JsName("12")
    val InconsistentValue_STRING: String
    @JsName("13")
    val ResourceUnavailable_STRING: String
    @JsName("14")
    val CommitFailed_STRING: String
    @JsName("15")
    val UndoFailed_STRING: String
    @JsName("16")
    val AuthorizationError_STRING: String
    @JsName("17")
    val NotWritable_STRING: String
    @JsName("18")
    val InconsistentName_STRING: String
}

/** SNMP Object Type codes and names */
external object ObjectType {
    val Boolean: Int
    val Integer: Int
    val BitString: Int
    val OctetString: Int
    val Null: Int
    val OID: Int
    val IpAddress: Int
    val Counter: Int
    val Gauge: Int
    val TimeTicks: Int
    val Opaque: Int
    val Counter64: Int
    val NoSuchObject: Int
    val NoSuchInstance: Int
    val EndOfMibView: Int

    // Reverse mapping (code to name)
    @JsName("1")
    val Boolean_STRING: String
    @JsName("2")
    val Integer_STRING: String
    @JsName("3")
    val BitString_STRING: String
    @JsName("4")
    val OctetString_STRING: String
    @JsName("5")
    val Null_STRING: String
    @JsName("6")
    val OID_STRING: String
    @JsName("64")
    val IpAddress_STRING: String
    @JsName("65")
    val Counter_STRING: String
    @JsName("66")
    val Gauge_STRING: String
    @JsName("67")
    val TimeTicks_STRING: String
    @JsName("68")
    val Opaque_STRING: String
    @JsName("70")
    val Counter64_STRING: String
    @JsName("128")
    val NoSuchObject_STRING: String
    @JsName("129")
    val NoSuchInstance_STRING: String
    @JsName("130")
    val EndOfMibView_STRING: String

    // Aliases
    val INTEGER: Int
    @JsName("OCTET STRING")
    val OCTET_STRING: Int
    @JsName("OBJECT IDENTIFIER")
    val OBJECT_IDENTIFIER: Int
    val Integer32: Int
    val Counter32: Int
    val Gauge32: Int
    val Unsigned32: Int
}

/** SNMP PDU Type codes and names */
external object PduType {
    val GetRequest: Int
    val GetNextRequest: Int
    val GetResponse: Int
    val SetRequest: Int
    val Trap: Int
    val GetBulkRequest: Int
    val InformRequest: Int
    val TrapV2: Int
    val Report: Int

    // Reverse mapping (code to name)
    @JsName("160")
    val GetRequest_STRING: String
    @JsName("161")
    val GetNextRequest_STRING: String
    @JsName("162")
    val GetResponse_STRING: String
    @JsName("163")
    val SetRequest_STRING: String
    @JsName("164")
    val Trap_STRING: String
    @JsName("165")
    val GetBulkRequest_STRING: String
    @JsName("166")
    val InformRequest_STRING: String
    @JsName("167")
    val TrapV2_STRING: String
    @JsName("168")
    val Report_STRING: String
}

/** SNMPv1 Trap Type codes and names */
external object TrapType {
    val ColdStart: Int
    val WarmStart: Int
    val LinkDown: Int
    val LinkUp: Int
    val AuthenticationFailure: Int
    val EgpNeighborLoss: Int
    val EnterpriseSpecific: Int

    // Reverse mapping (code to name)
    @JsName("0")
    val ColdStart_STRING: String
    @JsName("1")
    val WarmStart_STRING: String
    @JsName("2")
    val LinkDown_STRING: String
    @JsName("3")
    val LinkUp_STRING: String
    @JsName("4")
    val AuthenticationFailure_STRING: String
    @JsName("5")
    val EgpNeighborLoss_STRING: String
    @JsName("6")
    val EnterpriseSpecific_STRING: String
}

/** SNMPv3 Security Level codes and names */
external object SecurityLevel {
    val noAuthNoPriv: Int
    val authNoPriv: Int
    val authPriv: Int

    // Reverse mapping (code to name)
    @JsName("1")
    val noAuthNoPriv_STRING: String
    @JsName("2")
    val authNoPriv_STRING: String
    @JsName("3")
    val authPriv_STRING: String
}

/** SNMPv3 Authentication Protocol names and codes */
external object AuthProtocols {
    val none: String
    val md5: String
    val sha: String
    val sha224: String
    val sha256: String
    val sha384: String
    val sha512: String

    // Reverse mapping (name to code)
    @JsName("1")
    val none_CODE: Int
    @JsName("2")
    val md5_CODE: Int
    @JsName("3")
    val sha_CODE: Int
    @JsName("4")
    val sha224_CODE: Int
    @JsName("5")
    val sha256_CODE: Int
    @JsName("6")
    val sha384_CODE: Int
    @JsName("7")
    val sha512_CODE: Int
}

/** SNMPv3 Privacy Protocol names and codes */
external object PrivProtocols {
    val none: String
    val des: String
    val aes: String
    val aes256b: String
    val aes256r: String

    // Reverse mapping (name to code)
    @JsName("1")
    val none_CODE: Int
    @JsName("2")
    val des_CODE: Int
    @JsName("4")
    val aes_CODE: Int
    @JsName("6")
    val aes256b_CODE: Int
    @JsName("8")
    val aes256r_CODE: Int
}

/** SNMPv3 USM Stats OID base */
external val UsmStatsBase: String

/** SNMPv3 USM Stats error names and codes */
external object UsmStats {
    @JsName("Unsupported Security Level")
    val UnsupportedSecurityLevel: String

    @JsName("Not In Time Window")
    val NotInTimeWindow: String

    @JsName("Unknown User Name")
    val UnknownUserName: String

    @JsName("Unknown Engine ID")
    val UnknownEngineID: String

    @JsName("Wrong Digest (incorrect password, community or key)")
    val WrongDigest: String

    @JsName("Decryption Error")
    val DecryptionError: String

    // Reverse mapping (name to code)
    @JsName("1")
    val UnsupportedSecurityLevel_CODE: Int
    @JsName("2")
    val NotInTimeWindow_CODE: Int
    @JsName("3")
    val UnknownUserName_CODE: Int
    @JsName("4")
    val UnknownEngineID_CODE: Int
    @JsName("5")
    val WrongDigest_CODE: Int
    @JsName("6")
    val DecryptionError_CODE: Int
}

/** MIB Provider Type codes and names */
external object MibProviderType {
    val Scalar: Int
    val Table: Int

    // Reverse mapping (code to name)
    @JsName("1")
    val Scalar_STRING: String
    @JsName("2")
    val Table_STRING: String
}

/** AgentX PDU Type codes and names */
external object AgentXPduType {
    val Open: Int
    val Close: Int
    val Register: Int
    val Unregister: Int
    val Get: Int
    val GetNext: Int
    val GetBulk: Int
    val TestSet: Int
    val CommitSet: Int
    val UndoSet: Int
    val CleanupSet: Int
    val Notify: Int
    val Ping: Int
    val IndexAllocate: Int
    val IndexDeallocate: Int
    val AddAgentCaps: Int
    val RemoveAgentCaps: Int
    val Response: Int

    // Reverse mapping (code to name)
    @JsName("1")
    val Open_STRING: String
    @JsName("2")
    val Close_STRING: String
    @JsName("3")
    val Register_STRING: String
    @JsName("4")
    val Unregister_STRING: String
    @JsName("5")
    val Get_STRING: String
    @JsName("6")
    val GetNext_STRING: String
    @JsName("7")
    val GetBulk_STRING: String
    @JsName("8")
    val TestSet_STRING: String
    @JsName("9")
    val CommitSet_STRING: String
    @JsName("10")
    val UndoSet_STRING: String
    @JsName("11")
    val CleanupSet_STRING: String
    @JsName("12")
    val Notify_STRING: String
    @JsName("13")
    val Ping_STRING: String
    @JsName("14")
    val IndexAllocate_STRING: String
    @JsName("15")
    val IndexDeallocate_STRING: String
    @JsName("16")
    val AddAgentCaps_STRING: String
    @JsName("17")
    val RemoveAgentCaps_STRING: String
    @JsName("18")
    val Response_STRING: String
}

/** Access Control Model Type codes and names */
external object AccessControlModelType {
    val None: Int
    val Simple: Int

    // Reverse mapping (code to name)
    @JsName("0")
    val None_STRING: String
    @JsName("1")
    val Simple_STRING: String
}

/** Access Level codes and names */
external object AccessLevel {
    val None: Int
    val ReadOnly: Int
    val ReadWrite: Int

    // Reverse mapping (code to name)
    @JsName("0")
    val None_STRING: String
    @JsName("1")
    val ReadOnly_STRING: String
    @JsName("2")
    val ReadWrite_STRING: String
}

/** SMIv2 MAX-ACCESS value codes and names */
external object MaxAccess {
    @JsName("not-accessible")
    val notAccessible: Int
    @JsName("accessible-for-notify")
    val accessibleForNotify: Int
    @JsName("read-only")
    val readOnly: Int
    @JsName("read-write")
    val readWrite: Int
    @JsName("read-create")
    val readCreate: Int

    // Reverse mapping (code to name)
    @JsName("0")
    val notAccessible_STRING: String
    @JsName("1")
    val accessibleForNotify_STRING: String
    @JsName("2")
    val readOnly_STRING: String
    @JsName("3")
    val readWrite_STRING: String
    @JsName("4")
    val readCreate_STRING: String
}

/** Row Status codes and names (used for values and actions) */
external object RowStatus {
    // status values
    val active: Int
    val notInService: Int
    val notReady: Int

    // actions
    val createAndGo: Int
    val createAndWait: Int
    val destroy: Int

    // Reverse mapping (code to name)
    @JsName("1")
    val active_STRING: String
    @JsName("2")
    val notInService_STRING: String
    @JsName("3")
    val notReady_STRING: String
    @JsName("4")
    val createAndGo_STRING: String
    @JsName("5")
    val createAndWait_STRING: String
    @JsName("6")
    val destroy_STRING: String
}

/** Internal codes for ResponseInvalidError */
external object ResponseInvalidCode {
    val EIp4AddressSize: Int
    val EUnknownObjectType: Int
    val EUnknownPduType: Int
    val ECouldNotDecrypt: Int
    val EAuthFailure: Int
    val EReqResOidNoMatch: Int
    val EOutOfOrder: Int
    val EVersionNoMatch: Int
    val ECommunityNoMatch: Int
    val EUnexpectedReport: Int
    val EResponseNotHandled: Int
    val EUnexpectedResponse: Int

    // Reverse mapping (code to name)
    @JsName("1")
    val EIp4AddressSize_STRING: String
    @JsName("2")
    val EUnknownObjectType_STRING: String
    @JsName("3")
    val EUnknownPduType_STRING: String
    @JsName("4")
    val ECouldNotDecrypt_STRING: String
    @JsName("5")
    val EAuthFailure_STRING: String
    @JsName("6")
    val EReqResOidNoMatch_STRING: String
    @JsName("8")
    val EOutOfOrder_STRING: String
    @JsName("9")
    val EVersionNoMatch_STRING: String
    @JsName("10")
    val ECommunityNoMatch_STRING: String
    @JsName("11")
    val EUnexpectedReport_STRING: String
    @JsName("12")
    val EResponseNotHandled_STRING: String
    @JsName("13")
    val EUnexpectedResponse_STRING: String
}

/** OID translation formats */
external object OidFormat {
    val oid: String
    val path: String
    val module: String
}

// --- Error Classes ---

///** Error indicating an invalid response was received */
//external class ResponseInvalidError(message: String, code: Int? = definedExternally, info: Any? = definedExternally) : JsError
//
///** Error indicating an invalid request was constructed */
//external class RequestInvalidError( message: String) : JsError
//
///** Error indicating a request failed on the remote agent */
//external class RequestFailedError(message: String, status: Int? = definedExternally) : JsError
//
///** Error indicating a request timed out */
//external class RequestTimedOutError(message: String) : JsError
//
///** Error indicating a failure during incoming message processing */
//external class ProcessingError(message: String, error: JsError? = definedExternally, rinfo: RemoteInfo? = definedExternally, buffer: Buffer? = definedExternally) : JsError

// --- Interfaces and Types ---

/** Represents an SNMP Varbind */
external class Varbind {
    var oid: String
    var type: Int // ObjectType enum value
    var value: Any?

    // Additional properties potentially added by Agent/AgentX handlers or internal processing
    var errorStatus: Int? get() = definedExternally
    var requestType: Int? get() = definedExternally
    var requestValue: Any? get() = definedExternally
    var oldValue: Any? get() = definedExternally
    var autoCreated: Boolean? get() = definedExternally
    var deleted: Boolean? get() = definedExternally
    var providerName: String? get() = definedExternally
    var previousOid: String? get() = definedExternally
    var column: Int? get() = definedExternally
    var columnPosition: Int? get() = definedExternally
    var rowIndex: Array<Any>? get() = definedExternally
    var row: Array<Any>? get() = definedExternally
}

/** Represents an SNMPv3 User */
external class User {
    var name: String
    var level: Int // SecurityLevel enum value
    var authProtocol: String? get() = definedExternally // AuthProtocols string value
    var authKey: dynamic /* String | Buffer */ get() = definedExternally
    var privProtocol: String? get() = definedExternally // PrivProtocols string value
    var privKey: dynamic /* String | Buffer */ get() = definedExternally
}

/** Options for creating an SNMP Session */
external class SessionOptions {
    var port: Int? get() = definedExternally
    var trapPort: Int? get() = definedExternally
    var version: Int? get() = definedExternally // Version1, Version2c, or Version3
    var retries: Int? get() = definedExternally
    var timeout: Int? get() = definedExternally
    var backoff: Double? get() = definedExternally
    var transport: String? get() = definedExternally // E.g., "udp4", "udp6"
    var sourceAddress: String? get() = definedExternally
    var sourcePort: Int? get() = definedExternally
    var context: String? get() = definedExternally
    var idBitsSize: Int? get() = definedExternally // 16 or 32
    var engineID: dynamic /* String | Buffer */ get() = definedExternally // Hex string or Buffer
    var backwardsGetNexts: Boolean? get() = definedExternally
    var reportOidMismatchErrors: Boolean? get() = definedExternally
    var debug: Boolean? get() = definedExternally
}

/** Options for PDUs */
external class PduOptions {
    var nonRepeaters: Int? get() = definedExternally
    var maxRepetitions: Int? get() = definedExternally
    var context: String? get() = definedExternally
    var agentAddr: String? get() = definedExternally // For TrapPdu
    var upTime: Int? get() = definedExternally // For TrapPdu, InformRequestPdu, TrapV2Pdu
}

// --- PDU Base Classes (Abstract) ---

/** Base class for most SNMP PDUs (Get, GetNext, Set, GetBulk, Inform, TrapV2) */
external abstract class SimplePdu {
    open var type: Int // PduType enum value
    open var id: Int
    open var varbinds: Array<Varbind>
    open var options: PduOptions
    open var contextName: String
    open var contextEngineID: Buffer?
    open var scoped: Boolean?

    // Note: nonRepeaters and maxRepetitions are read from `options` during construction/sending

    /** Encodes the PDU into a BER writer */
    fun toBuffer(buffer: dynamic /* asn1-ber.Writer */): Unit

    /** Creates a GetResponse PDU corresponding to this request PDU */
    fun getResponsePduForRequest(): GetResponsePdu

    companion object {
        /** Creates a PDU instance */
        fun createFromVariables(
            pduClass: JsClass<SimplePdu>,
            id: Int,
            varbinds: Array<Varbind>,
            options: PduOptions? = definedExternally
        ): SimplePdu
    }
}

/** Base class for SNMP Response PDUs (GetResponse, Report) */
external abstract class SimpleResponsePdu : SimplePdu {
    override var type: Int // PduType enum value (GetResponse or Report)
    var errorStatus: Int? // ErrorStatus enum value
    var errorIndex: Int?
}

// --- PDU Concrete Classes ---

/** SNMP GetRequest PDU */
external class GetRequestPdu : SimplePdu {
    companion object {
        /** Decodes a PDU from a BER reader */
        fun createFromBuffer(reader: dynamic /* asn1-ber.Reader */): GetRequestPdu

        /** Creates a GetRequest PDU instance */
        fun createFromVariables(
            id: Int,
            varbinds: Array<Varbind>,
            options: PduOptions? = definedExternally
        ): GetRequestPdu
    }
}

/** SNMP GetNextRequest PDU */
external class GetNextRequestPdu : SimplePdu {
    companion object {
        fun createFromBuffer(reader: dynamic /* asn1-ber.Reader */): GetNextRequestPdu
    }
}

/** SNMP SetRequest PDU */
external class SetRequestPdu : SimplePdu {
    companion object {
        fun createFromBuffer(reader: dynamic /* asn1-ber.Reader */): SetRequestPdu
    }
}

/** SNMP GetBulkRequest PDU */
external class GetBulkRequestPdu : SimplePdu {
    companion object {
        fun createFromBuffer(reader: dynamic /* asn1-ber.Reader */): GetBulkRequestPdu
    }
}

/** SNMP InformRequest PDU */
external class InformRequestPdu : SimplePdu {
    companion object {
        fun createFromBuffer(reader: dynamic /* asn1-ber.Reader */): InformRequestPdu
    }
}

/** SNMPv2 Trap PDU */
external class TrapV2Pdu : SimplePdu {
    companion object {
        /** Decodes a PDU from a BER reader */
        fun createFromBuffer(reader: dynamic /* asn1-ber.Reader */): TrapV2Pdu

        /** Creates a TrapV2 PDU instance */
        fun createFromVariables(id: Int, varbinds: Array<Varbind>, options: PduOptions? = definedExternally): TrapV2Pdu
    }
}

/** SNMP GetResponse PDU */
external class GetResponsePdu : SimpleResponsePdu {
    companion object {
        /** Decodes a PDU from a BER reader */
        fun createFromBuffer(reader: dynamic /* asn1-ber.Reader */): GetResponsePdu

        /** Creates a GetResponse PDU instance */
        fun createFromVariables(
            id: Int,
            varbinds: Array<Varbind>,
            options: PduOptions? = definedExternally
        ): GetResponsePdu
    }
}

/** SNMPv3 Report PDU */
external class ReportPdu : SimpleResponsePdu {
    companion object {
        /** Decodes a PDU from a BER reader */
        fun createFromBuffer(reader: dynamic /* asn1-ber.Reader */): ReportPdu

        /** Creates a Report PDU instance */
        fun createFromVariables(id: Int, varbinds: Array<Varbind>, options: PduOptions? = definedExternally): ReportPdu
    }
}

/** SNMPv1 Trap PDU */
external class TrapPdu {
    val type: Int // PduType.Trap
    var enterprise: String
    var agentAddr: String
    var generic: Int // TrapType enum value
    var specific: Int
    var upTime: Int?
    var varbinds: Array<Varbind>

    /** Encodes the PDU into a BER writer */
    fun toBuffer(buffer: dynamic /* asn1-ber.Writer */): Unit

    companion object {
        /** Decodes a PDU from a BER reader */
        fun createFromBuffer(reader: dynamic /* asn1-ber.Reader */): TrapPdu

        /** Creates a Trap PDU instance */
        fun createFromVariables(
            typeOrOid: dynamic /* Int | String */,
            varbinds: Array<Varbind>,
            options: PduOptions? = definedExternally
        ): TrapPdu
    }
}

// --- Message Class ---

/** SNMPv3 Global Data structure */
external interface MsgGlobalData {
    var msgID: Int
    var msgMaxSize: Int
    var msgFlags: Int // Bitmask: 1=auth, 2=priv, 4=reportable
    var msgSecurityModel: Int // Should be 3 for USM
}

/** SNMPv3 Security Parameters structure */
external interface MsgSecurityParameters {
    var msgAuthoritativeEngineID: Buffer
    var msgAuthoritativeEngineBoots: Int
    var msgAuthoritativeEngineTime: Int
    var msgUserName: String
    var msgAuthenticationParameters: dynamic /* String | Buffer */ // Type depends on creation vs parsing
    var msgPrivacyParameters: Buffer
}

/** Represents a complete SNMP message (v1, v2c, or v3) */
external class Message {
    var version: Int // Version1, Version2c, or Version3
    var community: String? // For v1/v2c
    var user: User? // For v3
    var pdu: SimplePdu // Can be any concrete PDU type, including TrapPdu
    var msgGlobalData: MsgGlobalData? // For v3
    var msgSecurityParameters: MsgSecurityParameters? // For v3
    var encryptedPdu: Buffer? // For v3 encrypted message
    var disableAuthentication: Boolean? // Internal flag set by Authorizer

    /** Gets the request ID (PDU ID for v1/v2c, msgID for v3) */
    fun getReqId(): Int

    /** Encodes the entire message to a Buffer */
    fun toBuffer(): Buffer

    /** Processes incoming security parameters (authentication, decryption) */
    fun processIncomingSecurity(user: User, responseCb: (error: JsError?) -> Unit): Boolean

    /** Checks if the authentication flag is set (v3) */
    fun hasAuthentication(): Boolean

    /** Checks if the privacy flag is set (v3) */
    fun hasPrivacy(): Boolean

    /** Checks if the reportable flag is set (v3) */
    fun isReportable(): Boolean

    /** Creates a response message corresponding to this request message */
    fun createResponseForRequest(responsePdu: SimplePdu): Message

    /** Creates a Report response message for a discovery request */
    fun createReportResponseMessage(engine: Engine, context: String? = definedExternally): Message

    companion object {
        /** Creates a v1/v2c message */
        fun createCommunity(version: Int, community: String, pdu: SimplePdu): Message

        /** Creates a v3 request message */
        fun createRequestV3(user: User, msgSecurityParameters: MsgSecurityParameters, pdu: SimplePdu): Message

        /** Creates a generic v3 message */
        fun createV3(
            user: User,
            msgGlobalData: MsgGlobalData,
            msgSecurityParameters: MsgSecurityParameters,
            pdu: SimplePdu
        ): Message

        /** Creates a v3 discovery request message */
        fun createDiscoveryV3(pdu: SimplePdu): Message

        /** Decodes a message from a Buffer */
        fun createFromBuffer(buffer: Buffer): Message
    }
}

// --- Session Class ---

typealias SessionResponseCallback = (error: JsError?, varbinds: Array<Varbind>) -> Unit
typealias SessionWalkFeedCallback = (varbinds: Array<Varbind>) -> Boolean // Return true to stop walk
typealias SessionDoneCallback = (error: JsError?) -> Unit
typealias SessionSubtreeFeedCallback = (varbinds: Array<Varbind>) -> Boolean // Return true to stop subtree walk
typealias SessionTableResponseCallback = (error: JsError?, table: dynamic /* JsObject with { index: { column: value } } structure */) -> Unit
typealias SessionTrapCallback = (error: JsError?) -> Unit

/** Represents an SNMP client session */
external class Session(
    target: String? = definedExternally,
    authenticator: dynamic /* String | User */ = definedExternally,
    options: SessionOptions? = definedExternally
) : EventEmitter {
    val target: String
    val version: Int
    val community: String? // Populated for v1/v2c
    val user: User? // Populated for v3
    val transport: String
    val port: Int
    val trapPort: Int
    val retries: Int
    val timeout: Int
    val backoff: Double
    val sourceAddress: String?
    val sourcePort: Int?
    val idBitsSize: Int
    val context: String
    val engine: Engine
    val dgram: DgramSocket // The underlying UDP socket

    /** Sends a GetRequest PDU */
    fun get(oids: Array<String>, responseCb: SessionResponseCallback): Session

    /** Sends a GetNextRequest PDU */
    fun getNext(oids: Array<String>, responseCb: SessionResponseCallback): Session

    /** Sends a GetBulkRequest PDU */
    fun getBulk(
        oids: Array<String>,
        nonRepeaters: Int? = definedExternally,
        maxRepetitions: Int? = definedExternally,
        responseCb: SessionResponseCallback
    ): Session

    /** Sends a SetRequest PDU */
    fun set(varbinds: Array<Varbind>, responseCb: SessionResponseCallback): Session

    /** Sends an InformRequest PDU (SNMPv2c/v3 only) */
    fun inform(
        typeOrOid: dynamic /* Int | String */,
        varbinds: Array<Varbind>? = definedExternally,
        options: PduOptions? = definedExternally,
        responseCb: SessionResponseCallback
    ): Session
    // Note: options argument might also be used for agentAddr string in older signatures - use PduOptions consistently

    /** Performs a walk operation using GetNext or GetBulk */
    fun walk(
        oid: String,
        maxRepetitions: Int? = definedExternally,
        feedCb: SessionWalkFeedCallback,
        doneCb: SessionDoneCallback
    ): Session

    /** Performs a subtree walk, similar to walk but filters results */
    fun subtree(
        oid: String,
        maxRepetitions: Int? = definedExternally,
        feedCb: SessionSubtreeFeedCallback,
        doneCb: SessionDoneCallback
    ): Session

    /** Retrieves an entire table */
    fun table(oid: String, maxRepetitions: Int? = definedExternally, responseCb: SessionTableResponseCallback): Session

    /** Retrieves specific columns of a table */
    fun tableColumns(
        oid: String,
        columns: Array<Int>,
        maxRepetitions: Int? = definedExternally,
        responseCb: SessionTableResponseCallback
    ): Session

    /** Sends a Trap PDU (v1) or TrapV2 PDU (v2c/v3) */
    fun trap(
        typeOrOid: dynamic /* Int | String */,
        varbinds: Array<Varbind>? = definedExternally,
        options: PduOptions? /* or agentAddr: String? */ = definedExternally,
        responseCb: SessionTrapCallback
    ): Session
    // Note: options argument might also be used for agentAddr string in older signatures - use PduOptions consistently

    /** Closes the session socket */
    fun close(): Session

    companion object {
        /** Creates an SNMP v1/v2c session */
        fun create(
            target: String? = definedExternally,
            community: String? = definedExternally,
            options: SessionOptions? = definedExternally
        ): Session

        /** Creates an SNMP v3 session */
        fun createV3(
            target: String? = definedExternally,
            user: User,
            options: SessionOptions? = definedExternally
        ): Session
    }
}

// --- Receiver ---

/** Configuration for a single listening socket */
external interface SocketOption {
    var transport: String? get() = definedExternally // default 'udp4'
    var address: String? get() = definedExternally // default null
    var port: Int? get() = definedExternally // default 161 or 162
}

/** Options for creating an SNMP Receiver (Trap/Inform listener) */
external interface ReceiverOptions {
    var port: Int? get() = definedExternally // Default 162
    var transport: String? get() = definedExternally // Default 'udp4'
    var address: String? get() = definedExternally

    /** Alternative way to specify multiple sockets */
    var sockets: Array<SocketOption>? get() = definedExternally
    var disableAuthorization: Boolean? get() = definedExternally
    var includeAuthentication: Boolean? get() = definedExternally // Include community/user in callback data
    var engineID: dynamic /* String | Buffer */ get() = definedExternally // Hex string or Buffer
    var accessControlModelType: Int? get() = definedExternally // AccessControlModelType enum value
    var debug: Boolean? get() = definedExternally
}

/** Data structure passed to the Receiver callback */
external interface FormattedCallbackData {
    var pdu: SimplePdu // Actually GetResponsePdu (for Inform) or TrapPdu/TrapV2Pdu
    var rinfo: RemoteInfo
}

/** Callback function type for the Receiver */
typealias ReceiverCallback = (error: JsError?, data: FormattedCallbackData?) -> Unit

/** Listens for incoming SNMP Trap and Inform messages */
external class Receiver(options: ReceiverOptions, callback: ReceiverCallback) {
    val authorizer: Authorizer
    val engine: Engine
    val listener: Listener // Internal listener helper

    /** Gets the Authorizer instance */
    fun getAuthorizer(): Authorizer

    /** Closes all listening sockets */
    fun close(callback: ((socketInfo: dynamic) -> Unit)? = definedExternally): Unit

    companion object {
        /** Creates and starts a Receiver */
        fun create(options: ReceiverOptions, callback: ReceiverCallback): Receiver
    }
}

// --- Authorizer & Access Control ---

/** Options for creating an Authorizer */
external interface AuthorizerOptions {
    var disableAuthorization: Boolean? get() = definedExternally
    var accessControlModelType: Int? get() = definedExternally // AccessControlModelType enum value
}

/** Manages community strings and SNMPv3 users for authorization */
external class Authorizer(options: AuthorizerOptions? = definedExternally) {
    var communities: Array<String>
    var users: Array<User>
    var disableAuthorization: Boolean?
    var accessControlModelType: Int // AccessControlModelType enum value
    var accessControlModel: SimpleAccessControlModel? // Or potentially other models

    /** Adds a community string */
    fun addCommunity(community: String): Unit

    /** Gets a specific community string if it exists */
    fun getCommunity(community: String): String?

    /** Gets all registered community strings */
    fun getCommunities(): Array<String>

    /** Deletes a community string */
    fun deleteCommunity(community: String): Unit

    /** Adds or updates an SNMPv3 user */
    fun addUser(user: User): Unit

    /** Gets a specific user by name */
    fun getUser(userName: String): User?

    /** Gets all registered users */
    fun getUsers(): Array<User>

    /** Deletes a user by name */
    fun deleteUser(userName: String): Unit

    /** Gets the configured Access Control Model type */
    fun getAccessControlModelType(): Int

    /** Gets the Access Control Model instance */
    fun getAccessControlModel(): SimpleAccessControlModel? // Or potentially other models

    /** Checks if access is allowed based on the configured model */
    fun isAccessAllowed(securityModel: Int, securityName: String, pduType: Int): Boolean
}

/** Base interface for access control entries */
external interface AccessEntry {
    var level: Int // AccessLevel enum value
}

/** Access control entry for a community */
external interface CommunityAccessEntry : AccessEntry {
    var community: String
}

/** Access control entry for a user */
external interface UserAccessEntry : AccessEntry {
    var userName: String
}

/** Simple Access Control Model implementation */
external class SimpleAccessControlModel {
    var communitiesAccess: Array<CommunityAccessEntry>
    var usersAccess: Array<UserAccessEntry>

    /** Gets the access entry for a community */
    fun getCommunityAccess(community: String): CommunityAccessEntry?

    /** Gets the access level for a community */
    fun getCommunityAccessLevel(community: String): Int // AccessLevel enum value

    /** Gets all community access entries */
    fun getCommunitiesAccess(): Array<CommunityAccessEntry>

    /** Sets the access level for a community */
    fun setCommunityAccess(community: String, accessLevel: Int): Unit

    /** Removes access control for a community */
    fun removeCommunityAccess(community: String): Unit

    /** Gets the access entry for a user */
    fun getUserAccess(userName: String): UserAccessEntry?

    /** Gets the access level for a user */
    fun getUserAccessLevel(user: String): Int // AccessLevel enum value

    /** Gets all user access entries */
    fun getUsersAccess(): Array<UserAccessEntry>

    /** Sets the access level for a user */
    fun setUserAccess(userName: String, accessLevel: Int): Unit

    /** Removes access control for a user */
    fun removeUserAccess(userName: String): Unit

    /** Checks if access is allowed */
    fun isAccessAllowed(securityModel: Int, securityName: String, pduType: Int): Boolean
}

// --- Engine ---
/** Represents an SNMP Engine (primarily for SNMPv3) */
external class Engine(
    engineID: dynamic /* String | Buffer */ = definedExternally,
    engineBoots: Int? = definedExternally,
    engineTime: Int? = definedExternally
) {
    var engineID: Buffer
    var engineBoots: Int
    var engineTime: Int

    // generateEngineID is called internally if no engineID is provided
}

// --- Listener (Internal Helper) ---
/** Internal helper class for managing listening sockets */
external class Listener(options: dynamic, receiver: dynamic /* Receiver | Agent */) {
    /** Binds sockets and starts listening */
    fun startListening(): Unit

    /** Sends an SNMP message */
    fun send(message: Message, rinfo: RemoteInfo, socket: DgramSocket): Unit

    /** Closes all managed sockets */
    fun close(callback: ((socketInfo: dynamic) -> Unit)? = definedExternally): Unit

    companion object {
        /** Formats data for the Receiver/Agent callback */
        fun formatCallbackData(pdu: SimplePdu, rinfo: RemoteInfo): FormattedCallbackData

        /** Processes an incoming buffer, performs authorization, and returns a Message object */
        fun processIncoming(
            buffer: Buffer,
            authorizer: Authorizer,
            callback: ReceiverCallback /* or AgentCallback */
        ): Message?
    }
}

// --- MIB Store ---

/** Loads and manages MIB modules */
external class ModuleStore {
    /** Loads MIB definitions from a file */
    fun loadFromFile(fileName: String): Unit

    /** Gets the parsed structure of a specific MIB module */
    fun getModule(moduleName: String): dynamic // Parsed MIB module structure (depends on mibparser)

    /** Gets all loaded MIB modules */
    fun getModules(includeBase: Boolean? = definedExternally): dynamic // JsObject { moduleName: module }

    /** Gets the names of all loaded MIB modules */
    fun getModuleNames(includeBase: Boolean? = definedExternally): Array<String>

    /** Extracts provider definitions (scalar/table) from a loaded module */
    fun getProvidersForModule(moduleName: String): Array<ProviderDefinition>

    /** Translates between OID, path, and module::name formats */
    fun translate(name: String, destinationFormat: String /* OidFormat string value */): String

    /** Gets all known syntax types, including textual conventions */
    fun getSyntaxTypes(): dynamic // JsObject { SyntaxName: ObjectType | SyntaxDefinition }

    companion object {
        /** Creates a new ModuleStore with base MIBs loaded */
        fun create(): ModuleStore

        /** List of base MIB modules automatically loaded */
        val BASE_MODULES: Array<String>
    }
}

// --- MIB ---

/** Options for creating a MIB instance */
external interface MibOptions {
    /** Automatically set scalar value from DEFVAL when provider is registered */
    var addScalarDefaultsOnRegistration: Boolean? get() = definedExternally
}

/** Represents a node in the MIB tree */
external interface MibNode {
    val address: Array<Int>
    val oid: String
    val parent: MibNode?
    val children: dynamic // JsObject { index: MibNode }
    val provider: ProviderDefinition? // Set if this node is the root of a provider subtree
    var value: Any? // Value if this is an instance node (leaf)
    var valueType: Int? // ObjectType enum value if this is an instance node

    /** Gets a direct child node by its OID component index */
    fun child(index: Int): MibNode?

    /** Lists the integer indices of children, sorted */
    fun listChildren(lowest: Int? = definedExternally): Array<Int>

    /** Finds the child node immediately preceding the given index */
    fun findChildImmediatelyBefore(index: Int): MibNode?

    /** Checks if the given address is a descendant of this node */
    fun isDescendant(address: Array<Int>): Boolean

    /** Checks if the given address is an ancestor of this node */
    fun isAncestor(address: Array<Int>): Boolean

    /** Traverses up the tree to find the nearest ancestor with a provider */
    fun getAncestorProvider(): MibNode?

    /** For an instance node within a table, finds the column number */
    fun getTableColumnFromInstanceNode(): Int?

    /** Gets the MIB constraints (range, size, enum) applicable to this instance node */
    fun getConstraintsFromProvider(): MibConstraints?

    /** Sets the value of this instance node, validating against constraints */
    fun setValue(typeFromSet: Int, valueFromSet: Any): Boolean // Returns true on success

    /** Finds the single instance node representing a conceptual row (only if index has one component?) */
    fun getInstanceNodeForTableRow(): MibNode?

    /** Finds the specific instance node corresponding to a table row index */
    fun getInstanceNodeForTableRowIndex(index: Array<Int>): MibNode?

    /** Finds all instance nodes belonging to the same table column as this node */
    fun getInstanceNodesForColumn(): Array<MibNode>

    /** Finds the next lexicographical instance node in the MIB */
    fun getNextInstanceNode(): MibNode?

    /** Deletes this leaf node from the MIB */
    fun delete(): Unit

    /** Recursively deletes parent nodes if they become empty after a child deletion */
    fun pruneUpwards(): Unit

    /** Dumps the MIB subtree rooted at this node to the console */
    fun dump(options: DumpOptions? = definedExternally): Unit

    companion object {
        /** Checks if one OID is a descendant of another */
        fun oidIsDescended(oid: dynamic /* String | Array<Int> */, ancestor: dynamic /* String | Array<Int> */): Boolean
    }
}

/** Options for dumping the MIB content */
external interface DumpOptions {
    var leavesOnly: Boolean? get() = definedExternally // Default: true
    var showProviders: Boolean? get() = definedExternally // Default: true
    var showValues: Boolean? get() = definedExternally // Default: true
    var showTypes: Boolean? get() = definedExternally // Default: true
}

/** MIB constraint for integer ranges or string/octet sizes */
external interface MibRange {
    var min: Number;
    var max: Number
} // Number can be Int or Double

/** MIB constraint for exact string/octet sizes */
external interface MibSize {
    var min: Int;
    var max: Int
}

/** MIB constraints (range, size, or enumeration) */
external interface MibConstraints {
    var ranges: Array<MibRange>? get() = definedExternally
    var sizes: Array<MibSize>? get() = definedExternally
    var enumeration: dynamic? get() = definedExternally // JsObject { number: name }
}

/** Definition for a table column within a MIB provider */
external interface TableColumnDefinition {
    var number: Int // The OID component identifying the column
    var name: String // The MIB name of the column
    var type: Int // ObjectType enum value
    var maxAccess: Int // MaxAccess enum value
    var constraints: MibConstraints? get() = definedExternally
    var defVal: Any? get() = definedExternally // Default value
    var rowStatus: Boolean? get() = definedExternally // True if this column is RowStatus
}

/** Definition for a table index component */
external interface TableIndexEntry {
    var columnName: String? get() = definedExternally
    var columnNumber: Int? get() = definedExternally
    var implied: Boolean? get() = definedExternally // True if length is not prepended for OctetString/OID
    var foreign: String? get() = definedExternally // Name of the foreign table provider if index is external
    var type: Int? get() = definedExternally // ObjectType, added during MIB processing
    var length: Int? get() = definedExternally // Fixed length (e.g., for IpAddress), added during MIB processing
}

/** Base definition for a MIB provider (Scalar or Table) */
external interface BaseProviderDefinition {
    var name: String // Unique name for the provider
    var oid: String // The OID corresponding to the provider root
    var type: Int // MibProviderType enum value

    /** Handler function called for Get/GetNext/Set requests */
    var handler: ((request: MibRequest) -> Unit)? get() = definedExternally

    /** Handler function called to create instances (read-create or RowStatus) */
    var createHandler: ((request: CreateRequest) -> Any?)? get() = definedExternally // Return value depends on provider type
    var maxAccess: Int? // MaxAccess enum value (mainly for scalars, columns have their own)
    var defVal: Any? // Default value (mainly for scalars, columns have their own)
}

/** Definition for a scalar MIB provider */
external interface ScalarProviderDefinition : BaseProviderDefinition {
    override var type: Int /* = MibProviderType.Scalar */
    var scalarType: Int // ObjectType enum value
    var constraints: MibConstraints? get() = definedExternally
}

/** Definition for a table MIB provider */
external interface TableProviderDefinition : BaseProviderDefinition {
    override var type: Int /* = MibProviderType.Table */
    var tableColumns: Array<TableColumnDefinition>

    /** Definition of the table's index columns */
    var tableIndex: Array<dynamic /* Int | String | TableIndexEntry */> // Processed into Array<TableIndexEntry>

    /** Name of the table this table augments, if any */
    var tableAugments: String? get() = definedExternally
}
/** Union type for provider definitions (use `as` cast in Kotlin) */
typealias ProviderDefinition = BaseProviderDefinition

/** Manages the MIB tree structure and associated providers */
external class Mib(options: MibOptions? = definedExternally) {
    val root: MibNode
    val providers: dynamic // JsObject { name: ProviderDefinition }
    val providersByOid: dynamic // JsObject { oid: ProviderDefinition }
    val providerNodes: dynamic // JsObject { name: MibNode } // Nodes where providers are attached
    val options: MibOptions?

    /** Adds nodes to the tree for a given OID, creating parents if needed */
    fun addNodesForOid(oidString: String): MibNode

    /** Looks up an existing node by OID */
    fun lookup(oid: String): MibNode?

    /** Finds the closest preceding node in lexicographical order for GetNext */
    fun getTreeNode(oid: String): MibNode

    /** Attaches a provider definition to its corresponding node */
    fun addProviderToNode(provider: ProviderDefinition): MibNode

    /** Registers a provider definition */
    fun registerProvider(provider: ProviderDefinition): Unit

    /** Registers multiple provider definitions */
    fun registerProviders(providers: Array<ProviderDefinition>): Unit

    /** Unregisters a provider by name and removes its node if empty */
    fun unregisterProvider(name: String): Unit

    /** Gets a provider definition by name */
    fun getProvider(name: String): ProviderDefinition?

    /** Gets all registered provider definitions */
    fun getProviders(): dynamic // JsObject { name: ProviderDefinition }

    /** Dumps provider information to the console */
    fun dumpProviders(): Unit

    /** Gets the value of a registered scalar */
    fun getScalarValue(scalarName: String): Any?

    /** Sets the value of a registered scalar */
    fun setScalarValue(scalarName: String, newValue: Any): Unit

    /** Sets the default value for a scalar provider (for future instances) */
    fun setScalarDefaultValue(name: String, value: Any): Unit

    /** Sets the default values for a table's columns (for future rows) */
    fun setTableRowDefaultValues(name: String, values: Array<Any>): Unit

    /** Sets range constraints for a scalar provider */
    fun setScalarRanges(name: String, ranges: Array<MibRange>): Unit

    /** Sets range constraints for a specific table column */
    fun setTableColumnRanges(name: String, column: Int, ranges: Array<MibRange>): Unit

    /** Sets size constraints for a scalar provider */
    fun setScalarSizes(name: String, sizes: Array<MibSize>): Unit

    /** Sets size constraints for a specific table column */
    fun setTableColumnSizes(name: String, column: Int, sizes: Array<MibSize>): Unit

    /** Adds a row to a table provider */
    fun addTableRow(table: String, row: Array<Any>): Unit

    /** Gets the definitions of a table's columns */
    fun getTableColumnDefinitions(table: String): Array<TableColumnDefinition>

    /** Gets all cell values for a specific column */
    fun getTableColumnCells(
        table: String,
        columnNumber: Int,
        includeInstances: Boolean? = definedExternally
    ): dynamic /* Array<Any> | Array<Array<Any>> containing [indexValues, columnValues] */

    /** Gets all cell values for a specific row identified by its index */
    fun getTableRowCells(table: String, rowIndex: Array<Any>): Array<Any>?

    /** Gets all cell values for a table */
    fun getTableCells(
        table: String,
        byRows: Boolean? = definedExternally,
        includeInstances: Boolean? = definedExternally
    ): Array<Array<Any>>

    /** Gets the value of a single table cell */
    fun getTableSingleCell(table: String, columnNumber: Int, rowIndex: Array<Any>): Any?

    /** Sets the value of a single table cell */
    fun setTableSingleCell(table: String, columnNumber: Int, rowIndex: Array<Any>, value: Any): Unit

    /** Deletes a table row identified by its index */
    fun deleteTableRow(table: String, rowIndex: Array<Any>): Boolean // Returns true on success

    /** Finds the nearest ancestor provider for a given OID */
    fun getAncestorProviderFromOid(oid: String): ProviderDefinition?

    /** Dumps the entire MIB tree structure to the console */
    fun dump(options: DumpOptions? = definedExternally): Unit

    companion object {
        /** Creates a new MIB instance */
        fun create(options: MibOptions? = definedExternally): Mib

        /** Converts an OID string or array to an array of integers */
        fun convertOidToAddress(oid: dynamic /* String | Array<Int> */): Array<Int>

        /** Extracts the portion of an OID relative to a base OID */
        fun getSubOidFromBaseOid(oid: String, base: String): String

        /** Extracts the index values from an instance OID based on the table's index definition */
        fun getRowIndexFromOid(oid: String, index: Array<TableIndexEntry>): Array<Any>
    }
}

// --- MIB Request (Internal Helper) ---
/** Information passed to a provider's createHandler */
external interface CreateRequest {
    var provider: ProviderDefinition

    /** Action name if creation is via RowStatus (e.g., "createAndGo") */
    var action: String? get() = definedExternally

    /** Index values if creation is for a table row */
    var row: Array<Any>? get() = definedExternally
}

/** Represents a request processed by the Agent MIB */
external class MibRequest(requestDefinition: dynamic) {
    val operation: Int // PduType or AgentXPduType enum value
    val address: Array<Int>
    val oid: String
    val providerNode: MibNode?
    val provider: ProviderDefinition?
    val instanceNode: MibNode?
    val setType: Int? // ObjectType enum value (for SetRequest)
    val setValue: Any? // Value (for SetRequest)

    /** Checks if the request targets a scalar provider */
    fun isScalar(): Boolean

    /** Checks if the request targets a table provider */
    fun isTabular(): Boolean

    /** Callback function for the provider handler to signal completion */
    fun done(error: dynamic /* { errorStatus, type?, value? } */ = definedExternally): Unit
}

// --- Agent ---

/** Options for creating an SNMP Agent */
external interface AgentOptions : ReceiverOptions {
    // Inherit ReceiverOptions properties
    override var port: Int? get() = definedExternally // Default 161
    override var transport: String? get() = definedExternally
    override var address: String? get() = definedExternally
    override var sockets: Array<SocketOption>? get() = definedExternally
    override var disableAuthorization: Boolean? get() = definedExternally
    override var engineID: dynamic /* String | Buffer */ get() = definedExternally
    override var accessControlModelType: Int? get() = definedExternally
    override var debug: Boolean? get() = definedExternally
    // Agent specific options
    /** Options passed to the internal MIB instance */
    var mibOptions: MibOptions? get() = definedExternally
}

/** Callback function type for the Agent (includes responses sent) */
typealias AgentCallback = (error: JsError?, data: FormattedCallbackData?) -> Unit

/** Implements an SNMP Agent capable of responding to Get/Set/GetNext/GetBulk requests */
external class Agent(
    options: AgentOptions,
    callback: AgentCallback? = definedExternally,
    mib: Mib? = definedExternally
) {
    val listener: Listener // Internal listener helper
    val engine: Engine
    val authorizer: Authorizer
    var mib: Mib
    val forwarder: Forwarder // For proxying requests

    /** Gets the internal MIB instance */
    fun getMib(): Mib

    /** Sets the internal MIB instance */
    fun setMib(mib: Mib): Unit

    /** Gets the Authorizer instance */
    fun getAuthorizer(): Authorizer

    /** Registers a MIB provider with the internal MIB */
    fun registerProvider(provider: ProviderDefinition): Unit

    /** Registers multiple MIB providers */
    fun registerProviders(providers: Array<ProviderDefinition>): Unit

    /** Unregisters a MIB provider by name */
    fun unregisterProvider(name: String): Unit

    /** Gets a registered provider definition by name */
    fun getProvider(name: String): ProviderDefinition?

    /** Gets all registered provider definitions */
    fun getProviders(): dynamic // JsObject { name: ProviderDefinition }

    /** Gets the Forwarder instance for managing proxies */
    fun getForwarder(): Forwarder

    /** Closes the agent's listening sockets */
    fun close(callback: ((socketInfo: dynamic) -> Unit)? = definedExternally): Unit

    companion object {
        /** Creates and starts an Agent */
        fun create(
            options: AgentOptions,
            callback: AgentCallback? = definedExternally,
            mib: Mib? = definedExternally
        ): Agent
    }
}

// --- Forwarder ---
/** Definition for an SNMPv3 proxy target */
external interface ProxyDefinition {
    var context: String // The context name that triggers this proxy
    var target: String // The IP address or hostname of the proxied agent
    var user: User // The SNMPv3 user credentials to use for the proxied session
    var port: Int? get() = definedExternally // Port of the proxied agent (default 161)
    var transport: String? get() = definedExternally // Transport for the proxied session (default 'udp4')

    // Internal properties added by Forwarder:
    var session: Session? get() = definedExternally
    var listener: Listener? get() = definedExternally
}

/** Manages proxy definitions for forwarding SNMP requests based on context */
external class Forwarder(listener: Listener, callback: AgentCallback) {
    /** Adds and initializes a proxy definition */
    fun addProxy(proxy: ProxyDefinition): Unit

    /** Deletes a proxy definition by context name */
    fun deleteProxy(proxyName: String): Unit

    /** Gets a proxy definition by context name */
    fun getProxy(proxyName: String): ProxyDefinition?

    /** Gets all configured proxy definitions */
    fun getProxies(): dynamic // JsObject { context: ProxyDefinition }

    /** Dumps proxy configuration to the console */
    fun dumpProxies(): Unit
}

// --- AgentX PDU ---
/** Represents an AgentX Varbind */
external interface AgentXVarbind {
    var type: Int // ObjectType enum value
    var oid: String? // AgentX allows null OID
    var value: Any?
}

/** Represents a search range in AgentX Get/GetNext/GetBulk */
external interface AgentXSearchRange {
    var start: String?
    var end: String?
}

/** Options for creating an AgentXPdu */
external interface AgentXPduOptions {
    var flags: Int? get() = definedExternally // Bitmask, usually includes NETWORK_BYTE_ORDER
    var pduType: Int? get() = definedExternally // AgentXPduType enum value
    var sessionID: Int? get() = definedExternally
    var transactionID: Int? get() = definedExternally
    var packetID: Int? get() = definedExternally

    // PDU specific fields
    var timeout: Int? get() = definedExternally // For Open PDU
    var oid: String? get() = definedExternally // For Open, Register, Unregister, Add/RemoveAgentCaps
    var descr: String? get() = definedExternally // For Open, AddAgentCaps
    var priority: Int? get() = definedExternally // For Register, Unregister
    var rangeSubid: Int? get() = definedExternally // For Register, Unregister
    var varbinds: Array<AgentXVarbind>? get() = definedExternally // For Notify, TestSet, Response
    var sysUpTime: Int? get() = definedExternally // For Response
    var error: Int? get() = definedExternally // For Response (AgentX error codes)
    var index: Int? get() = definedExternally // For Response
    var nonRepeaters: Int? get() = definedExternally // For GetBulk
    var maxRepetitions: Int? get() = definedExternally // For GetBulk
    var searchRangeList: Array<AgentXSearchRange>? get() = definedExternally // For Get, GetNext, GetBulk
}

/** Represents an AgentX PDU */
external class AgentXPdu {
    var version: Int
    var pduType: Int // AgentXPduType enum value
    var flags: Int
    var sessionID: Int
    var transactionID: Int
    var packetID: Int
    var payloadLength: Int

    // PDU specific fields are populated based on pduType during creation/parsing
    var timeout: Int?
    var oid: String?
    var descr: String?
    var priority: Int?
    var rangeSubid: Int?
    var varbinds: Array<AgentXVarbind>?
    var sysUpTime: Int?
    var error: Int? // AgentX error status (see RFC 2741)
    var index: Int?
    var nonRepeaters: Int?
    var maxRepetitions: Int?
    var searchRangeList: Array<AgentXSearchRange>?

    /** Encodes the PDU to a Buffer */
    fun toBuffer(): Buffer

    /** Creates a Response PDU corresponding to this request PDU */
    fun getResponsePduForRequest(): AgentXPdu

    companion object {
        /** Creates an AgentXPdu instance */
        fun createFromVariables(vars: AgentXPduOptions): AgentXPdu

        /** Decodes an AgentXPdu from a Buffer */
        fun createFromBuffer(socketBuffer: Buffer): AgentXPdu
    }
}

// --- Subagent ---
/** Options for creating an AgentX Subagent */
external interface SubagentOptions {
    var master: String? get() = definedExternally // Master agent hostname/IP (default 'localhost')
    var masterPort: Int? get() = definedExternally // Master agent port (default 705)
    var timeout: Int? get() = definedExternally // Timeout for Open PDU (default 0)
    var description: String? get() = definedExternally // Subagent description for Open PDU
    var oid: String? get() = definedExternally // OID for Open PDU
    var debug: Boolean? get() = definedExternally
}

/** Callback function type for Subagent operations */
typealias AgentXCallback = (error: JsError?, pdu: AgentXPdu?) -> Unit

/** Implements an AgentX Subagent client */
external class Subagent(options: SubagentOptions) : EventEmitter {
    val mib: Mib
    val master: String
    val masterPort: Int
    val timeout: Int // Open PDU timeout
    val descr: String
    val sessionID: Int // Populated after successful Open
    val socket: NetSocket // The underlying TCP socket

    /** Gets the internal MIB instance */
    fun getMib(): Mib

    /** Connects the TCP socket to the master agent */
    fun connectSocket(): Unit // Called automatically by Subagent.create

    /** Sends an Open PDU to establish a session */
    fun open(callback: AgentXCallback? = definedExternally): Unit

    /** Sends a Close PDU to terminate the session */
    fun close(callback: AgentXCallback? = definedExternally): Unit

    /** Registers a MIB region (provider) with the master agent */
    fun registerProvider(provider: ProviderDefinition, callback: AgentXCallback? = definedExternally): Unit

    /** Unregisters a MIB region (provider) from the master agent */
    fun unregisterProvider(name: String, callback: AgentXCallback? = definedExternally): Unit

    /** Registers multiple MIB providers */
    fun registerProviders(providers: Array<ProviderDefinition>, callback: AgentXCallback? = definedExternally): Unit

    /** Gets a provider definition registered with the internal MIB */
    fun getProvider(name: String): ProviderDefinition?

    /** Gets all provider definitions registered with the internal MIB */
    fun getProviders(): dynamic // JsObject { name: ProviderDefinition }

    /** Sends an AddAgentCaps PDU */
    fun addAgentCaps(oid: String, descr: String, callback: AgentXCallback? = definedExternally): Unit

    /** Sends a RemoveAgentCaps PDU */
    fun removeAgentCaps(oid: String, callback: AgentXCallback? = definedExternally): Unit

    /** Sends a Notify PDU */
    fun notify(
        typeOrOid: dynamic /* Int | String */,
        varbinds: Array<Varbind>? = definedExternally,
        callback: AgentXCallback? = definedExternally
    ): Unit

    /** Sends a Ping PDU */
    fun ping(callback: AgentXCallback? = definedExternally): Unit

    companion object {
        /** Creates and connects a Subagent */
        fun create(options: SubagentOptions): Subagent
    }
}

// --- Top-level functions ---

/** Creates an SNMP v1/v2c session */
external fun createSession(
    target: String? = definedExternally,
    community: String? = definedExternally,
    options: SessionOptions? = definedExternally
): Session

/** Creates an SNMP v3 session */
external fun createV3Session(
    target: String? = definedExternally,
    user: User,
    options: SessionOptions? = definedExternally
): Session

/** Creates and starts a Receiver */
external fun createReceiver(options: ReceiverOptions, callback: ReceiverCallback): Receiver

/** Creates and starts an Agent */
external fun createAgent(
    options: AgentOptions,
    callback: AgentCallback? = definedExternally,
    mib: Mib? = definedExternally
): Agent

/** Creates a new ModuleStore with base MIBs loaded */
external fun createModuleStore(): ModuleStore

/** Creates and connects a Subagent */
external fun createSubagent(options: SubagentOptions): Subagent

/** Creates a new MIB instance */
external fun createMib(options: MibOptions? = definedExternally): Mib

/** Checks if a varbind represents an SNMP error condition */
external fun isVarbindError(varbind: Varbind): Boolean

/** Returns a string representation of a varbind error */
external fun varbindError(varbind: Varbind): String

// --- Utility Objects (Potentially Internal but Exported) ---

/** Utility functions for validating and casting MIB values */
external object ObjectTypeUtil {
    fun castSetValue(type: Int, value: Any, constraints: MibConstraints? = definedExternally): Any
    fun isValid(type: Int, value: Any, constraints: MibConstraints? = definedExternally): Boolean
    fun doesIntegerMeetConstraints(value: Int, constraints: MibConstraints): Boolean
    fun doesStringMeetConstraints(value: dynamic /* String | Buffer */, constraints: MibConstraints): Boolean
    fun getEnumerationNumberFromName(enumeration: dynamic, name: String): Int?
}

/** Utility functions for SNMPv3 Authentication */
external object Authentication {
    /** Derives an authentication key from a password and engine ID */
    fun passwordToKey(authProtocol: Int /* AuthProtocols code */, authPasswordString: String, engineID: Buffer): Buffer

    /** Gets the expected length of the authentication parameters for a protocol */
    fun getParametersLength(authProtocol: Int /* AuthProtocols code */): Int

    /** Calculates and writes the authentication digest into the message buffer */
    fun writeParameters(
        messageBuffer: Buffer,
        authProtocol: Int /* AuthProtocols code */,
        authPassword: dynamic /* String | Buffer */,
        engineID: Buffer,
        digestInMessage: Buffer
    ): Unit

    /** Verifies the authenticity of a received message */
    fun isAuthentic(
        messageBuffer: Buffer,
        authProtocol: Int /* AuthProtocols code */,
        authPassword: dynamic /* String | Buffer */,
        engineID: Buffer,
        digestInMessage: Buffer
    ): Boolean

    /** Calculates the authentication digest for a message */
    fun calculateDigest(
        messageBuffer: Buffer,
        authProtocol: Int /* AuthProtocols code */,
        authPassword: dynamic /* String | Buffer */,
        engineID: Buffer
    ): Buffer
}

/** Result of an encryption operation */
external interface EncryptionResult {
    var encryptedPdu: Buffer
    var msgPrivacyParameters: Buffer
}

/** Utility functions for SNMPv3 Privacy (Encryption) */
external object Encryption {
    /** Encrypts a ScopedPDU */
    fun encryptPdu(
        privProtocol: Int /* PrivProtocols code */,
        scopedPdu: Buffer,
        privPassword: dynamic /* String | Buffer */,
        authProtocol: Int /* AuthProtocols code */,
        engine: dynamic /* { engineID: Buffer, engineBoots: Int, engineTime: Int } */
    ): EncryptionResult

    /** Decrypts a ScopedPDU */
    fun decryptPdu(
        privProtocol: Int /* PrivProtocols code */,
        encryptedPdu: Buffer,
        privParameters: Buffer,
        privPassword: dynamic /* String | Buffer */,
        authProtocol: Int /* AuthProtocols code */,
        engine: dynamic /* { engineID: Buffer, engineBoots: Int, engineTime: Int } */
    ): Buffer
}

// Note: Ber and MibParser related types from dependencies 'asn1-ber' and './lib/mib'
// are assumed to be dynamic or require separate type definitions if needed.
