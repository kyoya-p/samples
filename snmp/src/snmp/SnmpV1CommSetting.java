package snmp;

import org.snmp4j.CommunityTarget;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;

import jp.co.sharp.common.discovery.handler.DiscoveryDefinition;
import jp.co.sharp.personalityModule.SnmpSetting;
import lombok.Setter;
import lombok.ToString;

/**
 * SNMP条件(SNMPv1)
 *
 * @author SBC(SSL)
 *
 */
@Setter
@ToString
public class SnmpV1CommSetting extends AbstractSnmpCommSetting {

	/**
	 *
	 */
	private static final long serialVersionUID = 1856256764958848838L;

	private String getCommunity;

	public SnmpV1CommSetting() {
		super(SnmpConstants.version1);
	}

	@Override
	public Target getTarget() {

		CommunityTarget target = new CommunityTarget();

		target.setVersion(version);
		target.setCommunity(new OctetString(getCommunity));
		target.setTimeout(timeout);
		target.setRetries(retries);
		target.setAddress(address);

		return target;
	}

	@Override
	public SnmpSetting getSnmpSetting() {

		SnmpSetting snmpSetting = new SnmpSetting();
		snmpSetting.setSnmp(DiscoveryDefinition.SNMP_VER_v1);
		snmpSetting.setCommunity(getCommunity);
		snmpSetting.setRetry(retries);
		snmpSetting.setTimeout(timeout);

		snmpSetting.setContextName("");
		snmpSetting.setUserName("");
		snmpSetting.setSecurityAuthType("");
		snmpSetting.setSecurityAuthKey("");
		snmpSetting.setSecurityPrivType("");
		snmpSetting.setSecurityPrivKey("");

		return snmpSetting;
	}

	@Override
	public SnmpV1CommSetting clone() throws CloneNotSupportedException {

		SnmpV1CommSetting commSetting = null;
		commSetting = (SnmpV1CommSetting)super.clone();

		return commSetting;
	}


}
