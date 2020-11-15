package gdvm.agent.mib

import kotlinx.serialization.Serializable

@Serializable
data class GdvmDevice(
    val cluster: String,
    val name: String = "anonymous",
    val password: String = "",
    val confidPath: String? = null,
    val config: DeviceConfig? = null,
)

@Serializable
data class DeviceConfig(
    val schedule: Schedule = Schedule(1),
    val time: Long? = null,
)

@Serializable
data class SnmpAgentDevice(
    val cluster: String,
    val confidPath: String? = null,
    val config: SnmpAgentConfig? = null,
    val time: Long? = 0,
)

@Serializable
data class SnmpAgentConfig(
    val scanAddrSpecs: List<SnmpTarget>,
    val autoRegistration: Boolean,
    val schedule: Schedule = Schedule(1),
    val time: Long? = null,
)

@Serializable
data class SnmpTarget(
    val addr: String,
    val port: Int = 161,
    val credential: Credential = Credential(),
    val retries: Int = 5,
    val interval: Long = 5000,

    val isBroadcast: Boolean = false, // for discovery by broadcast
    val addrRangeEnd: String? = null, // for IP ranged discovery
)

@Serializable
data class Credential(
    val ver: String = "2c",
    val v1commStr: String = "public",
    // v3...
)

val GET: Int get() = -96
val GETNEXT: Int get() = -95
val RESPONSE: Int get() = -94
val SET: Int get() = -93

val sysDescr get() = "1.3.6.1.2.1.1.1"
val sysObjectID get() = "1.3.6.1.2.1.1.2"
val sysName get() = "1.3.6.1.2.1.1.5"
val sysLocation get() = "1.3.6.1.2.1.1.6"

val hrDeviceStatus get() = "1.3.6.1.4.1.11.2.3.9.4.2.3.3.2.1.5"
val hrDeviceDescr get() = "1.3.6.1.2.1.25.3.2.1.3"
val hrPrinterStatus get() = "1.3.6.1.2.1.25.3.5.1.1"
val hrPrinterDetectedErrorState get() = "1.3.6.1.2.1.25.3.5.1.2"
val prtGeneralSerialNumber get() = "1.3.6.1.2.1.43.5.1.1.17"


@Serializable
data class PDU(
    val type: Int = GETNEXT,
    val vbl: List<VB> = listOf(VB("1.3")),
    val errSt: Int = 0,
    val errIdx: Int = 0,
)

val ASN_UNIVERSAL get() = 0x00
val ASN_APPLICATION get() = 0x40
val NULL get() = ASN_UNIVERSAL or 0x05
val OCTETSTRING get() = ASN_UNIVERSAL or 0x04
val IPADDRESS get() = ASN_APPLICATION or 0x00

@Serializable
data class VB(
    val oid: String,
    val stx: Int = NULL,
    val value: String = "",
)

@Serializable
data class Schedule(
    val limit: Int = 1, //　回数は有限に。失敗すると破産するし
    val interval: Long = 0,
)

@Serializable
data class Report(
    val time: Long,
    val deviceId: String,
    val type: String = "agent.mfp.mib",
    val result: Result = Result(),
)

@Serializable
data class Result(
    val detected: List<String> = listOf()
)
