package practicalfilament.demo

import practicalfilament.core.CameraConfig
import practicalfilament.core.Float3
import practicalfilament.core.Float4
import practicalfilament.core.PracticalFilamentEngine
import practicalfilament.geometry.MeshGenerators
import practicalfilament.light.LightType
import practicalfilament.material.Material
import practicalfilament.material.MaterialBuilder
import practicalfilament.material.PbrParameters

object FilamentDemo {

    /**
     * Demo 1: Redball / Marble (Nicole Terc's primary reference screen)
     * Demonstrates PBR materials (roughness, metallic, clearCoat) with Filament directional lighting.
     */
    fun runMarbleDemo(rasterizer: ConsoleRasterizer, frames: Int = 12): List<String> {
        val engine = PracticalFilamentEngine()
        engine.initialize()

        // Setup material: Red Plastic with high clearCoat
        val redPlastic = engine.createMaterial(
            "RedPlastic",
            PbrParameters(
                baseColor = Float4(0.95f, 0.1f, 0.1f, 1.0f),
                roughness = 0.25f,
                metallic = 0.0f,
                clearCoat = 1.0f,
                clearCoatRoughness = 0.1f
            )
        )

        val sphereHandle = engine.createSphere(redPlastic, radius = 1.2f)
        engine.updateCamera(CameraConfig(position = Float3(0f, 0f, 3.5f), lookAt = Float3.ZERO))

        val outputFrames = mutableListOf<String>()
        for (f in 0 until frames) {
            val angle = f * (360f / frames)
            engine.setRenderableRotation(sphereHandle, xDeg = 15f, yDeg = angle)
            outputFrames.add(rasterizer.render(engine.defaultView))
        }

        engine.destroy()
        return outputFrames
    }

    /**
     * Demo 2: Lit Cube (Nicole Terc's LitCubeScreen)
     * Demonstrates directional light direction gotcha and multi-face shading.
     */
    fun runLitCubeDemo(rasterizer: ConsoleRasterizer, frames: Int = 12): List<String> {
        val engine = PracticalFilamentEngine()
        engine.initialize()

        val goldMat = Material.createGold(1)
        val cubeHandle = engine.createCube(goldMat, size = 1.6f)

        engine.updateCamera(CameraConfig(position = Float3(0f, 1.0f, 3.8f), lookAt = Float3.ZERO))

        val outputFrames = mutableListOf<String>()
        for (f in 0 until frames) {
            val angle = f * (360f / frames)
            engine.setRenderableRotation(cubeHandle, xDeg = 25f, yDeg = angle, zDeg = 10f)
            outputFrames.add(rasterizer.render(engine.defaultView))
        }

        engine.destroy()
        return outputFrames
    }

    /**
     * Demo 3: Hello Triangle (Nicole Terc's HelloTriangleScreen)
     * Demonstrates custom geometry with generated Filament tangent quaternion.
     */
    fun runTriangleDemo(rasterizer: ConsoleRasterizer, frames: Int = 8): List<String> {
        val engine = PracticalFilamentEngine()
        engine.initialize()

        val mat = Material.createDefaultLit(1, "TriangleMat")
        val triHandle = engine.createTriangle(mat)

        val outputFrames = mutableListOf<String>()
        for (f in 0 until frames) {
            val angle = f * (360f / frames)
            engine.setRenderableRotation(triHandle, xDeg = 0f, yDeg = angle)
            outputFrames.add(rasterizer.render(engine.defaultView))
        }

        engine.destroy()
        return outputFrames
    }

    /**
     * Demo 4: Material Builder (.mat code generation as in MaterialBuilderScreen)
     */
    fun generateFilamentMaterialSample(): String {
        return MaterialBuilder("CustomGlowWater")
            .float4Parameter("baseColor", Float4(0.1f, 0.4f, 0.8f, 0.7f))
            .floatParameter("roughness", 0.05f)
            .floatParameter("metallic", 0.0f)
            .floatParameter("waveSpeed", 1.5f)
            .vertexShader("""
                // Animate vertices based on wave parameters
                float wave = sin(getPosition().x * 4.0 + getTime() * 1.5) * 0.1;
                material.worldPosition.y += wave;
            """.trimIndent())
            .materialShader("""
                // Fragment material shader
                material.baseColor = materialParams.baseColor;
                material.roughness = materialParams.roughness;
                material.metallic = materialParams.metallic;
                material.reflectance = 0.5;
                material.clearCoat = 1.0;
            """.trimIndent())
            .buildFilamentMatText()
    }
}
