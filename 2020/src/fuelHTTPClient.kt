package jp.co.casareal.fuel

// 参考
// https://www.codeflow.site/ja/article/kotlin-fuel

import com.github.kittinunf.fuel.httpGet
import java.io.File

fun main() {

    // 非同期処理
    /* "https://batspi.com/card/BS38-X03.jpg".httpGet().response { request, response, result ->
        when (result) {
            is Result.Success -> println("非同期処理の結果：" + String(response.data))
            is Result.Failure -> println("通信に失敗しました。")
        }
    }*/


    val result = "https://batspi.com/card/BS38-X03.jpg".httpGet().response()
    File("image.jpg").writeBytes(result.second.data)
}
