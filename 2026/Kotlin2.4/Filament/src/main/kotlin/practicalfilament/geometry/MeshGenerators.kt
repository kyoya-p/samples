package practicalfilament.geometry

import practicalfilament.core.Float3
import practicalfilament.core.Float4
import practicalfilament.core.Quaternion
import kotlin.math.*

object MeshGenerators {

    /**
     * Creates a single 3D triangle, as in HelloTriangleScreen.
     */
    fun createTriangle(): Mesh {
        val mesh = Mesh()
        mesh.vertices.add(Vertex(position = Float3(-1.0f, -0.8f, 0.0f), color = Float4(1f, 0f, 0f, 1f), uv = Pair(0f, 0f)))
        mesh.vertices.add(Vertex(position = Float3(1.0f, -0.8f, 0.0f), color = Float4(0f, 1f, 0f, 1f), uv = Pair(1f, 0f)))
        mesh.vertices.add(Vertex(position = Float3(0.0f, 0.9f, 0.0f), color = Float4(0f, 0f, 1f, 1f), uv = Pair(0.5f, 1f)))
        mesh.indices.addAll(listOf(0, 1, 2))
        mesh.computeTangents()
        return mesh
    }

    /**
     * Creates a UV Sphere, as in MarbleScreen and RedballScreen.
     */
    fun createSphere(radius: Float = 1.0f, latitudeBands: Int = 16, longitudeBands: Int = 24): Mesh {
        val mesh = Mesh()

        for (lat in 0..latitudeBands) {
            val theta = lat * Math.PI.toFloat() / latitudeBands
            val sinTheta = sin(theta)
            val cosTheta = cos(theta)

            for (lon in 0..longitudeBands) {
                val phi = lon * 2 * Math.PI.toFloat() / longitudeBands
                val sinPhi = sin(phi)
                val cosPhi = cos(phi)

                val x = cosPhi * sinTheta
                val y = cosTheta
                val z = sinPhi * sinTheta
                val u = 1.0f - (lon.toFloat() / longitudeBands)
                val v = 1.0f - (lat.toFloat() / latitudeBands)

                val normal = Float3(x, y, z).normalized()
                val tangent = Float3(-sinPhi, 0f, cosPhi).normalized()
                val tangentQuat = Quaternion.fromNormalAndTangent(normal, tangent)

                mesh.vertices.add(
                    Vertex(
                        position = Float3(x * radius, y * radius, z * radius),
                        normal = normal,
                        tangentQuaternion = tangentQuat,
                        uv = Pair(u, v)
                    )
                )
            }
        }

        for (lat in 0 until latitudeBands) {
            for (lon in 0 until longitudeBands) {
                val first = lat * (longitudeBands + 1) + lon
                val second = first + longitudeBands + 1

                mesh.indices.add(first)
                mesh.indices.add(second)
                mesh.indices.add(first + 1)

                mesh.indices.add(second)
                mesh.indices.add(second + 1)
                mesh.indices.add(first + 1)
            }
        }

        return mesh
    }

    /**
     * Creates a Cube with distinct face normals, as in LitCubeScreen.
     */
    fun createCube(size: Float = 1.0f): Mesh {
        val h = size * 0.5f
        val mesh = Mesh()

        fun addQuad(p0: Float3, p1: Float3, p2: Float3, p3: Float3, normal: Float3, tangent: Float3) {
            val base = mesh.vertices.size
            val tq = Quaternion.fromNormalAndTangent(normal, tangent)
            mesh.vertices.add(Vertex(position = p0, normal = normal, tangentQuaternion = tq, uv = Pair(0f, 0f)))
            mesh.vertices.add(Vertex(position = p1, normal = normal, tangentQuaternion = tq, uv = Pair(1f, 0f)))
            mesh.vertices.add(Vertex(position = p2, normal = normal, tangentQuaternion = tq, uv = Pair(1f, 1f)))
            mesh.vertices.add(Vertex(position = p3, normal = normal, tangentQuaternion = tq, uv = Pair(0f, 1f)))

            mesh.indices.addAll(listOf(base, base + 1, base + 2, base, base + 2, base + 3))
        }

        // Front
        addQuad(Float3(-h, -h,  h), Float3( h, -h,  h), Float3( h,  h,  h), Float3(-h,  h,  h), Float3(0f, 0f, 1f), Float3(1f, 0f, 0f))
        // Back
        addQuad(Float3( h, -h, -h), Float3(-h, -h, -h), Float3(-h,  h, -h), Float3( h,  h, -h), Float3(0f, 0f, -1f), Float3(-1f, 0f, 0f))
        // Top
        addQuad(Float3(-h,  h,  h), Float3( h,  h,  h), Float3( h,  h, -h), Float3(-h,  h, -h), Float3(0f, 1f, 0f), Float3(1f, 0f, 0f))
        // Bottom
        addQuad(Float3(-h, -h, -h), Float3( h, -h, -h), Float3( h, -h,  h), Float3(-h, -h,  h), Float3(0f, -1f, 0f), Float3(1f, 0f, 0f))
        // Right
        addQuad(Float3( h, -h,  h), Float3( h, -h, -h), Float3( h,  h, -h), Float3( h,  h,  h), Float3(1f, 0f, 0f), Float3(0f, 0f, -1f))
        // Left
        addQuad(Float3(-h, -h, -h), Float3(-h, -h,  h), Float3(-h,  h,  h), Float3(-h,  h, -h), Float3(-1f, 0f, 0f), Float3(0f, 0f, 1f))

        return mesh
    }

    /**
     * Creates a Morph Mesh with target positions, as in MorphingScreen.
     */
    class MorphMesh(
        val baseMesh: Mesh,
        val targetShapes: List<List<Float3>>
    ) {
        fun interpolate(weights: FloatArray): Mesh {
            val result = Mesh()
            for (i in baseMesh.vertices.indices) {
                var p = baseMesh.vertices[i].position
                for (targetIdx in targetShapes.indices) {
                    val w = if (targetIdx < weights.size) weights[targetIdx] else 0f
                    val targetP = targetShapes[targetIdx][i]
                    p = p + (targetP - baseMesh.vertices[i].position) * w
                }
                result.vertices.add(baseMesh.vertices[i].copy(position = p))
            }
            result.indices.addAll(baseMesh.indices)
            result.computeTangents()
            return result
        }
    }
}
