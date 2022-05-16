import jp.wjg.shokkaa.snmp4jutils.sampleMibList
import jp.wjg.shokkaa.snmp4jutils.snmpAgent
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class AgentKtTest {
    @Test
    fun snmpAgentTest() = runBlocking {
        snmpAgent(sampleMibList)
    }
}