https://github.com/Kotlin/kotlin-wasm-examples/blob/main
https://github.com/Kotlin/kotlin-wasm-examples/tree/main/nodejs-example#kotlinwasm-nodejs-example

# demo code実行
```
# Ubuntu 22.04
# JDK 17

git clone --depth 1 https://github.com/Kotlin/kotlin-wasm-examples
cd kotlin-wasm-examples/nodejs-example
./gradlew wasmJsNodeRun
```

gradle中でをnode-v21をダウンロードしてくるようだ。


# WebAssembly実行環境 wasmer
```
curl https://get.wasmer.io -sSfL | sh
```