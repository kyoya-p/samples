const snmp= require("net-snmp");
const session = snmp.Session.create("192.168.11.5", "public")
session.getNext(["1.3.6"], function (error, varbinds) {
    if (error) {
        console.error(error);
    } else {
        for (var i = 0; i < varbinds.length; i++) {
            if (snmp.isVarbindError(varbinds[i])) {
                console.error(snmp.varbindError(varbinds[i]));
            } else {
                console.log(varbinds[i].oid + " = " + varbinds[i].value);
            }
        }
    }
    session.close();
});
