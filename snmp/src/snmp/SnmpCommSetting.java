package snmp;

import java.io.Serializable;

import org.snmp4j.Target;

import jp.co.sharp.personalityModule.SnmpSetting;

/**
 * SNMP条件を保持するクラスのinterface
 *
 * @author SBC(SSL)
 *
 */
public interface SnmpCommSetting extends Serializable, Cloneable {

	public Target getTarget();
	public int getVersion();
	public void setAddress(String address);
	public SnmpSetting getSnmpSetting();
}
