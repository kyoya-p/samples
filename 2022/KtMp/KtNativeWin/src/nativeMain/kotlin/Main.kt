import kotlinx.cinterop.*
import platform.windows.*
import sample.enumService

fun main() {
    println("Windows Service Management Tool")
    enumService()
//    installWindowsService()
}

fun enumServiceStatus() = memScoped {
    /* See: https://docs.microsoft.com/en-us/windows/win32/api/winsvc/nf-winsvc-openscmanagerw
    SC_HANDLE OpenSCManagerW(
      [in, optional] LPCWSTR lpMachineName,
      [in, optional] LPCWSTR lpDatabaseName,
      [in]           DWORD   dwDesiredAccess
    );
    */
    val hSCManager = OpenSCManagerW(
        null,
        null,
        SC_MANAGER_ALL_ACCESS.toUInt()
    )

    /* See: https://docs.microsoft.com/en-us/windows/win32/api/winsvc/nf-winsvc-enumservicesstatusw
    BOOL EnumServicesStatusW(
      [in]                SC_HANDLE              hSCManager,
      [in]                DWORD                  dwServiceType,
      [in]                DWORD                  dwServiceState,
      [out, optional]     LPENUM_SERVICE_STATUSW lpServices,
      [in]                DWORD                  cbBufSize,
      [out]               LPDWORD                pcbBytesNeeded,
      [out]               LPDWORD                lpServicesReturned,
      [in, out, optional] LPDWORD                lpResumeHandle
    );
    */
    val bytesNeeded = alloc<DWORDVar>()
    val servicesReturned = alloc<DWORDVar>()
    val serviceState = SERVICE_ACTIVE or SERVICE_INACTIVE
    val rc = EnumServicesStatusW(
        hSCManager,
        SERVICE_WIN32_OWN_PROCESS.toUInt(), //  [in]                DWORD                  dwServiceType,
        serviceState.toUInt(), //  [in]                DWORD                  dwServiceState,
        null, //  [out, optional]     LPENUM_SERVICE_STATUSA lpServices,
        0.toUInt(), //  [in]                DWORD                  cbBufSize,
        bytesNeeded.ptr, //[out]               LPDWORD                pcbBytesNeeded,
        servicesReturned.ptr,  //[out]               LPDWORD                lpServicesReturned,
        null, //[in, out, optional] LPDWORD                lpResumeHandle
    )
    /*
    public val EnumServicesStatus: kotlinx.cinterop.CPointer<kotlinx.cinterop.CFunction<(
        platform.windows.SC_HANDLE? /* = kotlinx.cinterop.CPointer<platform.windows.SC_HANDLE__>? */,
        platform.windows.DWORD /* = kotlin.UInt */,
        platform.windows.DWORD /* = kotlin.UInt */,
        platform.windows.LPENUM_SERVICE_STATUSW? /* = kotlinx.cinterop.CPointer<platform.windows._ENUM_SERVICE_STATUSW>? */,
        platform.windows.DWORD /* = kotlin.UInt */,
        platform.windows.LPDWORD? /* = kotlinx.cinterop.CPointer<platform.windows.DWORDVar /* = kotlinx.cinterop.UIntVarOf<kotlin.UInt> */>? */,
        platform.windows.LPDWORD? /* = kotlinx.cinterop.CPointer<platform.windows.DWORDVar /* = kotlinx.cinterop.UIntVarOf<kotlin.UInt> */>? */,
        platform.windows.LPDWORD? /* = kotlinx.cinterop.CPointer<platform.windows.DWORDVar /* = kotlinx.cinterop.UIntVarOf<kotlin.UInt> */>? */
    ) -> platform.windows.WINBOOL /* = kotlin.Int */>>?
    */
    println("rc: $rc")
    println("bytesNeeded: ${bytesNeeded.value}")
    println("servicesReturned: ${servicesReturned.value}")

    val bufSize = bytesNeeded.value

    val nItem = (bufSize.toInt() + sizeOf<_ENUM_SERVICE_STATUSW>() - 1) / sizeOf<_ENUM_SERVICE_STATUSW>()
    println("nItem=$nItem")
    val services = allocArray<_ENUM_SERVICE_STATUSW>(nItem)
    val rc2 = (EnumServicesStatus!!)(
        hSCManager,
        SERVICE_WIN32_OWN_PROCESS.toUInt(), //  [in]                DWORD                  dwServiceType,
        (SERVICE_ACTIVE or SERVICE_INACTIVE).toUInt(), //  [in]                DWORD                  dwServiceState,
        services.get(0).ptr, //  [out, optional]     LPENUM_SERVICE_STATUSA lpServices,
        bufSize, //  [in]                DWORD                  cbBufSize,
        bytesNeeded.ptr, //[out]               LPDWORD                pcbBytesNeeded,
        servicesReturned.ptr,  //[out]               LPDWORD                lpServicesReturned,
        null, //[in, out, optional] LPDWORD                lpResumeHandle
    )

    println("bytesNeeded: ${bytesNeeded.value}")
    println("servicesReturned: ${servicesReturned.value}")
    if (rc2 != 0) {
        val r = when (val r0 = GetLastError().toInt()) {
            ERROR_ACCESS_DENIED -> "ERROR_ACCESS_DENIED"
            ERROR_INVALID_HANDLE -> "ERROR_INVALID_HANDLE"
            ERROR_INVALID_PARAMETER -> "ERROR_INVALID_PARAMETER"
            ERROR_MORE_DATA -> "ERROR_MORE_DATA"
            else -> "else($r0)"
        }
        println("rc: $r")
    }
    for (i in (0 until servicesReturned.value.toInt())) {
        println("$i: ${services[i].lpServiceName?.toKString()}")
    }
    CloseServiceHandle(hSCManager)
}

fun installWindowsService() = memScoped {
    if (OpenSCManager == null) {
        println("Error: OpenSCManager() API not found")
        return@memScoped
    }
    // https://docs.microsoft.com/en-us/windows/win32/api/winsvc/nf-winsvc-openscmanagera
    val hSCManager = (OpenSCManager!!)(
        null,                    //   [in, optional] LPCSTR lpMachineName
        null,                    //   [in, optional] LPCSTR lpDatabaseName
        SC_MANAGER_ALL_ACCESS.toUInt() /*full access rights*/ //   [in]           DWORD  dwDesiredAccess
    )

    if (hSCManager == null) {
        val rc = GetLastError().toInt()
        val rmsgc = when (rc) {
            ERROR_ACCESS_DENIED -> "ERROR_ACCESS_DENIED"
            ERROR_DATABASE_DOES_NOT_EXIST -> "ERROR_DATABASE_DOES_NOT_EXIST"
            else -> "$rc"
        }
        print("Error: $rmsgc")
        return@memScoped
    }

    // https://docs.microsoft.com/en-us/windows/win32/api/winsvc/nf-winsvc-createservicea
    val serviceName = "KtNativeWin"
    val binaryPathName = """c:\..."""
    (CreateService!!)(
        hSCManager, // SC_HANDLE? /* = CPointer<SC_HANDLE__>? */,
        serviceName.wcstr.ptr, // lpServiceName:LPCWSTR? /* = CPointer<WCHARVar /* = UShortVarOf<UShort> */>? */,
        null, // lpDisplayName:LPCWSTR? /* = CPointer<WCHARVar /* = UShortVarOf<UShort> */>? */,
        GENERIC_EXECUTE.toUInt(), // dwDesiredAccess:DWORD /* = UInt */,
        SERVICE_WIN32_OWN_PROCESS.toUInt(),// dwServiceType:DWORD /* = UInt */,
        SERVICE_AUTO_START.toUInt(), // dwStartType:DWORD /* = UInt */,
        SERVICE_ERROR_NORMAL.toUInt(), // dwErrorControl:DWORD /* = UInt */,
        binaryPathName.wcstr.ptr, // lpBinaryPathName:LPCWSTR? /* = CPointer<WCHARVar /* = UShortVarOf<UShort> */>? */,
        null, // lpLoadOrderGroup:LPCWSTR? /* = CPointer<WCHARVar /* = UShortVarOf<kotlin.UShort> */>? */,
        null, // lpdwTagId:LPDWORD? /* = CPointer<DWORDVar /* = UIntVarOf<UInt> */>? */,
        null, // lpDependencies:LPCWSTR? /* = CPointer<WCHARVar /* = UShortVarOf<UShort> */>? */,
        null,// lpServiceStartName:LPCWSTR? /* = CPointer<WCHARVar /* = UShortVarOf<UShort> */>? */,
        null,// lpPassword:LPCWSTR? /* = CPointer<WCHARVar /* = UShortVarOf<UShort> */>? */,
    )

    CloseServiceHandle(hSCManager)
}