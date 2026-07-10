build.gradle.kts関係で、自分で2回以上検索したものです。自分の三回目用に。
基本Multiplatform志向。

# 環境
- [Kotlin](https://kotlinlang.org/docs/home.html): 2.0.20
- [Gradle](https://gradle.org/) 8.7

※[Kotlin-Gradle Version対応表](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-compatibility-guide.html)

# [Ubuntuにgradleインストール](https://gradle.org/install/)

Note: aptでインストールすると標準リポジトリのは古い(gradle-4.4とか)。
Note: 多くの場合java+gradlewで事足りるのでInstall不要

本家 https://services.gradle.org/distributions/ から取得
```sh:
version=8.7
wget https://services.gradle.org/distributions/gradle-$version-bin.zip
sudo unzip -d /opt/gradle gradle-$version-bin.zip
sudo ln -s /opt/gradle/gradle-$version/bin/gradle /usr/local/bin/
```


# [コンパイルオプション指定](https://www.youtube.com/watch?v=8F19ds109-o&list=WL&index=19)
```kotlin:build.gradle.kts 
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters") // Context Parameter 有効化
    }
}
```


# オブジェクト直列化 - [Serialization](https://kotlinlang.org/docs/serialization.html)
```kotlin:build.gradle.kts (JSONの例)
plugins {
    kotlin("plugin.serialization") version "2.2.21" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
}
//...
dependencies{
    implementation("org.jetbrains.kotlin:kotlin-serialization-json:1.9.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json
}
```

```source.kt
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class MyType(val a: Int)

val v: MyType = Json.decodeFromString("""{"a":42}""")
val s = Json.encodeToString(v)
```

- [Yaml](https://github.com/charleskorn/kaml) - [Sample](https://github.com/kyoya-p/samples/blob/78302f1403ccbf48138ece96d0e3ae56588abfb5/2021/SNMP4J/Snmp4jUtils_KtJvm/src/main/kotlin/Scanner.kt#L80)と[設定](https://github.com/kyoya-p/samples/blob/78302f1403ccbf48138ece96d0e3ae56588abfb5/2021/SNMP4J/Snmp4jUtils_KtJvm/build.gradle.kts#L22)
- [Yaml - Sampleと設定(2025/2/12)](https://github.com/kyoya-p/samples/blob/56192be88085c5d10e11ac39a4da40569c71d6d9/2025/openapi/openApiSchema/src/commonMain/kotlin/AppMain.kt#L20)

- [XML](https://github.com/rharter/kotlinx-serialization-xml)

# [Coroutine](https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core)

```build.gradle.kts
dependencies{
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
}
```

# 日時 - [DateTime](https://github.com/Kotlin/kotlinx-datetime)
```build.gradle.kts
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
}
```

```sample.kt
val format_yyyyMMddHHmmss =
    LocalDateTime.Format { year(); monthNumber(); dayOfMonth(); char('_'); hour(); minute();second() }
val localTime = now().toLocalDateTime(currentSystemDefault())
println(localTime.format(format_yyyyMMddHHmmss))
```

# ファイル入出力 - [okio](https://github.com/square/okio/)
```build.gradle.kts
dependencies{
    implementation("com.squareup.okio:okio:3.8.0")
    // https://mvnrepository.com/artifact/com.squareup.okio/okio
}
```

```kotlin:src/countLine.kt - 例: **/*.ktファイルを列挙し各行数をファイルに書き込む
val fileSystem=FileSystem.SYSTEM
// val fileSystem=NodeJsFileSystem // Kotlin/js(Node.js)の場合
val projRoot = fileSystem.canonicalize("../../../..".toPath())
val result = projRoot.resolve("build/count.txt").apply { fileSystem.delete(this) }
fileSystem.listRecursively(projRoot.resolve("src"))
    .filter { it.name.endsWith(".kt") }
    .map { it to fileSystem.read(it) { generateSequence { readUtf8Line() }.count() } }
    .forEach { p -> fileSystem.appendingSink(result).buffer().use { sink -> sink.writeUtf8("$p\n") } }
````

# データをファイルに保存 - [KStore](https://xxfast.github.io/KStore/overview.html)
[➔Installation](https://xxfast.github.io/KStore/installation.html)
```KStoreSample.kt

```
# WebAppフレームワーク - [Ktor](https://ktor.io/)
[Ktorプロジェクトのテンプレ作成サイト](https://start.ktor.io/)

```kotlin:Server - jsonを使用
dependencies {
    val ktor_version="3.3.1" // https://mvnrepository.com/artifact/io.ktor/ktor-server-core
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
}
```
```kotlin:Client - jsonを使用
dependencies {
    val ktor_version="3.3.1" // https://mvnrepository.com/artifact/io.ktor/ktor-client-core
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
}
```

[Json Client サンプル](https://github.com/ktorio/ktor-documentation/tree/main/codeSnippets/snippets/client-json-kotlinx)
[Ktor Clientで証明書チェックを無効化する例](https://github.com/ktorio/ktor-documentation/blob/main/codeSnippets/snippets/sockets-client-tls/src/main/kotlin/com/example/Application.kt)

# 既存タスクに処理を追加 - 例1:build後生成されたファイルコピー

```build.gradle.kts
tasks["build"].doLast {
    copy {
        from("$buildDir/distributions")
        into("$projectDir/dist")
    }
}
```
※task生成されるタイミングによってはtaskがまだ存在しなくてエラーになる可能性あり[TODO]

# 既存タスクに処理を追加 - 例2:build前に環境変数値をコードに挿入
```build.gradle.kts
tasks["build"].doFirst {
    copy {
        from("src/main/kotlin/Properties.kt.tmpl")
        into("src/main/kotlin/")
        rename { "Properties.kt" }
        filter { it.replace("$[APPKEY]", System.getenv("APPKEY") ?: "'export APPKEY=...'") }
    }
}
```
```kotlin:src/jsMain/kotlin/Properties.kt.tmpl
val appKey="$[APPKEY]"
```
※誤ってAPPKEYを含んだProperties.kt をコミットしないよう注意。 例: gitの場合、src/main/kotlin/.gitignore に `Properties.kt` を追記

# 自分のMavenリポジトリへのUpload
ローカルリポジトリにデプロイ
```build.gradle.kts
plugins{
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "jp.wjg.shokkaa"
            artifactId = "myappname"
            version = "1.0.0"
            from(components["kotlin"])
        }
    }
}
```
```sh:ローカルリポジトリにデプロイ
gradlew publishToMavenLocal
```
ローカルリポジトリの場所: `~\.m2\repository\`

```build.gradle.kts:build.gradle.kts - ローカルリポジトリ利用側
repositories {
   mavenLocal()
}
dependencies {
  implementation("jp.wjg.shokkaa:myappname:1.0.0")
}

```
# [ソースコード配置フォルダ指定](https://kotlinlang.org/docs/gradle.html#kotlin-and-java-sources)
```build.gradle.kts
sourceSets.main {
    java.srcDirs("src/main/myJava", "src/main/myKotlin")
}
```

# テスト - Junit
```build.gradle.kts
dependencies {
    testImplementation(kotlin("test"))
}
tasks.withType<Test>().configureEach {
   useJUnitPlatform()
}
```
```kotlin: 例：Tests.kt
class Tests {
    @Test fun test1() {assert(2 + 3 == 6)} // Assertion failed
}
```
[](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)
```kotlin: coroutine/suspend関数のテスト - build.gradle.kts
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
```
```kotlin: coroutine/suspend関数のテスト - 例:Test.kt
class MyTests {
    @Test fun test2() = runTest { delay(1.seconds) }
}
```

```sh:クラス/メソッドを指定しテスト
sh gradlew test --tests 'Tests'
sh gradlew test --tests 'Tests#test1'
```
# テスト - [Kotest](https://kotest.io/)
```build.gradle.kts
dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-framework-engine:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-property:5.9.1")
    testImplementation("io.kotest:kotest-extensions-jvm:5.9.1")
}
```
```kotlin:例: Tests.kt
class Tests : FunSpec({
    test("1") { 1 + 2 shouldBe 3 }
    test("sum2") { 1 + 2 shouldNotBe 5 }
})
```
```kotlin:例2: Tests.kt - 
class Test2 : FunSpec() {
    override fun isolationMode(): IsolationMode = IsolationMode.InstancePerLeaf
    init {
        var c = 0
        test("inc1") { ++c shouldBe 1 }
        test("inc2") { ++c shouldBe 1 shouldNotBe 2 }
    }
}
```
```sh:クラス/領域を選択しテスト
kotest_filter_specs='Tests' sh ./gradlew test # テストクラス名指定
kotest_filter_tests='sum*' sh ./gradlew test  # test()で指定のテスト名の正規表現
```
[Tagでカテゴライズする方法](https://kotest.io/docs/framework/tags.html#running-with-tags)


```kotlin:  gradle plugin設定
plugins{
  id("io.kotest") version "6.0.0.M2"  // https://plugins.gradle.org/plugin/io.kotest
}
```

# テスト - TestBalloon

テスト - TestBalloon (https://github.com/infix-de/testBalloon)

```build.gradle.kts
plugins {
    kotlin("jvm") version "2.4.0"
    id("de.infix.testBalloon") version "1.0.1-K2.4.0" // https://plugins.gradle.org/plugin/de.infix.testBalloon
}

dependencies {
    implementation("de.infix.testBalloon:testBalloon-framework-core:1.0.1-K2.4.0")
    implementation("io.kotest:kotest-assertions-core:5.9.1")
}
```

```例: FibonacciTest.kt
package testballoon.fibonacci

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

// トップレベルに testSuite の委譲プロパティとして定義
val FibonacciTests by testSuite {
    test("fibonacci base cases") {
        fib(0) shouldBe 0
        fib(1) shouldBe 1
    }

    test("fibonacci sequence values") {
        fib(5) shouldBe 5
        fib(10) shouldBe 55
    }

    test("fibonacci negative input throws exception") {
        shouldThrow<IllegalArgumentException> {
            fib(-1)
        }
    }
}
```

```sh: 全部テスト / 特定のテストスイート/テストを選択しテスト
./gradlew test
./gradlew test --tests "testballoon.fibonacci.FibonacciTests|fibonacci base cases"
```

# [関連ファイルをダウンロード](https://github.com/kyoya-p/samples/blob/1c4bb40e0075b1c0a15c65470a0ffd7e486339f3/2022/Tess4jOCR/build.gradle.kts#L33-L36)
```build.gradle.kts
plugins {
  // ...
  id("de.undercouch.download") version "5.0.5" // https://github.com/michel-kraemer/gradle-download-task
}

// OpenCVをダウンロードするタスクの例
val downloadFiles by tasks.registering {
    doLast {
        download.run {
            src("https://sourceforge.net/projects/opencvlibrary/files/4.5.5/opencv-4.5.5-vc14_vc15.exe/download")
            dest("$buildDir/libs/opencv.exe")
        }
    }
}
```
# [build中のzipファイル展開](https://github.com/kyoya-p/samples/blob/1c4bb40e0075b1c0a15c65470a0ffd7e486339f3/2022/Tess4jOCR/build.gradle.kts#L37-L40)


# [7zipプラグイン](https://docs.freefair.io/gradle-plugins/6.5.1/reference/)
```build.gradle.kts
plugins {
 // ...
 id("io.freefair.compress.7z") version "6.4.3" // https://plugins.gradle.org/plugin/io.freefair.compress.7z
}
```
[参考](https://www.programcreek.com/java-api-examples/samples/?api=org.apache.commons.compress.archivers.sevenz.SevenZFile)


# 外部のプログラムの実行
```build.gradle.kts
// OpenCVの自己解凍exe形式ファイルを実行(展開)しコピーする例(Windows用)
val extractOpenCV by tasks.registering {
  doLast {
    exec {
      commandLine("$buildDir/libs/opencv.exe", "-o$buildDir", "-y")
    }
    copy {
      from(fileTree("$buildDir/opencv/build/java/x64/"))
      into("$projectDir")
    }
  }
  //dependsOn(downloadOpenCV)
}
```
```build.gradle.kts
task("listDir") {
    doLast {
        project.exec {
            commandLine("cmd", "/c", "dir")
            standardOutput = file("dir_output.txt").outputStream()
        }
    }
}
```
※設定キャッシュ(org.gradle.configuration-cache=true)を有効にするとエラー(TODO)

# ShadowJar - 単一の実行可能jarファイル
- Kotlin :2.1.20
```

```


- Kotlin1.5.20, Shadow7.0.0 
https://github.com/johnrengelman/shadow
https://imperceptiblethoughts.com/shadow/
https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
※Shadowのメジャーバージョンはgradleに対応している模様(Shadow Version 7.0.0以降はgradle7が必要)
設定

```build.gradle.kts
plugins{
    application
    id("com.github.johnrengelman.shadow") version "7.1.2" // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
}
application {
    mainClass.set("mypackage.MainKt") // package mypackageかつファイル名main.ktのmain()を実行する場合
}
```

ビルド

`gradlew shadowJar`
build/libs 以下にjarファイルが生成される

実行(例)

`java -jar build/libs/app-1.0-SNAPSHOT-all.jar`

# gradleのVersion指定(gradlewやIntelliJ使用)

```gradle/wrapper/gradle-wrapper.propertiesを変更
...
distributionUrl=https\://services.gradle.org/distributions/gradle-6.8-bin.zip
...
```


# 外部のJarファイルを参照

```build.gradle.kts
dependencies {
    implementation(files("****.jar")) // jarファイル等
}
```
[プロジェクトへの依存関係 Example.11](https://translate.google.com/translate?hl=ja&sl=en&u=https://docs.gradle.org/current/userguide/declaring_dependencies.html&prev=search&pto=aue)
[TODO] TypeSafeな外部プロジェクト指定 (7.0時点で実験的機能) enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")


# 依存モジュール
[脆弱性チェック:](https://plugins.gradle.org/plugin/org.owasp.dependencycheck)
```build.gradle.kts
plugins{
    id("org.owasp.dependencycheck") version "11.1.0"
    // https://plugins.gradle.org/plugin/org.owasp.dependencycheck
}
// 最近はNVDの脆弱性DB取得にAPI_KEYが(事実上)必須な模様
// https://nvd.nist.gov/developers/request-an-api-key
dependencyCheck {
    nvd { apiKey = System.getenv("NVD_API_KEY") }
}
```
```shell:チェック
export NVD_API_KEY=XXXXXX
sh gradlew dependencyCheckAnalyze
# 結果: build/reports/dependency-check-report.html
```

[SBOM生成:](https://github.com/CycloneDX/cyclonedx-gradle-plugin)
```build.gradle.kts
plugins{
  id("org.cyclonedx.bom") version "1.10.0" // https://plugins.gradle.org/plugin/org.cyclonedx.bom
}
group = "my-group" // 自身のBOM生成に必要
version = "1.0-SNAPSHOT" // 自身のBOM生成に必要
```
```shell:生成
sh gradlew cyclonedxBom
# 結果: build/reports/bon.json
```

# [Proxy設定](https://docs.gradle.org/current/userguide/build_environment.html#sec:accessing_the_web_via_a_proxy)
https://docs.gradle.org/current/userguide/build_environment.html#sec:accessing_the_web_via_a_proxy

# [Windows][日本語] ビルド時のエラーメッセージが化ける場合
```build.gradle.kts
tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
```

# Githubに生成物をコミット
https://qiita.com/shokkaa/items/27627a46ac67f9192ef5
