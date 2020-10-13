package mibtool.snmp4jWrapper

import org.snmp4j.PDU
import org.snmp4j.smi.VariableBinding


fun PDU.toPDU() = mibtool.PDU(
        errSt = errorStatus,
        errIdx = errorIndex,
        type = type,
        vbl = variableBindings.map { it.toVB() }
)

fun VariableBinding.toVB() = mibtool.VB(
        oid = oid.toOidString(),
        stx = syntax,
        value = toValueString(),
)
