package practicalfilament.demo

import practicalfilament.core.Float3
import practicalfilament.core.Float4
import practicalfilament.core.Mat4
import practicalfilament.core.View
import practicalfilament.material.MaterialParameterValue
import kotlin.math.*

class ConsoleRasterizer(val width: Int = 64, val height: Int = 28) {
    private val zBuffer = FloatArray(width * height)
    private val charBuffer = CharArray(width * height)
    private val colorBuffer = Array(width * height) { Float3.ZERO }

    // Shading ramps: 12 levels of density
    private val asciiRamp = " .:-=+*#%@"

    fun clear() {
        zBuffer.fill(Float.MAX_VALUE)
        charBuffer.fill(' ')
        colorBuffer.fill(Float3.ZERO)
    }

    fun render(view: View): String {
        clear()

        val aspect = (width.toFloat() / height.toFloat()) * 0.5f // Correct for non-square console characters
        val viewMatrix = view.camera.getViewMatrix()
        val projMatrix = view.camera.getProjectionMatrix(aspect)
        val vpMatrix = projMatrix * viewMatrix

        val lights = view.scene.getAllLights()
        val renderables = view.scene.getAllRenderables()

        for (entity in renderables) {
            if (!entity.isVisible) continue

            val modelMatrix = entity.transform
            val mvp = vpMatrix * modelMatrix
            val mesh = entity.mesh
            val material = entity.material

            // Material properties
            val baseColor = (material.getParameter("baseColor") as? MaterialParameterValue.Float4Value)?.value
                ?: Float4(0.8f, 0.8f, 0.8f, 1f)
            val metallic = (material.getParameter("metallic") as? MaterialParameterValue.FloatValue)?.value ?: 0.0f
            val roughness = (material.getParameter("roughness") as? MaterialParameterValue.FloatValue)?.value ?: 0.5f
            val clearCoat = (material.getParameter("clearCoat") as? MaterialParameterValue.FloatValue)?.value ?: 0.0f

            for (i in mesh.indices.indices step 3) {
                val idx0 = mesh.indices[i]
                val idx1 = mesh.indices[i + 1]
                val idx2 = mesh.indices[i + 2]

                val v0 = mesh.vertices[idx0]
                val v1 = mesh.vertices[idx1]
                val v2 = mesh.vertices[idx2]

                // Transform positions to world space
                val w0 = modelMatrix.transformPoint(v0.position)
                val w1 = modelMatrix.transformPoint(v1.position)
                val w2 = modelMatrix.transformPoint(v2.position)

                // Face normal in world space
                val faceNormal = (w1 - w0).cross(w2 - w0).normalized()

                // Backface culling: check if facing camera
                val toCamera = (view.camera.position - w0).normalized()
                if (faceNormal.dot(toCamera) <= 0.0f) continue

                // Transform to clip space
                val c0 = mvp.transformPoint(v0.position)
                val c1 = mvp.transformPoint(v1.position)
                val c2 = mvp.transformPoint(v2.position)

                // Screen coordinates
                val s0 = toScreen(c0)
                val s1 = toScreen(c1)
                val s2 = toScreen(c2)

                // Rasterize triangle
                rasterizeTriangle(
                    s0, s1, s2,
                    w0, w1, w2,
                    faceNormal,
                    baseColor,
                    metallic,
                    roughness,
                    clearCoat,
                    lights,
                    view.camera.position
                )
            }
        }

        return toConsoleString()
    }

    private fun toScreen(clip: Float3): Float3 {
        val sx = (clip.x + 1f) * 0.5f * width
        val sy = (1f - (clip.y + 1f) * 0.5f) * height
        return Float3(sx, sy, clip.z)
    }

    private fun rasterizeTriangle(
        s0: Float3, s1: Float3, s2: Float3,
        w0: Float3, w1: Float3, w2: Float3,
        normal: Float3,
        baseColor: Float4,
        metallic: Float,
        roughness: Float,
        clearCoat: Float,
        lights: List<practicalfilament.light.Light>,
        cameraPos: Float3
    ) {
        val minX = max(0, min(s0.x, min(s1.x, s2.x)).toInt())
        val maxX = min(width - 1, max(s0.x, max(s1.x, s2.x)).toInt())
        val minY = max(0, min(s0.y, min(s1.y, s2.y)).toInt())
        val maxY = min(height - 1, max(s0.y, max(s1.y, s2.y)).toInt())

        val denom = (s1.y - s2.y) * (s0.x - s2.x) + (s2.x - s1.x) * (s0.y - s2.y)
        if (abs(denom) < 1e-6f) return

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val px = x + 0.5f
                val py = y + 0.5f

                val wA = ((s1.y - s2.y) * (px - s2.x) + (s2.x - s1.x) * (py - s2.y)) / denom
                val wB = ((s2.y - s0.y) * (px - s2.x) + (s0.x - s2.x) * (py - s2.y)) / denom
                val wC = 1f - wA - wB

                if (wA >= 0f && wB >= 0f && wC >= 0f) {
                    val z = wA * s0.z + wB * s1.z + wC * s2.z
                    val pixelIdx = y * width + x

                    if (z < zBuffer[pixelIdx]) {
                        zBuffer[pixelIdx] = z

                        // Interpolate world position
                        val worldPos = w0 * wA + w1 * wB + w2 * wC
                        val viewDir = (cameraPos - worldPos).normalized()

                        // Filament PBR lighting calculation
                        var totalDiffuse = 0.05f // ambient
                        var totalSpecular = 0f

                        for (light in lights) {
                            val lightDir = light.getIncomingLightDirection(worldPos)
                            val nDotL = max(0f, normal.dot(lightDir))

                            if (nDotL > 0f) {
                                totalDiffuse += nDotL * (light.intensity / 100_000f)

                                // Blinn-Phong specular (approximating Filament GGX)
                                val halfVec = (lightDir + viewDir).normalized()
                                val nDotH = max(0f, normal.dot(halfVec))
                                val shininess = (1f - roughness) * 32f + 1f
                                val specPower = nDotH.pow(shininess)

                                val specIntensity = if (metallic > 0.5f) {
                                    specPower * (1.0f + clearCoat * 0.5f)
                                } else {
                                    specPower * (0.2f + clearCoat * 0.8f)
                                }
                                totalSpecular += specIntensity * (light.intensity / 100_000f)
                            }
                        }

                        val luminance = (totalDiffuse * (1f - metallic * 0.5f) + totalSpecular).coerceIn(0f, 1f)
                        val rampIdx = (luminance * (asciiRamp.length - 1)).toInt().coerceIn(0, asciiRamp.length - 1)
                        charBuffer[pixelIdx] = asciiRamp[rampIdx]

                        // Linear RGB color
                        val r = (baseColor.x * totalDiffuse + totalSpecular).coerceIn(0f, 1f)
                        val g = (baseColor.y * totalDiffuse + totalSpecular).coerceIn(0f, 1f)
                        val b = (baseColor.z * totalDiffuse + totalSpecular).coerceIn(0f, 1f)
                        colorBuffer[pixelIdx] = Float3(r, g, b)
                    }
                }
            }
        }
    }

    private fun toConsoleString(): String {
        val sb = StringBuilder()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val ch = charBuffer[idx]
                val c = colorBuffer[idx]
                if (ch != ' ') {
                    // ANSI 24-bit TrueColor format
                    val ir = (c.x * 255).toInt().coerceIn(0, 255)
                    val ig = (c.y * 255).toInt().coerceIn(0, 255)
                    val ib = (c.z * 255).toInt().coerceIn(0, 255)
                    sb.append("\u001B[38;2;${ir};${ig};${ib}m$ch\u001B[0m")
                } else {
                    sb.append(' ')
                }
            }
            sb.appendLine()
        }
        return sb.toString()
    }
}
