package sample

import kotlinx.cinterop.*
import platform.windows.*

val serviceName = "My Service1"

fun createService() = memScoped {
    val hSCManager = openSCManager()
    if (hSCManager == null) return@memScoped null

    val binaryPathName = getModuleFileName()
    val hService = CreateServiceW(
        hSCManager = hSCManager,
        lpServiceName = serviceName,
        lpDisplayName = serviceName,
        dwDesiredAccess = GENERIC_EXECUTE,
        dwServiceType = SERVICE_WIN32_OWN_PROCESS,
        dwStartType = SERVICE_AUTO_START,
        dwErrorControl = SERVICE_ERROR_NORMAL,
        lpBinaryPathName = binaryPathName,
        lpdwTagId = null,
        lpLoadOrderGroup = null,
        lpDependencies = null,
        lpServiceStartName = null,
        lpPassword = null,
    )
    if (hService == null) {
        val msg = when (val err = GetLastError().toInt()) {
            ERROR_ACCESS_DENIED -> "ERROR_ACCESS_DENIED"
            ERROR_CIRCULAR_DEPENDENCY -> "ERROR_CIRCULAR_DEPENDENCY"
            ERROR_DUPLICATE_SERVICE_NAME -> "ERROR_DUPLICATE_SERVICE_NAME"
            ERROR_INVALID_HANDLE -> "ERROR_INVALID_HANDLE"
            ERROR_INVALID_NAME -> "ERROR_INVALID_NAME"
            ERROR_INVALID_PARAMETER -> "ERROR_INVALID_PARAMETER"
            ERROR_INVALID_SERVICE_ACCOUNT -> "ERROR_INVALID_SERVICE_ACCOUNT"
            ERROR_SERVICE_EXISTS -> "ERROR_SERVICE_EXISTS"
            ERROR_SERVICE_MARKED_FOR_DELETE -> "ERROR_SERVICE_MARKED_FOR_DELETE"
            else -> "Unknown($err)"
        }
        println("LastError=$msg")
    } else {
        CloseServiceHandle(hService)
    }
    CloseServiceHandle(hSCManager)
}

fun createServiceA() = memScoped {
    val hSCManager = openSCManager()
    if (hSCManager == null) return@memScoped null

    val binaryPathName = getModuleFileName()

    val hService = CreateServiceA(
        hSCManager = hSCManager,
        lpServiceName = serviceName,
        lpDisplayName = serviceName,
        dwDesiredAccess = GENERIC_EXECUTE,
        dwServiceType = SERVICE_WIN32_OWN_PROCESS,
        dwStartType = SERVICE_AUTO_START,
        dwErrorControl = SERVICE_ERROR_NORMAL,
        lpBinaryPathName = binaryPathName,
        lpdwTagId = null,
        lpLoadOrderGroup = null,
        lpDependencies = null,
        lpServiceStartName = null,
        lpPassword = null,
    )
    if (hService == null) {
        val msg = when (val err = GetLastError().toInt()) {
            ERROR_ACCESS_DENIED -> "ERROR_ACCESS_DENIED"
            ERROR_CIRCULAR_DEPENDENCY -> "ERROR_CIRCULAR_DEPENDENCY"
            ERROR_DUPLICATE_SERVICE_NAME -> "ERROR_DUPLICATE_SERVICE_NAME"
            ERROR_INVALID_HANDLE -> "ERROR_INVALID_HANDLE"
            ERROR_INVALID_NAME -> "ERROR_INVALID_NAME"
            ERROR_INVALID_PARAMETER -> "ERROR_INVALID_PARAMETER"
            ERROR_INVALID_SERVICE_ACCOUNT -> "ERROR_INVALID_SERVICE_ACCOUNT"
            ERROR_SERVICE_EXISTS -> "ERROR_SERVICE_EXISTS"
            ERROR_SERVICE_MARKED_FOR_DELETE -> "ERROR_SERVICE_MARKED_FOR_DELETE"
            else -> "Unknown($err)"
        }
        println("LastError=$msg")
    } else {
        CloseServiceHandle(hService)
    }
    CloseServiceHandle(hSCManager)
}

fun openSCManager(): SC_HANDLE? {
    val hSCManager = OpenSCManagerW(null, null, SC_MANAGER_ALL_ACCESS)

    if (hSCManager == null) {
        val msg = when (val err = GetLastError().toInt()) {
            ERROR_ACCESS_DENIED -> "ERROR_ACCESS_DENIED"
            ERROR_DATABASE_DOES_NOT_EXIST -> "ERROR_DATABASE_DOES_NOT_EXIST"
            else -> "Unknown($err)"
        }
        println("LastError=$msg")
        return null
    }
    return hSCManager
}

fun getModuleFileName() = memScoped {
    val path = allocArray<WCHARVar>(MAX_PATH + 1)
    val pathLen = GetModuleFileNameW(
        hModule = null,
        lpFilename = path,
        nSize = MAX_PATH.toUInt()
    )
    path[pathLen.toInt()] = 0x00u
    path.toKString()
}

fun getModuleFileNameA() = memScoped {
    val path = allocArray<CHARVar>(MAX_PATH + 1)
    val pathLen = GetModuleFileNameA(
        hModule = null,
        lpFilename = path,
        nSize = MAX_PATH.toUInt()
    )
    path[pathLen.toInt()] = 0x00
    path.toKString()
}

fun openService(): SC_HANDLE? {
    val hSCManager = openSCManager()
    if (hSCManager == null) return null

    val hService = OpenServiceW(hSCManager, serviceName, SC_MANAGER_ALL_ACCESS)
    if (hService == null) {
        val msg = when (val err = GetLastError().toInt()) {
            ERROR_ACCESS_DENIED -> "ERROR_ACCESS_DENIED"
            ERROR_INVALID_HANDLE -> "ERROR_INVALID_HANDLE"
            ERROR_INVALID_NAME -> "ERROR_INVALID_NAME"
            ERROR_SERVICE_DOES_NOT_EXIST -> "ERROR_SERVICE_DOES_NOT_EXIST"
            else -> "Unknown($err)"
        }
        println("LastError=$msg")
    }

    CloseServiceHandle(hSCManager)
    return hService
}

fun deleteService() {
    val hService = openService()
    if (hService == null) return

    if (DeleteService(hService) == 0) {
        val msg = when (val err = GetLastError().toInt()) {
            ERROR_ACCESS_DENIED -> "ERROR_ACCESS_DENIED"
            ERROR_INVALID_HANDLE -> "ERROR_INVALID_HANDLE"
            ERROR_SERVICE_MARKED_FOR_DELETE -> "ERROR_SERVICE_MARKED_FOR_DELETE"
            else -> "Unknown($err)"
        }
        println("LastError=$msg")
    }

    CloseServiceHandle(hService)
}