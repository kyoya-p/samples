package snmp;

import java.util.UUID;

import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import jp.co.sharp.common.discovery.handler.DiscoveryDefinition;
import jp.co.sharp.personalityModule.SnmpSetting;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * SNMP条件(SNMPv3)
 *
 * @author SBC(SSL)
 *
 */
@Setter
@ToString
public class SnmpV3CommSetting extends AbstractSnmpCommSetting {

	/**
	 *
	 */
	private static final long serialVersionUID = -7764771782778238284L;

	@Getter
	private String contextName;
	private String userName;
	private OID securityAuthType;
	private String securityAuthKey;
	private OID securityPrivType;
	private String securityPrivKey;

	@Getter
	private final OctetString securityName;			// SNMP4jのUSMテーブルに登録する時のuserName

	public SnmpV3CommSetting() {
		super(SnmpConstants.version3);
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		securityName = new OctetString(uuid);
	}

	@Override
	public Target getTarget() {

		UserTarget target = new UserTarget();

		target.setAddress(address);								// UDPアドレス(IPアドレス/SNMPポート)
		target.setTimeout(timeout);								// タイムアウト時間（ミリ秒）
		target.setRetries(retries);								// リトライ数
		target.setVersion(version);								// SNMPバージョン
		target.setSecurityLevel(SecurityLevel.AUTH_PRIV);		//Security Level
		target.setSecurityName(new OctetString(userName));		// SecurityName(MFPに設定しているユーザ名)

		return target;
	}

	@Override
	public SnmpSetting getSnmpSetting() {

		SnmpSetting snmpSetting = new SnmpSetting();
		snmpSetting.setSnmp(DiscoveryDefinition.SNMP_VER_v3);
		snmpSetting.setCommunity("");
		snmpSetting.setRetry(retries);
		snmpSetting.setTimeout(timeout);

		snmpSetting.setContextName(contextName);
		snmpSetting.setUserName(userName);
		snmpSetting.setSecurityAuthType(getAuthTypeString(securityAuthType));
		snmpSetting.setSecurityAuthKey(securityAuthKey);
		snmpSetting.setSecurityPrivType(getPrivProtocol(securityPrivType));
		snmpSetting.setSecurityPrivKey(securityPrivKey);

		return snmpSetting;
	}

	public UsmUser getUsmUser() {
		UsmUser usmUser = new UsmUser(new OctetString(userName),
				securityAuthType,
				new OctetString(securityAuthKey),
				securityPrivType,
				new OctetString(securityPrivKey));
		return usmUser;
	}

	/**
	 * AuthTypeのOIDから文字列に変換
	 * @param securityAuthProtocol
	 * @return
	 */
	private String getAuthTypeString(OID securityAuthProtocol){

		String authType;
		if(securityAuthProtocol.equals(AuthSHA.ID) == true) {
			authType = DiscoveryDefinition.AUTHTYPE_SHA;
		} else {
			authType = DiscoveryDefinition.AUTHTYPE_MD5;
		}
		return authType;
	}

	/**
	 * PrivTypeの文字列からOIDに変更
	 * @param securityPrivProtocol
	 * @return
	 */
	private String getPrivProtocol(OID securityPrivProtocol){

		String privType;
		if(securityPrivProtocol.equals(PrivAES128.ID) == true) {
			privType = DiscoveryDefinition.PRIVTYPE_AES;
		} else {
			privType = DiscoveryDefinition.PRIVTYPE_DES;
		}
		return privType;
	}
}
