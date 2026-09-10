package practicalfilament.geometry

import practicalfilament.core.Float3
import practicalfilament.core.Float4
import practicalfilament.core.Quaternion

data class Vertex(
    var position: Float3 = Float3.ZERO,
    var normal: Float3 = Float3.UP,
    var tangentQuaternion: Quaternion = Quaternion.IDENTITY,
    var uv: Pair<Float, Float> = Pair(0f, 0f),
    var color: Float4 = Float4(1f, 1f, 1f, 1f)
)

data class BoundingBox(
    val center: Float3 = Float3.ZERO,
    val halfExtent: Float3 = Float3.ONE
)

enum class PrimitiveType {
    TRIANGLES,
    LINES,
    POINTS
}

class Mesh(
    val vertices: MutableList<Vertex> = mutableListOf(),
    val indices: MutableList<Int> = mutableListOf(),
    val primitiveType: PrimitiveType = PrimitiveType.TRIANGLES
) {
    fun calculateBoundingBox(): BoundingBox {
        if (vertices.isEmpty()) return BoundingBox()
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (v in vertices) {
            minX = minOf(minX, v.position.x)
            minY = minOf(minY, v.position.y)
            minZ = minOf(minZ, v.position.z)
            maxX = maxOf(maxX, v.position.x)
            maxY = maxOf(maxY, v.position.y)
            maxZ = maxOf(maxZ, v.position.z)
        }

        val center = Float3((minX + maxX) * 0.5f, (minY + maxY) * 0.5f, (minZ + maxZ) * 0.5f)
        val halfExtent = Float3((maxX - minX) * 0.5f, (maxY - minY) * 0.5f, (maxZ - minZ) * 0.5f)
        return BoundingBox(center, halfExtent)
    }

    /**
     * Recomputes Filament tangent space quaternions for all vertices.
     * Uses adjacent triangle geometry to compute normals and tangents.
     */
    fun computeTangents() {
        // Compute flat tangents & normals from triangles
        val accumulatedNormals = Array(vertices.size) { Float3.ZERO }
        val accumulatedTangents = Array(vertices.size) { Float3.ZERO }

        for (i in indices.indices step 3) {
            val i0 = indices[i]
            val i1 = indices[i + 1]
            val i2 = indices[i + 2]

            val v0 = vertices[i0]
            val v1 = vertices[i1]
            val v2 = vertices[i2]

            val edge1 = v1.position - v0.position
            val edge2 = v2.position - v0.position

            val deltaUV1 = Pair(v1.uv.first - v0.uv.first, v1.uv.second - v0.uv.second)
            val deltaUV2 = Pair(v2.uv.first - v0.uv.first, v2.uv.second - v0.uv.second)

            val faceNormal = edge1.cross(edge2).normalized()

            val denom = (deltaUV1.first * deltaUV2.second - deltaUV2.first * deltaUV1.second)
            val f = if (kotlin.math.abs(denom) > 1e-6f) 1.0f / denom else 1.0f

            val tangent = Float3(
                f * (deltaUV2.second * edge1.x - deltaUV1.second * edge2.x),
                f * (deltaUV2.second * edge1.y - deltaUV1.second * edge2.y),
                f * (deltaUV2.second * edge1.z - deltaUV1.second * edge2.z)
            ).normalized()

            accumulatedNormals[i0] = accumulatedNormals[i0] + faceNormal
            accumulatedNormals[i1] = accumulatedNormals[i1] + faceNormal
            accumulatedNormals[i2] = accumulatedNormals[i2] + faceNormal

            accumulatedTangents[i0] = accumulatedTangents[i0] + tangent
            accumulatedTangents[i1] = accumulatedTangents[i1] + tangent
            accumulatedTangents[i2] = accumulatedTangents[i2] + tangent
        }

        for (i in vertices.indices) {
            val n = accumulatedNormals[i].normalized()
            val rawT = accumulatedTangents[i]
            // Gram-Schmidt orthogonalize tangent: t = normalize(t - n * dot(n, t))
            val t = (rawT - n * n.dot(rawT)).normalized()
            vertices[i].normal = n
            vertices[i].tangentQuaternion = Quaternion.fromNormalAndTangent(n, t)
        }
    }
}
