plugins{
    kotlin("jvm") version "2.2.20"
    application
    id("org.graalvm.buildtools.native") version "0.9.28"
}

application {
    mainClass = "MainKt"
}

//graalvm.buildtools.native {
//    // 実行可能ファイル名を設定（.exe拡張子は自動で付加される）
//    binaries {
//        named("main") {
//            // 生成される実行可能ファイル名
//            imageName.set("my-kotlin-app")
//
//            // ビルド時にネイティブイメージビルドに渡す引数
//            buildArgs.addAll(
//                "--no-fallback", // フォールバックJarを生成しない
//                "-H:Name=my-kotlin-app"
//                // 必要に応じてヒープサイズやその他オプションを設定
//            )
//        }
//    }
//}