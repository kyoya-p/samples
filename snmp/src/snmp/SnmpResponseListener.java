package snmp;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import jp.co.sharp.common.snmp.SnmpDefinition.ListenerType;

/**
 * SNMP ResponseListenerの抽象クラス
 * SNMP4jからのResponseEventに対する共通処理を行う
 *
 * @author SBC(SSL)
 *
 */
public abstract class SnmpResponseListener implements ResponseListener {

	private static final Logger log = LoggerFactory.getLogger(SnmpResponseListener.class);

	protected final ListenerType listenerType;

	public SnmpResponseListener(ListenerType listenerType) {
		super();
		this.listenerType = listenerType;
	}

	/**
	 * カスタマイズResponseListener
	 * @param result
	 * @param responseDataList
	 */
	protected abstract void onResponseCustom(int result, String peerAddress, Object userObject, List<SnmpResponseData> responseDataList);


	@Override
	public void onResponse(ResponseEvent event) {

		int result = SnmpDefinition.SNMP_SUCCESS;
		String peerAddress = null;

		List<SnmpResponseData> responseDataList = new ArrayList<SnmpResponseData>();

		PDU resPDU = event.getResponse();
		Object userObject = event.getUserObject();

		if(resPDU != null) {

			// Peer Address取得
			Address peer = event.getPeerAddress();
			if(peer != null) {

				if(peer.isValid() == true) {

					peerAddress = peer.toString();

					// PDUの内容チェック
					boolean isSaveResponse = true;				// trueの場合、レスポンスデータを取得する
					int pduErrStatus = resPDU.getErrorStatus();
					if(pduErrStatus == PDU.noError) {

						/**
						 * SNMPv3の場合、PDUTypeがreportでエラーが通知される。
						 * そのため、PDUのerrorStatusがnoErrorでもreportでのエラーが無いかをチェックする必要がある。
						 */
						if(resPDU.getType() == PDU.REPORT) {

							isSaveResponse = false;
							if(isReportError(resPDU) == true) {
								log.warn("Report error. address[" + peerAddress + "]");
								result = SnmpDefinition.SNMP_SNMPERROR;
							}
						} else {

							VariableBinding var = resPDU.get(0);
							OID currentOid = var.getOid();
							if(var.isException() == true) {

								log.warn(currentOid + " -> " + "has exception syntax![" + var.toString() + "], address[" + peerAddress + "]");
								isSaveResponse = false;
								result = SnmpDefinition.SNMP_SNMPERROR;
							}
						}
					} else if(pduErrStatus == PDU.noSuchName) {

						/**
						 * 元にしたソースには、「GetNextの場合、No Such Nameになっても、正常に取得できている項目もある」と
						 * あったが、テストプログラムで試したところ、GetもGetNextもいずれかのOIDでNo Such Nameとなると
						 * 同時にリクエストした全てのOIDの値が取得で来ていない。
						 */
						StringBuffer sb = new StringBuffer();
						for(VariableBinding vb :resPDU.getVariableBindings()) {
							sb.append(vb.getOid()).append(",");
						}
						log.warn("PDU status:" + resPDU.getErrorStatusText() + ", errorIndex[" + resPDU.getErrorIndex() + "], address[" + peerAddress + "], oids[" + sb.toString() + "]");
						isSaveResponse = false;
						result = SnmpDefinition.SNMP_NOTSUPPORTED;
					} else {

						log.warn("PDU error:" + resPDU.getErrorStatusText() + ", errorIndex[" + resPDU.getErrorIndex() + "], address[" + peerAddress + "]");
						isSaveResponse = false;
						result = SnmpDefinition.SNMP_SNMPERROR;
					}

					// PDUからレスポンスデータを取得
					if(isSaveResponse == true) {

						for(VariableBinding vb : resPDU.getVariableBindings()) {
							responseDataList.add(new SnmpResponseData(vb));
						}
					}
				} else {
					log.warn("Peer Address is invalid. address[" + peer.toString() +"]");
					result = SnmpDefinition.SNMP_INVALIDRESPONSE;
				}
			} else {
				log.warn("Peer Address is null.", event.getError());
				result = SnmpDefinition.SNMP_INVALIDRESPONSE;
			}


			/**
			 * Unicastの場合、応答があった時点でrequestをcancelする。
			 * Broadcastの場合は、応答があってもrequestをcancelせず、
			 * timeoutまで待つ。
			 */
			if(listenerType == ListenerType.Unicast) {
				// SNMPのrequestをcancelする。
				((Snmp) event.getSource()).cancel(event.getRequest(), this);
			}
		} else {

			// SNMPのtimeoutの為、requestをcancelする。
			((Snmp) event.getSource()).cancel(event.getRequest(), this);
			result = SnmpDefinition.SNMP_NORESPONSE;
		}

		/**
		 * カスタマイズ処理の呼出し
		 */
		onResponseCustom(result, peerAddress, userObject, responseDataList);
	}

	/**
	 * report errorをチェック
	 * @param resPDU
	 * @return true:エラーあり／false:エラー無し
	 */
	private boolean isReportError(PDU resPDU) {

		boolean result = true;
		if (resPDU.size() < 1) {
			log.warn("REPORT PDU does not contain a variable binding.");
			return result;
		}

		VariableBinding vb = resPDU.get(0);
		OID oid = vb.getOid();
		if (SnmpConstants.usmStatsUnsupportedSecLevels.equals(oid)) {
			log.warn("REPORT: Unsupported Security Level.");
		} else if (SnmpConstants.usmStatsNotInTimeWindows.equals(oid)) {
			log.warn("REPORT: Message not within time window.");
		} else if (SnmpConstants.usmStatsUnknownUserNames.equals(oid)) {
			log.warn("REPORT: Unknown user name.");
		} else if (SnmpConstants.usmStatsUnknownEngineIDs.equals(oid)) {
			log.warn("REPORT: Unknown engine id.");
		} else if (SnmpConstants.usmStatsWrongDigests.equals(oid)) {
			log.warn("REPORT: Wrong digest.");
		} else if (SnmpConstants.usmStatsDecryptionErrors.equals(oid)) {
			log.warn("REPORT: Decryption error.");
		} else if (SnmpConstants.snmpUnknownSecurityModels.equals(oid)) {
			log.info("REPORT: Unknown security model.");
			result = false;
		} else if (SnmpConstants.snmpInvalidMsgs.equals(oid)) {
			log.info("REPORT: Invalid message.");
			result = false;
		} else if (SnmpConstants.snmpUnknownPDUHandlers.equals(oid)) {
			log.info("REPORT: Unknown PDU handler.");
			result = false;
		} else if (SnmpConstants.snmpUnavailableContexts.equals(oid)) {
			log.info("REPORT: Unavailable context.");
			result = false;
		} else if (SnmpConstants.snmpUnknownContexts.equals(oid)) {
			log.warn("REPORT: Unknown context.");
		} else {
			log.info("REPORT contains unknown OID[" + oid.toString() + "]");
			result = false;
		}

		return result;
	}
}
