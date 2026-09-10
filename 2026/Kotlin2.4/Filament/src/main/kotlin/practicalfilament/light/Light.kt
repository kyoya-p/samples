package practicalfilament.light

import practicalfilament.core.Float3
import practicalfilament.core.Float4

enum class LightType {
    DIRECTIONAL,
    POINT,
    SPOT,
    SUN,
    FOCUSED_SPOT
}

data class Light(
    val id: Int,
    val type: LightType = LightType.DIRECTIONAL,
    var color: Float3 = Float3(1f, 1f, 1f), // Linear RGB
    var intensity: Float = 100_000f, // Lux for directional/sun, Lumens for point/spot
    var position: Float3 = Float3(0f, 0f, 0f),
    /**
     * IMPORTANT (Nicole Terc Talk Gotcha):
     * In Filament, directional light vector points in the direction the light EMITS,
     * NOT toward the light source!
     * E.g. A front-light illuminating from +Z looking towards origin (0,0,0) has direction = (0, 0, -1).
     */
    var direction: Float3 = Float3(0f, 0f, -1f),
    var falloffRadius: Float = 10f,
    var spotInnerAngleDegrees: Float = 15f,
    var spotOuterAngleDegrees: Float = 45f,
    var castShadows: Boolean = true
) {
    init {
        direction = direction.normalized()
    }

    /**
     * Calculates the incoming light direction vector at a specific surface point.
     * Returned vector points TOWARDS the light source for BRDF calculations.
     */
    fun getIncomingLightDirection(surfacePoint: Float3): Float3 {
        return when (type) {
            LightType.DIRECTIONAL, LightType.SUN -> -direction
            LightType.POINT, LightType.SPOT, LightType.FOCUSED_SPOT -> (position - surfacePoint).normalized()
        }
    }

    /**
     * Calculates illuminance / irradiance received at a surface point with a given normal.
     */
    fun calculateIrradiance(surfacePoint: Float3, normal: Float3): Float3 {
        val l = getIncomingLightDirection(surfacePoint)
        val nDotL = maxOf(0f, normal.dot(l))
        if (nDotL <= 0f) return Float3.ZERO

        val attenuation = when (type) {
            LightType.DIRECTIONAL, LightType.SUN -> 1.0f
            LightType.POINT, LightType.SPOT, LightType.FOCUSED_SPOT -> {
                val dist = (position - surfacePoint).length()
                if (dist >= falloffRadius) 0f else (1f - (dist / falloffRadius)).coerceAtLeast(0f)
            }
        }

        val baseIntensity = (intensity / 100_000f) * attenuation * nDotL
        return color * baseIntensity
    }
}
