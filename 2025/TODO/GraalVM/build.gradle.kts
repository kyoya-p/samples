plugins {
    kotlin("jvm") version "1.9.23"
    application
    id("org.graalvm.buildtools.native") version "0.11.2"
}

repositories {
    mavenCentral()
}

application {
    mainClass = "MainKt"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
graalvmNative {
    binaries {
        named("main") {
            // 生成される実行可能ファイル名
            imageName.set("my-kotlin-app")

            // ビルド時にネイティブイメージビルドに渡す引数
            buildArgs.addAll(
                "--no-fallback", // フォールバックJarを生成しない
                "-H:Name=my-kotlin-app"
                // 必要に応じてヒープサイズやその他オプションを設定
            )
        }

    }
}
