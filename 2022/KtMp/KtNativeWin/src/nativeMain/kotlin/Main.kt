import kotlinx.cinterop.*
import platform.windows.*

fun main() {
    println("Windows Service Management Tool")
    installWindowsService()
}

fun installWindowsService() = memScoped {
    if (OpenSCManager == null) {
        println("Error: OpenSCManager() API not found")
        return@memScoped
    }

    // https://docs.microsoft.com/en-us/windows/win32/api/winsvc/nf-winsvc-openscmanagera
    val schSCManager = (OpenSCManager!!)(
        null,                    //   [in, optional] LPCSTR lpMachineName
        null,                    //   [in, optional] LPCSTR lpDatabaseName
        SC_MANAGER_ALL_ACCESS.toUInt() /*full access rights*/ //   [in]           DWORD  dwDesiredAccess
    )

    if (schSCManager == null) {
        val rc = GetLastError().toInt()
        val rmsgc = when (rc) {
            ERROR_ACCESS_DENIED -> "ERROR_ACCESS_DENIED"
            ERROR_DATABASE_DOES_NOT_EXIST -> "ERROR_DATABASE_DOES_NOT_EXIST"
            else -> "$rc"
        }
        print("Error: $rmsgc")
        return@memScoped
    }
    CloseServiceHandle(schSCManager)
}