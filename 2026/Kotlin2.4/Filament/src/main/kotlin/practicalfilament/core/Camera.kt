package practicalfilament.core

enum class ProjectionType {
    PERSPECTIVE,
    ORTHOGRAPHIC
}

data class CameraConfig(
    var position: Float3 = Float3(0f, 0f, 4f),
    var lookAt: Float3 = Float3(0f, 0f, 0f),
    var up: Float3 = Float3.UP,
    var fovDegrees: Float = 45f,
    var near: Float = 0.1f,
    var far: Float = 100f,
    var projectionType: ProjectionType = ProjectionType.PERSPECTIVE
) {
    fun getViewMatrix(): Mat4 = Mat4.lookAt(position, lookAt, up)

    fun getProjectionMatrix(aspectRatio: Float): Mat4 {
        return when (projectionType) {
            ProjectionType.PERSPECTIVE -> Mat4.perspective(fovDegrees, aspectRatio, near, far)
            ProjectionType.ORTHOGRAPHIC -> Mat4.identity() // Ortho matrix
        }
    }
}
