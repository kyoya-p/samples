package practicalfilament.demo

fun main(args: Array<String>) {
    if (args.contains("--gui") || args.contains("desktop")) {
        DesktopApp.main(args)
        return
    }

    println("================================================================================")
    println(" Practical Filament - Reshape your UI! | Nicole Terc Demo")
    println(" Video Reference: https://www.youtube.com/watch?v=J4lM9TnlPxk")
    println(" Codebase Reference: https://github.com/nicole-terc/PracticalFilament")
    println("================================================================================")
    println()
    println("[Key Concepts from the Talk]")
    println("1. Engine & Scene: Central resource manager and entity-based scene graph")
    println("2. PBR Materials: Metallic-roughness model (baseColor, roughness, metallic, clearCoat)")
    println("3. Directional Light Gotcha: Vector points in EMISSION direction (-Z for front-lit)")
    println("4. Tangents Gotcha: VertexAttribute.TANGENTS must contain tangent space quaternions")
    println("5. MaterialBuilder: Runtime generation of Filament .mat shader definitions")
    println()
    println("Tip: Run `mise run desktop` to launch the interactive GUI 3D window!")
    println()

    val rasterizer = ConsoleRasterizer(width = 60, height = 24)

    println("--------------------------------------------------------------------------------")
    println(">>> Demo 1: Redball / Marble (PBR Sphere with Specular Highlight & ClearCoat)")
    println("--------------------------------------------------------------------------------")
    val sphereFrames = FilamentDemo.runMarbleDemo(rasterizer, frames = 6)
    print(sphereFrames[1])

    println("--------------------------------------------------------------------------------")
    println(">>> Demo 2: Lit Cube (Gold Metallic Material & Multi-face Lighting)")
    println("--------------------------------------------------------------------------------")
    val cubeFrames = FilamentDemo.runLitCubeDemo(rasterizer, frames = 6)
    print(cubeFrames[2])

    println("--------------------------------------------------------------------------------")
    println(">>> Demo 3: Hello Triangle (Custom Geometry & Filament Tangent Encoding)")
    println("--------------------------------------------------------------------------------")
    val triFrames = FilamentDemo.runTriangleDemo(rasterizer, frames = 12)
    print(triFrames[0])

    println("--------------------------------------------------------------------------------")
    println(">>> Demo 4: Material Builder (.mat code generation)")
    println("--------------------------------------------------------------------------------")
    val matSource = FilamentDemo.generateFilamentMaterialSample()
    println(matSource)

    println("================================================================================")
    println(" Practical Filament Demo completed successfully via mise & Gradle!")
    println("================================================================================")
}
