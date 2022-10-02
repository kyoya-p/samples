plugins {
    application
    id("com.github.breadmoirai.github-release") version "2.3.7" // https://plugins.gradle.org/plugin/com.github.breadmoirai.github-release
}

version = "220615.4"

repositories {
    mavenCentral()
}

group = "com.github.kyoya-p"

githubRelease {
    repo.set("samples")
    owner.set(System.getenv("GITHUB_USER"))
    authorization("Token ${System.getenv("GITHUB_TOKEN")}")
    targetCommitish.set("master")
    releaseName.set("Test Project")
    releaseAssets.from(File("$buildDir/libs").walk().filter { it.isFile }.map { it.path }.toList())
    body.set("Path: https://github.com/kyoya-p/samples/tree/master/2022/GithubReleaseUploader")
    tagName.set("Test-$version")
    overwrite.set(true)
    //allowUploadToExisting.set(true)
    draft.set(true)
}
