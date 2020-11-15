/*
 Sample: Js dynamic object to kotlin data class
 */

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class CustomClass(
    val id: Int,
    val name: String,
)

@Serializer(forClass = CustomClass::class)
object CustomClassSerializer: KSerializer<CustomClass>{
    override fun deserialize(decoder: Decoder): CustomClass {
        TODO("Not yet implemented")
    }

    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(encoder: Encoder, value: CustomClass) {
        TODO("Not yet implemented")
    }
}