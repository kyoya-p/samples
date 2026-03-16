interface SnmpAgent {
    suspend fun start(port: Int = 161)
    suspend fun stop()
    suspend fun setValue(oid: String, value: String)
    suspend fun getValue(oid: String): String?
    suspend fun getNextValue(oid: String): Pair<String, String>?
} 