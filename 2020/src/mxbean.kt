import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory


fun main() {
    val osMx = ManagementFactory.getOperatingSystemMXBean()
    println("osMx.name:" + osMx.name)
    println("osMx.version:" + osMx.version)
    println("osMx.arch:" + osMx.arch)

    val sunOsMx = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
    println("sunOsMx.totalPhysicalMemorySize:" + sunOsMx.totalPhysicalMemorySize)
    println("sunOsMx.committedVirtualMemorySize:" + sunOsMx.committedVirtualMemorySize)
    println("sunOsMx.totalSwapSpaceSize:" + sunOsMx.totalSwapSpaceSize)

    val runtimeMx = ManagementFactory.getRuntimeMXBean()
    println("runtimeMx.vmName:" + runtimeMx.vmName)
    println("runtimeMx.vmVendor:" + runtimeMx.vmVendor)
    println("runtimeMx.vmVersion:" + runtimeMx.vmVersion)

    val thMx = ManagementFactory.getThreadMXBean()
    println("thMx.currentThreadCpuTime:" + thMx.currentThreadCpuTime)
    println("thMx.peakThreadCount:" + thMx.peakThreadCount)
    println("thMx.totalStartedThreadCount:" + thMx.totalStartedThreadCount)

    val memoryMx = ManagementFactory.getMemoryMXBean()
    println("memoryMx.heapMemoryUsage.used:" + memoryMx.heapMemoryUsage.used)
    println("memoryMx.heapMemoryUsage.committed:" + memoryMx.heapMemoryUsage.committed)
    println("memoryMx.heapMemoryUsage.init:" + memoryMx.heapMemoryUsage.init)
    println("memoryMx.heapMemoryUsage.max:" + memoryMx.heapMemoryUsage.max)
}

