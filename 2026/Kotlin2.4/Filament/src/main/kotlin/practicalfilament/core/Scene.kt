package practicalfilament.core

import practicalfilament.geometry.Mesh
import practicalfilament.light.Light
import practicalfilament.material.Material

data class RenderableEntity(
    val id: Int,
    var mesh: Mesh,
    var material: Material,
    var transform: Mat4 = Mat4.identity(),
    var isVisible: Boolean = true,
    var castShadows: Boolean = true,
    var receiveShadows: Boolean = true
) {
    fun setRotation(xDeg: Float, yDeg: Float, zDeg: Float = 0f) {
        transform = Mat4.rotationY(yDeg) * Mat4.rotationX(xDeg) * Mat4.rotationZ(zDeg)
    }

    fun setPosition(pos: Float3) {
        transform = Mat4.translation(pos.x, pos.y, pos.z)
    }
}

class Scene {
    private val renderables = mutableMapOf<Int, RenderableEntity>()
    private val lights = mutableMapOf<Int, Light>()
    private var nextEntityId = 1

    fun addRenderable(mesh: Mesh, material: Material): RenderableEntity {
        val id = nextEntityId++
        val entity = RenderableEntity(id, mesh, material)
        renderables[id] = entity
        return entity
    }

    fun getRenderable(id: Int): RenderableEntity? = renderables[id]

    fun removeRenderable(id: Int) {
        renderables.remove(id)
    }

    fun addLight(light: Light): Int {
        lights[light.id] = light
        return light.id
    }

    fun getLight(id: Int): Light? = lights[id]

    fun removeLight(id: Int) {
        lights.remove(id)
    }

    fun getAllRenderables(): List<RenderableEntity> = renderables.values.toList()
    fun getAllLights(): List<Light> = lights.values.toList()

    fun clear() {
        renderables.clear()
        lights.clear()
    }
}

data class Viewport(val x: Int = 0, val y: Int = 0, val width: Int = 80, val height: Int = 40)

class View(
    var scene: Scene = Scene(),
    var camera: CameraConfig = CameraConfig(),
    var viewport: Viewport = Viewport()
)
