package sample

import kotlinx.cinterop.*
import platform.windows.*

fun enumService() = memScoped {
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

    val serviceState = SERVICE_ACTIVE or SERVICE_INACTIVE
    val bytesNeeded = alloc<DWORDVar>()
    val servicesReturned = alloc<DWORDVar>()
    val resumeHandle = alloc<DWORDVar> { value = 0u }

    EnumServicesStatusW(
        hSCManager,
        SERVICE_WIN32_OWN_PROCESS.toUInt(),
        serviceState.toUInt(),
        null,
        0,
        bytesNeeded.ptr,
        servicesReturned.ptr,
        null,
    )

    // unionの使用
    // https://kotlinlang.org/docs/mapping-struct-union-types-from-c.html#inspect-generated-kotlin-apis-for-a-c-library

//    val cUnion = cValue<Union1> {
//        i = 0x01020304
//    }
//    val i = cUnion.useContents { i }
//    val b = cUnion.useContents { b }
//    val b1 = (3 downTo 0).map { b[it].toString(0x10) }.joinToString(",")
//
//    println("cUnion.i=${i.toString(0x10)}")
//    println("cUnion.b=$b1")

    val bufferSize = bytesNeeded.value.toLong()
    val buffer = allocArray<ByteVar>(bufferSize)
    val services = buffer.reinterpret<ENUM_SERVICE_STATUSW>()
    resumeHandle.value = 0u

    sequence {
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
        while (true) {
            val rc = EnumServicesStatusW(
                hSCManager,
                SERVICE_WIN32_OWN_PROCESS.toUInt(),
                serviceState.toUInt(),
                services[0].ptr,
                bufferSize.toUInt(),
                bytesNeeded.ptr,
                servicesReturned.ptr,
                resumeHandle.ptr,
            )
            println("rc: $rc")
            println("bytesNeeded: ${bytesNeeded.value}")
            println("resumeHandle: ${resumeHandle.value}")
            println("servicesReturned: ${servicesReturned.value}")

            val result = (0 until servicesReturned.value.toInt()).map { services[it] }

            if (rc == 0) {
                val err = GetLastError().toInt()
                val msg = when (err) {
                    ERROR_ACCESS_DENIED -> "ERROR_ACCESS_DENIED"
                    ERROR_INVALID_HANDLE -> "ERROR_INVALID_HANDLE"
                    ERROR_INVALID_PARAMETER -> "ERROR_INVALID_PARAMETER"
                    ERROR_MORE_DATA -> "ERROR_MORE_DATA"
                    else -> "UNK($err)"
                }
                println("LastError: $msg")
                if (err == ERROR_MORE_DATA) yield(result)
                else return@sequence
            } else {
                yield(result)
                return@sequence
            }
        }
    }.flatMap { it }.forEachIndexed { i, e ->
        print("$i,")
        print(" ${e.lpServiceName?.toKString()},")
        print(" ${e.lpDisplayName?.toKString()},")
        print(" ${e.ServiceStatus.dwServiceType},")
        print(" ${e.ServiceStatus.dwCheckPoint},")
        print(" ${e.ServiceStatus.dwWaitHint},")
        print(" ${e.ServiceStatus.dwCurrentState},")
        print(" ${e.ServiceStatus.dwControlsAccepted},")
        print(" ${e.ServiceStatus.dwServiceSpecificExitCode},")
        print(" ${e.ServiceStatus.dwWin32ExitCode}")
        println()
    }
    CloseServiceHandle(hSCManager)
}
