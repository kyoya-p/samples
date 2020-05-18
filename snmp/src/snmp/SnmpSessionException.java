package snmp;

/**
 * SnmpSession例外クラス
 * @author SBC(SSL)
 *
 */
public class SnmpSessionException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = -7413615375096098010L;

	public SnmpSessionException(String message) {
		super(message);
	}

	public SnmpSessionException(Throwable cause) {
		super(cause);
	}

	public SnmpSessionException(String message, Throwable cause) {
		super(message, cause);
	}
}
