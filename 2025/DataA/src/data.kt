import kotlinx.coroutines.flow.asFlow
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun main(): Unit = listItem().forEach { println(it) }
fun listItem() = with(SystemFileSystem) {
    val json = Json { ignoreUnknownKeys = true }
    fun String.toRecord() = json.decodeFromString<Record>(this)
    val src = source(Path("C:\\Users\\kyoya\\Desktop\\works\\ag\\agent.log")).buffered()
    generateSequence { src.readLine() }
        .mapNotNull { Regex("StatusReport\\[(.*)]").find(it)?.groupValues?.getOrNull(1) }
        .onEach { println(it) }
        .mapNotNull { runCatching { it.toRecord() }.onFailure { }.getOrNull() }
}
fun dataSet1() = listItem().asFlow()

@Serializable
data class Record(
    val environmentalInfo: EnvironmentalInfo,
//    val srdmUsageInfo: SrdmUsageInfo
)

@Serializable
data class EnvironmentalInfo(val usageInfo: UsageInfo, val systemInfo: SystemInfo)

@Serializable
data class UsageInfo(
    val systemUsage: SystemUsage,
    val vmProcessUsage: VmProcessUsage,
    val heapUsage: HeapUsage,
    val threadUsage: ThreadUsage
)

@Serializable
data class SystemUsage(
    val cpuUsage: Double,
    val cpuLoadAverage: Double,
    val physicalMemoryTotal: Long,
    val physicalMemoryFree: Long,
    val swapSpaceTotal: Long,
    val swapSpaceFree: Long,
    val diskSpaceTotal: Long,
    val diskSpaceFree: Long,
    val diskUsabeSpace: Long
)

@Serializable
data class VmProcessUsage(val cpuUsage: Double, val cpuTime: Long, val uptime: Long, val startTime: Long)

@Serializable
data class HeapUsage(val usage: Long, val init: Long, val commited: Long, val max: Long)

@Serializable
data class ThreadUsage(
    val threadCount: Int,
    val totalStartedThreadCount: Int,
    val peakThreadCount: Int,
    val daemonThreadCount: Int
)

@Serializable
data class SystemInfo(val hwInfo: HwInfo)

@Serializable
data class HwInfo(
    val numberOfProcessor: Int,
    val physicalMemory: Long,
    val virtualMemory: Long,
    val swapSpace: Long,
    val diskSpace: Long
)

@Serializable
data class SrdmUsageInfo(
    val devices: Int,
//    val reportLog: Map<String, String>,
//    val dbStorageSize: Int,
//    val userNumber: Int,
//    val accessCount: AccessCount,
//    val accessBrowserCount: Map<String, String>,
//    val storageFreeSpace: Int,
//    val dbFileSize: Int
)

@Serializable
data class AccessCount(val mib: Map<String, String>, val fss: Map<String, String>, val tco: Map<String, String>)


/*
{"status":"Regular","timeStamp":1747972464577,"agentInfo":{"ipAddress":"192.168.24.76","classifier":"embedded","domainName":"","port":8088,"hostName":""},"softwareUniqueInfo":{"softwareName":"Synappx Manage Agent","softwareIdentificationCode":"","softwareVersion":"1.9.17393"},"environmentalInfo":{"usageInfo":{"systemUsage":{"cpuUsage":0.05788658248795789,"cpuLoadAverage":5.30029296875,"physicalMemoryTotal":3272142848,"physicalMemoryFree":296202240,"swapSpaceTotal":3640647680,"swapSpaceFree":3640647680,"diskSpaceTotal":16729894912,"diskSpaceFree":15600713728,"diskUsabeSpace":14724943872},"vmProcessUsage":{"cpuUsage":5.023683077364719E-4,"cpuTime":160620000000,"uptime":15642893,"startTime":1747956821674},"heapUsage":{"usage":109014336,"init":52428800,"commited":164626432,"max":819986432},"threadUsage":{"threadCount":153,"totalStartedThreadCount":409,"peakThreadCount":163,"daemonThreadCount":133}},"systemInfo":{"osInfo":{"osname":"Linux","architecture":"amd64","version":"5.15.133-rt69-intel-pk-preempt-rt"},"hwInfo":{"numberOfProcessor":4,"physicalMemory":3272142848,"virtualMemory":4695703552,"swapSpace":3640647680,"diskSpace":16729894912},"vmInfo":{"vmName":"OpenJDK 64-Bit Server VM","vmVendor":"Eclipse Adoptium","vmVersion":"17.0.15+6"}}},"srdmUsageInfo":{"devices":0,"reportLog":{},"dbStorageSize":0,"userNumber":0,"accessCount":{"mib":{},"fss":{},"tco":{}},"accessBrowserCount":{},"storageFreeSpace":0,"dbFileSize":0},"instanceInfo":{"country":"USA","organizationName":""}}
 */
