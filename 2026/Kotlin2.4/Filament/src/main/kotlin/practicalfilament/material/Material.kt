package practicalfilament.material

import practicalfilament.core.Float4

enum class ShadingModel {
    LIT,
    UNLIT,
    SUBSURFACE,
    CLOTH,
    SPECULAR_GLOSSINESS
}

enum class BlendingMode {
    OPAQUE,
    TRANSPARENT,
    FADE,
    ADD,
    MASK
}

sealed class MaterialParameterValue {
    data class FloatValue(val value: Float) : MaterialParameterValue()
    data class Float2Value(val x: Float, val y: Float) : MaterialParameterValue()
    data class Float3Value(val x: Float, val y: Float, val z: Float) : MaterialParameterValue()
    data class Float4Value(val value: Float4) : MaterialParameterValue()
    data class BoolValue(val value: Boolean) : MaterialParameterValue()
    data class TextureValue(val textureHandle: Int, val samplerName: String) : MaterialParameterValue()
}

data class MaterialParameterDefinition(
    val name: String,
    val type: String,
    val defaultValue: MaterialParameterValue? = null
)

data class PbrParameters(
    var baseColor: Float4 = Float4(0.8f, 0.8f, 0.8f, 1.0f),
    var roughness: Float = 0.4f,
    var metallic: Float = 0.0f,
    var reflectance: Float = 0.5f, // 0.5 in Filament maps to 4% reflectance for standard dielectrics
    var clearCoat: Float = 0.0f,
    var clearCoatRoughness: Float = 0.0f,
    var anisotropy: Float = 0.0f,
    var ambientOcclusion: Float = 1.0f,
    var emissive: Float4 = Float4(0f, 0f, 0f, 0f)
) {
    fun copy(): PbrParameters = PbrParameters(
        baseColor = baseColor,
        roughness = roughness,
        metallic = metallic,
        reflectance = reflectance,
        clearCoat = clearCoat,
        clearCoatRoughness = clearCoatRoughness,
        anisotropy = anisotropy,
        ambientOcclusion = ambientOcclusion,
        emissive = emissive
    )
}

open class Material(
    val id: Int,
    val name: String,
    val shadingModel: ShadingModel = ShadingModel.LIT,
    val blendingMode: BlendingMode = BlendingMode.OPAQUE,
    val parameters: MutableMap<String, MaterialParameterValue> = mutableMapOf()
) {
    fun setParameter(name: String, value: Float) {
        parameters[name] = MaterialParameterValue.FloatValue(value)
    }

    fun setParameter(name: String, value: Float4) {
        parameters[name] = MaterialParameterValue.Float4Value(value)
    }

    fun setParameter(name: String, value: MaterialParameterValue) {
        parameters[name] = value
    }

    fun getParameter(name: String): MaterialParameterValue? = parameters[name]

    companion object {
        fun createDefaultLit(id: Int, name: String = "DefaultLit"): Material {
            val mat = Material(id, name, ShadingModel.LIT, BlendingMode.OPAQUE)
            mat.setParameter("baseColor", Float4(0.8f, 0.8f, 0.8f, 1.0f))
            mat.setParameter("roughness", 0.4f)
            mat.setParameter("metallic", 0.0f)
            mat.setParameter("reflectance", 0.5f)
            return mat
        }

        fun createGold(id: Int): Material {
            val mat = Material(id, "Gold", ShadingModel.LIT, BlendingMode.OPAQUE)
            mat.setParameter("baseColor", Float4(1.0f, 0.766f, 0.336f, 1.0f))
            mat.setParameter("metallic", 1.0f)
            mat.setParameter("roughness", 0.15f)
            mat.setParameter("reflectance", 0.5f)
            return mat
        }

        fun createRedPlastic(id: Int): Material {
            val mat = Material(id, "RedPlastic", ShadingModel.LIT, BlendingMode.OPAQUE)
            mat.setParameter("baseColor", Float4(0.9f, 0.05f, 0.05f, 1.0f))
            mat.setParameter("metallic", 0.0f)
            mat.setParameter("roughness", 0.2f)
            mat.setParameter("clearCoat", 1.0f)
            mat.setParameter("clearCoatRoughness", 0.1f)
            return mat
        }

        fun createGlass(id: Int): Material {
            val mat = Material(id, "Glass", ShadingModel.LIT, BlendingMode.TRANSPARENT)
            mat.setParameter("baseColor", Float4(0.95f, 0.95f, 0.98f, 0.2f))
            mat.setParameter("metallic", 0.0f)
            mat.setParameter("roughness", 0.05f)
            mat.setParameter("reflectance", 0.5f)
            mat.setParameter("clearCoat", 1.0f)
            return mat
        }
    }
}
