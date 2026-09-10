package practicalfilament.core

import kotlin.math.*

data class Float3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun plus(other: Float3) = Float3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Float3) = Float3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Float3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Float3(x / scalar, y / scalar, z / scalar)
    operator fun unaryMinus() = Float3(-x, -y, -z)

    fun dot(other: Float3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Float3) = Float3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Float3 {
        val len = length()
        return if (len > 1e-6f) this / len else Float3(0f, 0f, 0f)
    }

    companion object {
        val ZERO = Float3(0f, 0f, 0f)
        val ONE = Float3(1f, 1f, 1f)
        val UP = Float3(0f, 1f, 0f)
        val FORWARD = Float3(0f, 0f, -1f) // Filament standard look direction
    }
}

data class Float4(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val w: Float = 1f) {
    operator fun times(scalar: Float) = Float4(x * scalar, y * scalar, z * scalar, w * scalar)
}

data class Quaternion(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val w: Float = 1f) {
    fun normalized(): Quaternion {
        val len = sqrt(x * x + y * y + z * z + w * w)
        return if (len > 1e-6f) Quaternion(x / len, y / len, z / len, w / len) else IDENTITY
    }

    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)

        /**
         * Filament tangent space encoding: encodes normal and tangent into a quaternion.
         * The w component sign encodes the bitangent reflection/handedness.
         */
        fun fromNormalAndTangent(normal: Float3, tangent: Float3): Quaternion {
            val n = normal.normalized()
            val t = tangent.normalized()
            val b = n.cross(t).normalized()

            // Construct 3x3 rotation matrix where columns/rows are T, B, N
            // Filament assumes: tangent frame = (tangent, bitangent, normal)
            val trace = t.x + b.y + n.z
            return if (trace > 0f) {
                val s = 0.5f / sqrt(trace + 1.0f)
                Quaternion(
                    (b.z - n.y) * s,
                    (n.x - t.z) * s,
                    (t.y - b.x) * s,
                    0.25f / s
                ).normalized()
            } else {
                if (t.x > b.y && t.x > n.z) {
                    val s = 2.0f * sqrt(1.0f + t.x - b.y - n.z)
                    Quaternion(0.25f * s, (t.y + b.x) / s, (n.x + t.z) / s, (b.z - n.y) / s).normalized()
                } else if (b.y > n.z) {
                    val s = 2.0f * sqrt(1.0f + b.y - t.x - n.z)
                    Quaternion((t.y + b.x) / s, 0.25f * s, (b.z + n.y) / s, (n.x - t.z) / s).normalized()
                } else {
                    val s = 2.0f * sqrt(1.0f + n.z - t.x - b.y)
                    Quaternion((n.x + t.z) / s, (b.z + n.y) / s, 0.25f * s, (t.y - b.x) / s).normalized()
                }
            }
        }
    }
}

class Mat4(val m: FloatArray = FloatArray(16) { if (it % 5 == 0) 1f else 0f }) {
    operator fun get(row: Int, col: Int): Float = m[col * 4 + row] // Column-major order (Filament standard)
    operator fun set(row: Int, col: Int, v: Float) { m[col * 4 + row] = v }

    operator fun times(other: Mat4): Mat4 {
        val result = FloatArray(16)
        for (c in 0..3) {
            for (r in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += this[r, k] * other[k, c]
                }
                result[c * 4 + r] = sum
            }
        }
        return Mat4(result)
    }

    fun transformPoint(p: Float3): Float3 {
        val x = this[0, 0] * p.x + this[0, 1] * p.y + this[0, 2] * p.z + this[0, 3]
        val y = this[1, 0] * p.x + this[1, 1] * p.y + this[1, 2] * p.z + this[1, 3]
        val z = this[2, 0] * p.x + this[2, 1] * p.y + this[2, 2] * p.z + this[2, 3]
        val w = this[3, 0] * p.x + this[3, 1] * p.y + this[3, 2] * p.z + this[3, 3]
        return if (abs(w) > 1e-6f) Float3(x / w, y / w, z / w) else Float3(x, y, z)
    }

    fun transformDirection(d: Float3): Float3 {
        val x = this[0, 0] * d.x + this[0, 1] * d.y + this[0, 2] * d.z
        val y = this[1, 0] * d.x + this[1, 1] * d.y + this[1, 2] * d.z
        val z = this[2, 0] * d.x + this[2, 1] * d.y + this[2, 2] * d.z
        return Float3(x, y, z).normalized()
    }

    companion object {
        fun identity() = Mat4()

        fun translation(x: Float, y: Float, z: Float): Mat4 {
            val m = identity()
            m[0, 3] = x
            m[1, 3] = y
            m[2, 3] = z
            return m
        }

        fun rotationX(degrees: Float): Mat4 {
            val rad = Math.toRadians(degrees.toDouble()).toFloat()
            val cos = cos(rad)
            val sin = sin(rad)
            val m = identity()
            m[1, 1] = cos
            m[1, 2] = -sin
            m[2, 1] = sin
            m[2, 2] = cos
            return m
        }

        fun rotationY(degrees: Float): Mat4 {
            val rad = Math.toRadians(degrees.toDouble()).toFloat()
            val cos = cos(rad)
            val sin = sin(rad)
            val m = identity()
            m[0, 0] = cos
            m[0, 2] = sin
            m[2, 0] = -sin
            m[2, 2] = cos
            return m
        }

        fun rotationZ(degrees: Float): Mat4 {
            val rad = Math.toRadians(degrees.toDouble()).toFloat()
            val cos = cos(rad)
            val sin = sin(rad)
            val m = identity()
            m[0, 0] = cos
            m[0, 1] = -sin
            m[1, 0] = sin
            m[1, 1] = cos
            return m
        }

        fun scale(s: Float): Mat4 {
            val m = identity()
            m[0, 0] = s
            m[1, 1] = s
            m[2, 2] = s
            return m
        }

        fun lookAt(eye: Float3, target: Float3, up: Float3): Mat4 {
            val forward = (target - eye).normalized()
            val right = forward.cross(up).normalized()
            val trueUp = right.cross(forward).normalized()

            val m = identity()
            m[0, 0] = right.x
            m[0, 1] = right.y
            m[0, 2] = right.z
            m[0, 3] = -right.dot(eye)

            m[1, 0] = trueUp.x
            m[1, 1] = trueUp.y
            m[1, 2] = trueUp.z
            m[1, 3] = -trueUp.dot(eye)

            m[2, 0] = -forward.x
            m[2, 1] = -forward.y
            m[2, 2] = -forward.z
            m[2, 3] = forward.dot(eye)

            return m
        }

        fun perspective(fovYDegrees: Float, aspect: Float, near: Float, far: Float): Mat4 {
            val rad = Math.toRadians(fovYDegrees.toDouble() * 0.5).toFloat()
            val tanHalfFov = tan(rad)
            val m = FloatArray(16)
            m[0] = 1f / (aspect * tanHalfFov)
            m[5] = 1f / tanHalfFov
            m[10] = -(far + near) / (far - near)
            m[11] = -1f
            m[14] = -(2f * far * near) / (far - near)
            return Mat4(m)
        }
    }
}
