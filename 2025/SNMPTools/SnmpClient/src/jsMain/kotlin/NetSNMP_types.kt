@file:JsModule("net-snmp")
@file:JsNonModule // for CommonJS Module

package snmp.netsnmp.js

/* Export

exports.Session = Session;

exports.createSession = Session.create;
exports.createV3Session = Session.createV3;

exports.createReceiver = Receiver.create;
exports.createAgent = Agent.create;
exports.createModuleStore = ModuleStore.create;
exports.createSubagent = Subagent.create;
exports.createMib = Mib.create;

exports.isVarbindError = isVarbindError;
exports.varbindError = varbindError;

exports.Version1 = Version1;
exports.Version2c = Version2c;
exports.Version3 = Version3;
exports.Version = Version;

exports.ErrorStatus = ErrorStatus;
exports.TrapType = TrapType;
exports.ObjectType = ObjectType;
exports.PduType = PduType;
exports.AgentXPduType = AgentXPduType;
exports.MibProviderType = MibProviderType;
exports.SecurityLevel = SecurityLevel;
exports.AuthProtocols = AuthProtocols;
exports.PrivProtocols = PrivProtocols;
exports.AccessControlModelType = AccessControlModelType;
exports.AccessLevel = AccessLevel;
exports.MaxAccess = MaxAccess;
exports.RowStatus = RowStatus;
exports.OidFormat = OidFormat;

exports.ResponseInvalidCode = ResponseInvalidCode;
exports.ResponseInvalidError = ResponseInvalidError;
exports.RequestInvalidError = RequestInvalidError;
exports.RequestFailedError = RequestFailedError;
exports.RequestTimedOutError = RequestTimedOutError;

exports.ObjectParser = {
	readInt32: readInt32,
	readUint32: readUint32,
	readVarbindValue: readVarbindValue
};
exports.ObjectTypeUtil = ObjectTypeUtil;
exports.Authentication = Authentication;
exports.Encryption = Encryption;

*/

// 基本的な Error 型
external class Error {
    val message: String
    val stack: String?
}

// Varbind (OID と値のペア)
external class Varbind {
    val oid: String
    val type: Int // netSnmp.ObjectType.* 定数に対応
    val value: Any // 実際の型は様々 (Int, String, Buffer, etc.)
    val valueRaw: Array<UByte> // Buffer の生データ
    val valueHex: String? // Buffer の16進表現
}

// SNMP セッションオプション
external class SessionOptions {
    var port: Int?
    var retries: Int?
    var timeout: Int?
    var transport: String? // "udp4" or "udp6"
    var trapPort: Int?
    var version: Int? // netSnmp.Version1, netSnmp.Version2c, netSnmp.Version3
    var idBitsSize: Int? // version 3 specific
    var context: String? // version 3 specific
}

external fun createSession(address: String, community: String, options: SessionOptions? = definedExternally): Session

external class Session {
    companion object {
        fun create(target: String, community: String, options: SessionOptions? = definedExternally): Session
    }
    fun getNext(oids:Array<String>,responseCb:(error:Error?, varbinds:Array<Varbind>)->Unit )


    // SNMP バージョン定数
    val Version1: Int
    val Version2c: Int
    val Version3: Int

    // Varbind 型定数 (一部)
    object ObjectType {
        val Boolean: Int
        val Integer: Int
        val OctetString: Int
        val Null: Int
        val OID: Int
        val IpAddress: Int
        val Counter: Int
        val Gauge: Int
        val TimeTicks: Int
        val Opaque: Int
        val Counter64: Int
        val NoSuchObject: Int
        val NoSuchInstance: Int
        val EndOfMibView: Int
    }

    // エラーチェックユーティリティ
    fun isVarbindError(varbind: Varbind): Boolean
}


