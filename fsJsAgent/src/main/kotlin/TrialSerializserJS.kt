/*
 Sample: Js dynamic object to kotlin data class
 dynamic Type => Json String => data class

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
