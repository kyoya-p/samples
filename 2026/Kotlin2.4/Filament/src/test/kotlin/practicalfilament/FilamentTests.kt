package practicalfilament

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import practicalfilament.core.*
import practicalfilament.geometry.MeshGenerators
import practicalfilament.light.Light
import practicalfilament.light.LightType
import practicalfilament.material.Material
import practicalfilament.material.MaterialBuilder
import practicalfilament.material.PbrParameters
import practicalfilament.material.ShadingModel

class FilamentEngineTest {

    @Test
    fun testEngineLifecycle() {
        val engine = PracticalFilamentEngine()
        assertFalse(engine.isInitialized)

        engine.initialize()
        assertTrue(engine.isInitialized)
        assertEquals(1, engine.scene.getAllLights().size) // Default front light added

        engine.destroy()
        assertFalse(engine.isInitialized)
        assertEquals(0, engine.scene.getAllLights().size)
    }

    @Test
    fun testRenderableCreationAndTransformation() {
        val engine = PracticalFilamentEngine()
        engine.initialize()

        val mat = Material.createGold(1)
        val cubeId = engine.createCube(mat, size = 2.0f)
        assertTrue(cubeId > 0)

        val entity = engine.scene.getRenderable(cubeId)
        assertNotNull(entity)
        assertEquals(24, entity?.mesh?.vertices?.size) // 6 faces * 4 vertices
        assertEquals(36, entity?.mesh?.indices?.size)  // 6 faces * 2 triangles * 3 indices

        engine.setRenderableRotation(cubeId, 45f, 90f)
        assertNotNull(entity?.transform)

        engine.removeRenderable(cubeId)
        assertNull(engine.scene.getRenderable(cubeId))
        engine.destroy()
    }
}

class LightingTest {

    /**
     * Verifies the Nicole Terc talk gotcha:
     * Directional light vectors point in the direction the light emits, not toward the light source.
     * When camera is at +Z and looking at origin, front light has negative Z direction.
     */
    @Test
    fun testDirectionalLightEmissionDirectionGotcha() {
        // Front light: comes from +Z towards origin -> direction is (0, 0, -1)
        val frontLight = Light(
            id = 1,
            type = LightType.DIRECTIONAL,
            direction = Float3(0f, 0f, -1f)
        )

        val surfaceOrigin = Float3(0f, 0f, 0f)
        val frontNormal = Float3(0f, 0f, 1f) // Surface pointing towards +Z (camera)

        // Incoming light direction for BRDF must point TOWARDS the light (+Z)
        val incomingDir = frontLight.getIncomingLightDirection(surfaceOrigin)
        assertEquals(0f, incomingDir.x, 1e-4f)
        assertEquals(0f, incomingDir.y, 1e-4f)
        assertEquals(1f, incomingDir.z, 1e-4f)

        val irradiance = frontLight.calculateIrradiance(surfaceOrigin, frontNormal)
        assertTrue(irradiance.length() > 0.5f, "Front light with -Z direction should illuminate +Z normal surface")

        // In contrast, back light pointing in +Z direction fails to illuminate +Z normal surface
        val backLight = Light(
            id = 2,
            type = LightType.DIRECTIONAL,
            direction = Float3(0f, 0f, 1f)
        )
        val backIrradiance = backLight.calculateIrradiance(surfaceOrigin, frontNormal)
        assertEquals(0f, backIrradiance.length(), 1e-4f, "Light with +Z direction back-lights the surface and gives 0 irradiance")
    }
}

class GeometryTest {

    @Test
    fun testSphereGenerationAndBoundingBox() {
        val sphere = MeshGenerators.createSphere(radius = 1.5f, latitudeBands = 8, longitudeBands = 12)
        assertTrue(sphere.vertices.isNotEmpty())
        assertTrue(sphere.indices.isNotEmpty())

        val bbox = sphere.calculateBoundingBox()
        assertEquals(0f, bbox.center.x, 0.1f)
        assertEquals(0f, bbox.center.y, 0.1f)
        assertEquals(0f, bbox.center.z, 0.1f)
        assertEquals(1.5f, bbox.halfExtent.x, 0.15f)
    }

    @Test
    fun testFilamentTangentSpaceQuaternionEncoding() {
        val normal = Float3(0f, 0f, 1f)
        val tangent = Float3(1f, 0f, 0f)
        val quat = Quaternion.fromNormalAndTangent(normal, tangent)

        val len = kotlin.math.sqrt(quat.x * quat.x + quat.y * quat.y + quat.z * quat.z + quat.w * quat.w)
        assertEquals(1.0f, len, 1e-4f, "Tangent quaternion must be normalized")
    }

    @Test
    fun testMorphMeshInterpolation() {
        val base = MeshGenerators.createCube(1.0f)
        // Target shape: scaled by 2
        val targetShape = base.vertices.map { it.position * 2.0f }
        val morphMesh = MeshGenerators.MorphMesh(base, listOf(targetShape))

        val halfMorphed = morphMesh.interpolate(floatArrayOf(0.5f))
        assertEquals(base.vertices.size, halfMorphed.vertices.size)

        val originalPos = base.vertices[0].position
        val interpolatedPos = halfMorphed.vertices[0].position
        assertEquals(originalPos.x * 1.5f, interpolatedPos.x, 1e-4f)
    }
}

class MaterialTest {

    @Test
    fun testMaterialBuilderSyntax() {
        val builder = MaterialBuilder("TestPbrMat")
            .float4Parameter("baseColor", Float4(1f, 0f, 0f, 1f))
            .floatParameter("roughness", 0.3f)
            .floatParameter("metallic", 0.8f)

        val matText = builder.buildFilamentMatText()

        assertTrue(matText.contains("material {"))
        assertTrue(matText.contains("name : \"TestPbrMat\""))
        assertTrue(matText.contains("shadingModel : lit"))
        assertTrue(matText.contains("requires : ["))
        assertTrue(matText.contains("tangents"))
        assertTrue(matText.contains("fragment {"))
        assertTrue(matText.contains("materialParams.baseColor"))
    }

    @Test
    fun testPresetMaterials() {
        val gold = Material.createGold(1)
        assertEquals(ShadingModel.LIT, gold.shadingModel)
        assertNotNull(gold.getParameter("baseColor"))
        assertNotNull(gold.getParameter("metallic"))

        val glass = Material.createGlass(2)
        assertEquals(practicalfilament.material.BlendingMode.TRANSPARENT, glass.blendingMode)
    }
}
