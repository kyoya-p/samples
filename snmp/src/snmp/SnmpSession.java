package snmp;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.MessageProcessingModel;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.security.UsmUserEntry;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.transport.TransportListener;
import org.snmp4j.transport.UdpTransportMapping;

/**
 * SNMP Sessionの管理クラス
 *
 * @author SBC(SSL)
 *
 */
public class SnmpSession implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(SnmpSession.class);

	Snmp snmp = null;
	UdpTransportMapping utm = null;

	private boolean isGenerated = false;						// SNMP4jのインスタンス生成有無

	static {

		// SNMP4jのログ設定
//		LogFactory.setLogFactory(new JavaLogFactory());
//		LogFactory.getLogFactory().getRootLogger().setLogLevel(LogLevel.DEBUG);

		// Authentication Protocolの設定
		/**
		 * SNMP4j version3.0.0以降、デフォルトで有効なAuthentication ProtocolからMD5とSHA-1が削除されたため追加
		 */
		SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthMD5());
		SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthSHA());

		/**
		 * USMの設定（共通のUSMクラスを登録）
		 * SecurityModelsクラスに登録したUSMは、共通のUSMとして扱われる。
		 * 具体的には、MPv3クラスを生成する時、USM(MessageProcessingModel)をコンストラクタの引数に渡すと、
		 * 個別のSecurityModelsクラスが作成される。引数に指定しない場合、共通のSecurityModelsクラスが使われる。
		 * ここでは、共通のSecurityModelsのクラスにUSMを登録する。
		 * 但し、実際は、SNMPのインスタンス生成時、個別でUSMを作成し、MPv3クラスを生成するため、
		 * 共通のSecurityModelsクラスに登録したUSMは使用されることはない。
		 */
		OctetString localEngineId = new OctetString(MPv3.createLocalEngineID());
		USM usm = new USM(SecurityProtocols.getInstance(),
				localEngineId, 0);
		SecurityModels.getInstance().addSecurityModel(usm);
	}

	/**
	 * コンストラクタ(TransportListener指定なし)
	 */
	public SnmpSession() {
		this(null);
	}

	/**
	 * コンストラクタ(TransportListener指定あり)
	 * @param transportListener
	 */
	public SnmpSession(TransportListener transportListener) {

		log.info("Create SnmpSession.");
		try {
			utm = new DefaultUdpTransportMapping();
			if(transportListener != null) {
				utm.addTransportListener(transportListener);
			}
		} catch (SocketException e) {
			// 通常はありえない。
			log.error("UdpTransportMapping create faild.", e);
		}

		snmp = new Snmp(utm);

		/**
		 * USMを設定（個別のUSMを作成）
		 * MPv3クラスを作成する時、引数にUSMクラスを指定することで個別のUSMクラスを使用することができる。
		 * SRDMでは、一つの検索条件に一つのSNMP条件を持っており、また、一つのSNMPのインスタンスを生成することから、
		 * SNMPのインスタンス毎にUSMを作成することで、
		 * ・同じSecurityName(MFPに設定するユーザ名)でAuthPassやPrivPassが異なる設定。
		 * ・デバイス検出と詳細情報取得の間のMFPの再起動発生。
		 * など、当初SNMPv3対応時に発生した問題を回避できる。
		 */
		byte[] engineId = MPv3.createLocalEngineID();
		log.info("localEngineId(create):" + encodeHex(engineId));

		USM usm = new USM(SecurityProtocols.getInstance(),
				new OctetString(engineId), 0);
		MessageProcessingModel oldModel = snmp.getMessageDispatcher().getMessageProcessingModel(MessageProcessingModel.MPv3);
		if (oldModel != null) {
			snmp.getMessageDispatcher().removeMessageProcessingModel(oldModel);
		}
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv3(usm));

		isGenerated = true;

		// 最終的には、ログ出力を無くす
		if(log.isDebugEnabled() == true) {
			String hexString = encodeHex(snmp.getLocalEngineID());
			log.debug("localEngineId(SNMP):" + hexString);
			log.debug("localEngineId(USM) :" + snmp.getUSM().getLocalEngineID());
		}
	}

	/**
	 * listen開始
	 * （ディスパッチャーの起動、ローカルUDPポートのオープン、待機）
	 * @throws IOException
	 * @throws SnmpSessionException
	 */
	public void snmpListen() throws IOException, SnmpSessionException {

		if(isGenerated == false) {
			// SNMP4jのインスタンス生成が失敗している場合は、例外を返す
			throw new SnmpSessionException("SNMP 4 j is not generated.");
		}
		snmp.listen();
	}

	/**
	 * SNMP Get
	 * @param commSetting
	 * @param oids
	 * @param userHandle
	 * @param listener
	 * @throws IOException
	 * @throws SnmpSessionException
	 */
	public void snmpGet(SnmpCommSetting commSetting, List<OID> oids, Object userHandle, SnmpResponseListener listener) throws IOException, SnmpSessionException {

		PDU pdu;
		Target target;

		if(isGenerated == false) {
			// SNMP4jのインスタンス生成が失敗している場合は、例外を返す
			throw new SnmpSessionException("SNMP 4 j is not generated.");
		}

		if(commSetting.getVersion() == SnmpConstants.version1) {
			pdu = new PDU();
			pdu.setType(PDU.GET);

		} else {
			SnmpV3CommSetting v3CommSetting;
			if(commSetting instanceof SnmpV3CommSetting) {
				v3CommSetting = (SnmpV3CommSetting) commSetting;
			} else {
				log.warn("commSetting is not instance of SnmpV3CommSetting.");
				v3CommSetting = new SnmpV3CommSetting();
			}

			// UsmUserの追加
			snmp.getUSM().addUser(v3CommSetting.getSecurityName(), v3CommSetting.getUsmUser());

			pdu = new ScopedPDU();
			pdu.setType(PDU.GET);
			((ScopedPDU)pdu).setContextName(new OctetString(v3CommSetting.getContextName()));
		}

		for(OID oid : oids) {
			pdu.add(new VariableBinding(oid));
		}

		target = commSetting.getTarget();

		snmpSend(pdu, target, userHandle, listener);
	}

	/**
	 * SNMP GetNext
	 * @param commSetting
	 * @param oids
	 * @param userHandle
	 * @param listener
	 * @throws IOException
	 * @throws SnmpSessionException
	 */
	public void snmpGetNext(SnmpCommSetting commSetting, List<OID> oids, Object userHandle, ResponseListener listener) throws IOException, SnmpSessionException {

		PDU pdu;
		Target target;

		if(isGenerated == false) {
			// SNMP4jのインスタンス生成が失敗している場合は、例外を返す
			throw new SnmpSessionException("SNMP 4 j is not generated.");
		}

		if(commSetting.getVersion() == SnmpConstants.version1) {
			pdu = new PDU();
			pdu.setType(PDU.GETNEXT);

		} else {
			SnmpV3CommSetting v3CommSetting;
			if(commSetting instanceof SnmpV3CommSetting) {
				v3CommSetting = (SnmpV3CommSetting) commSetting;
			} else {
				log.warn("commSetting is not instance of SnmpV3CommSetting.");
				v3CommSetting = new SnmpV3CommSetting();
			}

			// UsmUserの追加
			snmp.getUSM().addUser(v3CommSetting.getSecurityName(), v3CommSetting.getUsmUser());

			pdu = new ScopedPDU();
			pdu.setType(PDU.GETNEXT);
			((ScopedPDU)pdu).setContextName(new OctetString(v3CommSetting.getContextName()));
		}

		for(OID oid : oids) {
			pdu.add(new VariableBinding(oid));
		}

		target = commSetting.getTarget();

		snmpSend(pdu, target, userHandle, listener);
	}

	/**
	 * 非同期メッセージの送信
	 *
	 * @param pdu
	 * @param target
	 * @param userHandle
	 * @param listener
	 * @throws IOException
	 * @throws SnmpSessionException
	 */
	public void snmpSend(PDU pdu, Target target, Object userHandle, ResponseListener listener) throws IOException, SnmpSessionException {

		if(isGenerated == false) {
			// SNMP4jのインスタンス生成が失敗している場合は、例外を返す
			throw new SnmpSessionException("SNMP4j is not generated.");
		}

		log.debug("Send address[" + target.getAddress() + "], timeout[" + target.getTimeout() + "], retries[" + target.getRetries() + "]");
		snmp.send(pdu, target, userHandle, listener);
	}

	/**
	 * SnmpSessionのclose
	 *
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {

		// SNMP4jのclose
		snmp.close();
	}

	/**
	 * USMテーブルにユーザを追加
	 * 登録時のユーザ名は、生成する
	 * @param securityName
	 * @param authPass
	 * @param privPass
	 * @throws SnmpSessionException
	 */
	public void addUser(String securityName, String authPass, String privPass) throws SnmpSessionException {

		if(isGenerated == false) {
			// SNMP4jのインスタンス生成が失敗している場合は、例外を返す
			throw new SnmpSessionException("SNMP 4 j is not generated.");
		}

		String userName = UUID.randomUUID().toString().replaceAll("-", "");
		snmp.getUSM().addUser(new OctetString(userName),
				new UsmUser(new OctetString(securityName),
						AuthMD5.ID,
						new OctetString(authPass),
						PrivDES.ID,
						new OctetString(privPass)));
	}

	/**
	 * 以降は、デバッグ用のメソッド
	 */

	/**
	 * UsmUserTableの内容をログに出力
	 */
	public void printUsmUserTable() {

		log.info("----------------------------------------------------------------------------");
		String hexString = encodeHex(snmp.getLocalEngineID());
		log.info("localEngineId(SNMP):" + hexString);
		log.info("localEngineId(USM) :" + snmp.getUSM().getLocalEngineID());
		for(UsmUserEntry entry : snmp.getUSM().getUserTable().getUserEntries()) {
			log.info("----------------------------------------------------------------------------");
			log.info("engineId      :" + entry.getEngineID());
			log.info("UserName      :" + entry.getUserName());
			log.info("securityName  :" + entry.getUsmUser().getSecurityName());
			log.info("authPass      :" + entry.getUsmUser().getAuthenticationPassphrase());
			log.info("privPass      :" + entry.getUsmUser().getPrivacyPassphrase());
			log.info("----------------------------------------------------------------------------");
		}
	}

	/**
	 * 実行中のリクエスト数をログに出力
	 */
	public void printRequestCount() {

		log.info("PendingAsyncRequestCount:" + snmp.getPendingAsyncRequestCount());
	}

	/**
	 * byte配列 → hex文字列変換
	 * @param data
	 * @return
	 */
	private String encodeHex(byte[] data) {

		StringBuilder sb = new StringBuilder();

		if(data.length != 0) {
			for(byte b : data) {
				sb.append(String.format("%02x:", b));
			}
			return sb.substring(0, sb.length() - 1);
		} else {
			return sb.toString();
		}
	}
}
