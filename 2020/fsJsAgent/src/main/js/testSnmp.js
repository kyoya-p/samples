var snmp = require('net-snmp');

var oids = ["1.3.6"];

var options = {
    port: 161,
    retries: 5,
    timeout: 5000,
    backoff: 1.0,
    transport: "udp4",
    trapPort: 162,
    version: snmp.Version1,
    backwardsGetNexts: true,
    idBitsSize: 32
};

var session = snmp.createSession ("120.105.129.244", "public",options);

session.getNext(oids, function (error, varbinds) {
    if(error){
       console.error(error);
    }else{
       for (var i = 0; i < varbinds.length; i++)
           if (snmp.isVarbindError (varbinds[i]))
               console.error (snmp.varbindError (varbinds[i]))
           else
               console.log (varbinds[i].oid + " = " + varbinds[i].value);
     }
});

