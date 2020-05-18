package snmp;

import org.snmp4j.smi.Address;
import org.snmp4j.smi.UdpAddress;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * SNMP条件(SNMP通信用）を保持する抽象クラス
 *
 * @author SBC(SSL)
 *
 */
@Setter
@ToString
public abstract class AbstractSnmpCommSetting implements SnmpCommSetting {

	/**
	 *
	 */
	private static final long serialVersionUID = -825310463435420201L;

	@Getter
	@Setter(AccessLevel.PROTECTED)
	int version;
	int retries;
	long timeout;

	Address address;

	public AbstractSnmpCommSetting(int version) {
		super();
		this.version = version;
	}

	@Override
	public void setAddress(String address) {

		String udpIpAddress = /*"udp:" +*/ address + SnmpDefinition.DEFAULT_PORT; // UDPアドレス(IPアドレス/SNMPポート)
		this.address = new UdpAddress(udpIpAddress);
	}

}
