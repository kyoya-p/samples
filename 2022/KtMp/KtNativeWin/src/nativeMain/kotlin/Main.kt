import kotlinx.cinterop.*
import platform.windows.*
import sample.enumService

fun main() {
    enumService()
//    installWindowsService()
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