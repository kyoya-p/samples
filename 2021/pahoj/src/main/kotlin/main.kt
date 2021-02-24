import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage

fun main() {

    val mqtt = MqttClient("server.url", "clientId")

    val listener = object : IMqttMessageListener {
        override fun messageArrived(topic: String?, message: MqttMessage?) {
            TODO("ここでDevice初期化処理を行う")
        }
    }
    mqtt.connect()
    mqtt.subscribe("topic/device/DEV1/response/MSG1",listener)
    val requestMessage = MqttMessage("Request{....}".toByteArray())
    mqtt.publish("tpoic/device/DEV1/request/MSG1", requestMessage)


}