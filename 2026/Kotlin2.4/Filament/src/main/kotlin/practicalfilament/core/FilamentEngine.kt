package practicalfilament.core

import practicalfilament.geometry.Mesh
import practicalfilament.geometry.MeshGenerators
import practicalfilament.light.Light
import practicalfilament.light.LightType
import practicalfilament.material.Material
import practicalfilament.material.MaterialBuilder
import practicalfilament.material.PbrParameters

interface FilamentEngine {
    val isInitialized: Boolean

    fun initialize()
    fun destroy()

    // Scene & View
    val scene: Scene
    val defaultView: View
    fun clearScene()

    // Camera
    fun updateCamera(config: CameraConfig)

    // Lighting
    fun addLight(type: LightType, color: Float3 = Float3.ONE, intensity: Float = 100_000f, direction: Float3 = Float3.FORWARD): Int
    fun removeLight(handle: Int)

    // Materials
    fun createMaterial(name: String, pbr: PbrParameters = PbrParameters()): Material
    fun createMaterialBuilder(name: String): MaterialBuilder

    // Renderables
    fun createTriangle(material: Material): Int
    fun createCube(material: Material, size: Float = 1.0f): Int
    fun createSphere(material: Material, radius: Float = 1.0f): Int
    fun setRenderableRotation(handle: Int, xDeg: Float, yDeg: Float, zDeg: Float = 0f)
    fun setRenderableTransform(handle: Int, transform: Mat4)
    fun removeRenderable(handle: Int)
}

class PracticalFilamentEngine : FilamentEngine {
    override var isInitialized: Boolean = false
        private set

    override val scene: Scene = Scene()
    override val defaultView: View = View(scene = scene)

    private var nextLightId = 1
    private var nextMaterialId = 1

    override fun initialize() {
        isInitialized = true
        defaultView.camera = CameraConfig(position = Float3(0f, 0f, 4f), lookAt = Float3.ZERO)
        // Add default front key light (pointing in -Z emission direction per Filament rules!)
        addLight(
            type = LightType.DIRECTIONAL,
            color = Float3(1f, 1f, 1f),
            intensity = 100_000f,
            direction = Float3(0.3f, -0.5f, -1.0f).normalized()
        )
    }

    override fun destroy() {
        scene.clear()
        isInitialized = false
    }

    override fun clearScene() {
        scene.clear()
    }

    override fun updateCamera(config: CameraConfig) {
        defaultView.camera = config
    }

    override fun addLight(type: LightType, color: Float3, intensity: Float, direction: Float3): Int {
        val id = nextLightId++
        val light = Light(
            id = id,
            type = type,
            color = color,
            intensity = intensity,
            direction = direction
        )
        scene.addLight(light)
        return id
    }

    override fun removeLight(handle: Int) {
        scene.removeLight(handle)
    }

    override fun createMaterial(name: String, pbr: PbrParameters): Material {
        val mat = Material.createDefaultLit(nextMaterialId++, name)
        mat.setParameter("baseColor", pbr.baseColor)
        mat.setParameter("roughness", pbr.roughness)
        mat.setParameter("metallic", pbr.metallic)
        mat.setParameter("reflectance", pbr.reflectance)
        mat.setParameter("clearCoat", pbr.clearCoat)
        return mat
    }

    override fun createMaterialBuilder(name: String): MaterialBuilder {
        return MaterialBuilder(name)
    }

    override fun createTriangle(material: Material): Int {
        val mesh = MeshGenerators.createTriangle()
        return scene.addRenderable(mesh, material).id
    }

    override fun createCube(material: Material, size: Float): Int {
        val mesh = MeshGenerators.createCube(size)
        return scene.addRenderable(mesh, material).id
    }

    override fun createSphere(material: Material, radius: Float): Int {
        val mesh = MeshGenerators.createSphere(radius)
        return scene.addRenderable(mesh, material).id
    }

    override fun setRenderableRotation(handle: Int, xDeg: Float, yDeg: Float, zDeg: Float) {
        scene.getRenderable(handle)?.setRotation(xDeg, yDeg, zDeg)
    }

    override fun setRenderableTransform(handle: Int, transform: Mat4) {
        scene.getRenderable(handle)?.transform = transform
    }

    override fun removeRenderable(handle: Int) {
        scene.removeRenderable(handle)
    }
}
