package practicalfilament.demo

import practicalfilament.core.*
import practicalfilament.geometry.Mesh
import practicalfilament.geometry.MeshGenerators
import practicalfilament.light.Light
import practicalfilament.light.LightType
import practicalfilament.material.Material
import practicalfilament.material.MaterialParameterValue
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.*

class DesktopApp : JFrame("Practical Filament 3D Desktop Viewer") {
    private val renderWidth = 400
    private val renderHeight = 400
    private val image = BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_RGB)
    private val pixelData = (image.raster.dataBuffer as DataBufferInt).data
    private val zBuffer = FloatArray(renderWidth * renderHeight)

    private val engine = PracticalFilamentEngine()
    private var currentModelIndex = 0
    private var currentMaterialIndex = 0

    private val materials = listOf(
        Material.createRedPlastic(1),
        Material.createGold(2),
        Material.createGlass(3)
    )

    private val meshes = listOf(
        Pair("PBR Sphere (Marble/Redball)", MeshGenerators.createSphere(radius = 1.3f, latitudeBands = 24, longitudeBands = 36)),
        Pair("Lit Cube (6 Faces)", MeshGenerators.createCube(size = 1.8f)),
        Pair("Hello Triangle", MeshGenerators.createTriangle())
    )

    private var rotX = 15f
    private var rotY = 25f
    private var lastMouseX = 0
    private var lastMouseY = 0
    private var autoRotate = true

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false

        engine.initialize()
        engine.updateCamera(CameraConfig(position = Float3(0f, 0f, 3.8f), lookAt = Float3.ZERO))

        val canvas = object : JPanel() {
            override fun getPreferredSize(): Dimension = Dimension(renderWidth * 2, renderHeight * 2)

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                // Draw 2x scaled image
                g2d.drawImage(image, 0, 0, width, height, null)

                // Overlay UI text
                g2d.color = java.awt.Color.WHITE
                g2d.font = Font("SansSerif", Font.BOLD, 14)
                val modelName = meshes[currentModelIndex].first
                val matName = materials[currentMaterialIndex].name
                g2d.drawString("Model: $modelName [Space to change]", 16, 28)
                g2d.drawString("Material: $matName [M to change]", 16, 50)
                g2d.drawString("Auto Rotate: ${if (autoRotate) "ON" else "OFF"} [R to toggle]", 16, 72)
                g2d.drawString("Drag mouse to rotate 3D view", 16, 94)
            }
        }

        val mouseHandler = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                lastMouseX = e.x
                lastMouseY = e.y
            }

            override fun mouseDragged(e: MouseEvent) {
                val dx = e.x - lastMouseX
                val dy = e.y - lastMouseY
                rotY += dx * 0.6f
                rotX += dy * 0.6f
                lastMouseX = e.x
                lastMouseY = e.y
                autoRotate = false
                canvas.repaint()
            }
        }

        canvas.addMouseListener(mouseHandler)
        canvas.addMouseMotionListener(mouseHandler)

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_SPACE -> {
                        currentModelIndex = (currentModelIndex + 1) % meshes.size
                    }
                    KeyEvent.VK_M -> {
                        currentMaterialIndex = (currentMaterialIndex + 1) % materials.size
                    }
                    KeyEvent.VK_R -> {
                        autoRotate = !autoRotate
                    }
                }
                canvas.repaint()
            }
        })

        add(canvas)
        pack()
        setLocationRelativeTo(null)

        // 60 FPS animation timer
        val timer = Timer(16) {
            if (autoRotate) {
                rotY += 1.0f
            }
            renderScene()
            canvas.repaint()
        }
        timer.start()
    }

    private fun renderScene() {
        zBuffer.fill(Float.MAX_VALUE)
        pixelData.fill(0x1a1a24) // Dark slate background

        val mesh = meshes[currentModelIndex].second
        val material = materials[currentMaterialIndex]

        val modelMatrix = Mat4.rotationY(rotY) * Mat4.rotationX(rotX)
        val viewMatrix = engine.defaultView.camera.getViewMatrix()
        val projMatrix = engine.defaultView.camera.getProjectionMatrix(1.0f)
        val mvp = projMatrix * viewMatrix * modelMatrix

        val lights = engine.scene.getAllLights()
        val cameraPos = engine.defaultView.camera.position

        // Material properties
        val baseColor = (material.getParameter("baseColor") as? MaterialParameterValue.Float4Value)?.value
            ?: Float4(0.8f, 0.8f, 0.8f, 1f)
        val metallic = (material.getParameter("metallic") as? MaterialParameterValue.FloatValue)?.value ?: 0.0f
        val roughness = (material.getParameter("roughness") as? MaterialParameterValue.FloatValue)?.value ?: 0.4f
        val clearCoat = (material.getParameter("clearCoat") as? MaterialParameterValue.FloatValue)?.value ?: 0.0f

        for (i in mesh.indices.indices step 3) {
            val i0 = mesh.indices[i]
            val i1 = mesh.indices[i + 1]
            val i2 = mesh.indices[i + 2]

            val v0 = mesh.vertices[i0]
            val v1 = mesh.vertices[i1]
            val v2 = mesh.vertices[i2]

            val w0 = modelMatrix.transformPoint(v0.position)
            val w1 = modelMatrix.transformPoint(v1.position)
            val w2 = modelMatrix.transformPoint(v2.position)

            val faceNormal = (w1 - w0).cross(w2 - w0).normalized()
            val toCamera = (cameraPos - w0).normalized()
            if (faceNormal.dot(toCamera) <= 0f) continue

            val c0 = mvp.transformPoint(v0.position)
            val c1 = mvp.transformPoint(v1.position)
            val c2 = mvp.transformPoint(v2.position)

            val s0 = Float3((c0.x + 1f) * 0.5f * renderWidth, (1f - (c0.y + 1f) * 0.5f) * renderHeight, c0.z)
            val s1 = Float3((c1.x + 1f) * 0.5f * renderWidth, (1f - (c1.y + 1f) * 0.5f) * renderHeight, c1.z)
            val s2 = Float3((c2.x + 1f) * 0.5f * renderWidth, (1f - (c2.y + 1f) * 0.5f) * renderHeight, c2.z)

            rasterizeTriangle(s0, s1, s2, w0, w1, w2, faceNormal, baseColor, metallic, roughness, clearCoat, lights, cameraPos)
        }
    }

    private fun rasterizeTriangle(
        s0: Float3, s1: Float3, s2: Float3,
        w0: Float3, w1: Float3, w2: Float3,
        normal: Float3,
        baseColor: Float4,
        metallic: Float,
        roughness: Float,
        clearCoat: Float,
        lights: List<Light>,
        cameraPos: Float3
    ) {
        val minX = max(0, min(s0.x, min(s1.x, s2.x)).toInt())
        val maxX = min(renderWidth - 1, max(s0.x, max(s1.x, s2.x)).toInt())
        val minY = max(0, min(s0.y, min(s1.y, s2.y)).toInt())
        val maxY = min(renderHeight - 1, max(s0.y, max(s1.y, s2.y)).toInt())

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
                    val pixelIdx = y * renderWidth + x

                    if (z < zBuffer[pixelIdx]) {
                        zBuffer[pixelIdx] = z

                        val worldPos = w0 * wA + w1 * wB + w2 * wC
                        val viewDir = (cameraPos - worldPos).normalized()

                        var totalDiffuse = 0.08f // Ambient
                        var totalSpecular = 0f

                        for (light in lights) {
                            val lightDir = light.getIncomingLightDirection(worldPos)
                            val nDotL = max(0f, normal.dot(lightDir))
                            if (nDotL > 0f) {
                                totalDiffuse += nDotL * (light.intensity / 100_000f)

                                val halfVec = (lightDir + viewDir).normalized()
                                val nDotH = max(0f, normal.dot(halfVec))
                                val shininess = (1f - roughness) * 48f + 1f
                                val specPower = nDotH.pow(shininess)

                                val specMultiplier = if (metallic > 0.5f) {
                                    specPower * (1.2f + clearCoat * 0.5f)
                                } else {
                                    specPower * (0.3f + clearCoat * 0.8f)
                                }
                                totalSpecular += specMultiplier * (light.intensity / 100_000f)
                            }
                        }

                        val r = ((baseColor.x * (1f - metallic * 0.4f)) * totalDiffuse + totalSpecular).coerceIn(0f, 1f)
                        val g = ((baseColor.y * (1f - metallic * 0.4f)) * totalDiffuse + totalSpecular).coerceIn(0f, 1f)
                        val b = ((baseColor.z * (1f - metallic * 0.4f)) * totalDiffuse + totalSpecular).coerceIn(0f, 1f)

                        val ir = (r * 255).toInt()
                        val ig = (g * 255).toInt()
                        val ib = (b * 255).toInt()

                        pixelData[pixelIdx] = (ir shl 16) or (ig shl 8) or ib
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                DesktopApp().isVisible = true
            }
        }
    }
}
