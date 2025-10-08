fun main() {
    val format = XML(mySerialModule) {
        // configuration options
        autoPolymorphism = true
    }

}

@Serializable
@XmlSerialName(value = "song")
data class OpenLyricsSong
