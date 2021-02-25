import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage

// Paho SDK使用
fun main() {
    // Deviceの初期化の処理
    // 認証略

    val mqtt = MqttClient("server.url", "clientId")

    val listener = object : IMqttMessageListener {
        override fun messageArrived(topic: String?, message: MqttMessage?) {
            TODO("ここでDevice初期化処理")
        }
    }
    mqtt.connect()
    mqtt.subscribe("topic/device/DEV1/response/MSG1", listener)

    val request = """
        device(id:"DEV1") {  //topicからdeviceIdが回収できるならid指定は不要
            schedule
            ipRange
            snmpCred
        }
    """.trimIndent()
    val requestMessage = MqttMessage(request.toByteArray())
    mqtt.publish("topic/device/DEV1/request/MSG1", requestMessage)

    // タイムアウトやエラーを検知した場合、publishをリトライする必要あり
    // コールバック関数との連携が必要
}