package practicalfilament.material

import practicalfilament.core.Float4

/**
 * Generates Filament .mat source definitions as described in Google Filament documentation
 * and Nicole Terc''s Practical Filament talk.
 */
class MaterialBuilder(val name: String) {
    var shadingModel: ShadingModel = ShadingModel.LIT
    var blendingMode: BlendingMode = BlendingMode.OPAQUE
    var doubleSided: Boolean = false
    var hasCustomVertexShader: Boolean = false

    private val parameters = mutableListOf<MaterialParameterDefinition>()
    private var customMaterialShader: String = ""
    private var customVertexShader: String = ""

    fun parameter(name: String, type: String, defaultValue: MaterialParameterValue? = null): MaterialBuilder {
        parameters.add(MaterialParameterDefinition(name, type, defaultValue))
        return this
    }

    fun floatParameter(name: String, default: Float): MaterialBuilder {
        return parameter(name, "float", MaterialParameterValue.FloatValue(default))
    }

    fun float4Parameter(name: String, default: Float4): MaterialBuilder {
        return parameter(name, "float4", MaterialParameterValue.Float4Value(default))
    }

    fun materialShader(source: String): MaterialBuilder {
        this.customMaterialShader = source
        return this
    }

    fun vertexShader(source: String): MaterialBuilder {
        this.customVertexShader = source
        this.hasCustomVertexShader = true
        return this
    }

    /**
     * Builds the complete Filament material text specification (.mat format)
     */
    fun buildFilamentMatText(): String {
        val sb = StringBuilder()
        sb.appendLine("material {")
        sb.appendLine("    name : \"$name\",")
        sb.appendLine("    shadingModel : ${shadingModel.name.lowercase()},")
        sb.appendLine("    blending : ${blendingMode.name.lowercase()},")
        if (doubleSided) {
            sb.appendLine("    doubleSided : true,")
        }
        if (hasCustomVertexShader) {
            sb.appendLine("    vertexDomain : object,")
        }

        if (parameters.isNotEmpty()) {
            sb.appendLine("    parameters : [")
            parameters.forEachIndexed { i, p ->
                val comma = if (i < parameters.lastIndex) "," else ""
                sb.append("        { type : ${p.type}, name : \"${p.name}\" }")
                sb.appendLine(comma)
            }
            sb.appendLine("    ],")
        }

        sb.appendLine("    requires : [")
        sb.appendLine("        position,")
        sb.appendLine("        tangents,")
        sb.appendLine("        uv0")
        sb.appendLine("    ]")
        sb.appendLine("}")
        sb.appendLine()

        if (hasCustomVertexShader && customVertexShader.isNotBlank()) {
            sb.appendLine("vertex {")
            sb.appendLine("    void materialVertex(inout MaterialVertexInputs material) {")
            customVertexShader.lines().forEach { line ->
                sb.appendLine("        $line")
            }
            sb.appendLine("    }")
            sb.appendLine("}")
            sb.appendLine()
        }

        sb.appendLine("fragment {")
        sb.appendLine("    void material(inout MaterialInputs material) {")
        sb.appendLine("        prepareMaterial(material);")
        if (customMaterialShader.isNotBlank()) {
            customMaterialShader.lines().forEach { line ->
                sb.appendLine("        $line")
            }
        } else {
            // Default lit material body
            sb.appendLine("        material.baseColor = materialParams.baseColor;")
            sb.appendLine("        material.roughness = materialParams.roughness;")
            sb.appendLine("        material.metallic = materialParams.metallic;")
            sb.appendLine("        material.reflectance = materialParams.reflectance;")
        }
        sb.appendLine("    }")
        sb.appendLine("}")

        return sb.toString()
    }
}
