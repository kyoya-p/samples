package snmp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.asn1.BER;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * SNMP responseで受信したデータ
 *
 * @author SBC(SSL)
 *
 */
public class SnmpResponseData implements Serializable {

	private static final Logger log = LoggerFactory.getLogger(SnmpResponseData.class);

	/**
	 *
	 */
	private static final long serialVersionUID = -372954439008119481L;

	private final VariableBinding vb;

	public SnmpResponseData(VariableBinding vb) {
		this.vb = vb;
	}

	@JsonIgnore
	public OID getOid() {
		return vb.getOid();
	}

	@JsonIgnore
	public String getValue() {
		if(vb.getSyntax() == SMIConstants.SYNTAX_NULL) {
			log.warn("Syntax is null oid:" + vb.getOid());
			return SnmpDefinition.RESULT_STR_NOTSUPPORTED;
		}
		return vb.getVariable().toString();
	}

	@JsonIgnore
	public int getSyntax() {
		return vb.getSyntax();
	}

	@JsonIgnore
	public byte[] getByteAray() {

		byte[] byteData = null;

		int syntax =vb.getSyntax();

		// 文字列データの場合
		if(syntax == BER.OCTETSTRING){

			try {
		 		ByteArrayOutputStream array = new ByteArrayOutputStream();
		 		Variable var = vb.getVariable();
				var.encodeBER(array);
				ByteBuffer bb = ByteBuffer.wrap(array.toByteArray());
	 			try(BERInputStream input = new BERInputStream(bb) ){
	 				BER.MutableByte type = new BER.MutableByte();
		 			byteData = BER.decodeString(input, type);

		 			if(log.isDebugEnabled() == true) {
			 			log.debug("retByteData oid:" + vb.getOid());
				 		log.debug("retByteData syntax:" + syntax);
				 		log.debug("retByteData byteData:");
			 			byteLog(byteData);
		 			}
		        }
			} catch (IOException e) {
				log.error("VariableBindingData to byte array convert error.", e);
			}
		}

		return byteData;
	}

	private void byteLog(byte[] bdata){

		for(int i=0; i<bdata.length; ){
			StringBuilder sb = new StringBuilder();
			sb.append("bdata[").append(i).append("]=");
			String str="";
			for(int j=i; j<i+16; j++){
				if(j>=bdata.length){
					break;
				}
				str = "0x" + String.format("%02x", bdata[j])+ " ";
				sb.append(str);
			}
			i=i+16;
			log.debug(sb.toString());
		}
	}
}
