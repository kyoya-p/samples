plugins {
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    id("org.ajoberstar.git-publish") version "5.1.2"
}

gitPublish {
    repoUri = "https://github.com/kyoya-p/kyoya-p.github.io.git"
    branch = "gh-pages"
    contents {
        from("composeApp/build/dist/wasmJs/productionExecutable")
        into("flexpillar-v2")
    }
    preserve {
        include("**/*")
    }
}

tasks.named("gitPublishCopy") {
    dependsOn(":composeApp:wasmJsBrowserDistribution")
}
