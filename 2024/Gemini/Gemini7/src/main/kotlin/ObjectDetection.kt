import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.aiplatform.v1.*
import com.google.protobuf.ByteString
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths


fun main() {
    // 1. 設定
    val project = "YOUR_PROJECT_ID" // 自身のプロジェクト ID に変更
    val location = "us-central1"  // 利用するリージョン
    val model = "gemini-pro-vision"
    val jsonKeyPath = "path/to/your/service_account_key.json" // 自身のキーファイルのパスに変更
    val imagePath = "path/to/your/image.jpg" // 解析する画像のパスに変更
    val objectToDetect = "cat"  // 検知する物体の種類

    // 2. 認証設定
    val credentials = GoogleCredentials.fromStream(FileInputStream(jsonKeyPath))
    val credentialsProvider = FixedCredentialsProvider.create(credentials)

    // 3. APIクライアントの作成
    val predictionServiceClient = PredictionServiceClient.create(
        PredictionServiceSettings.newBuilder()
            .setCredentialsProvider(credentialsProvider)
            .build()
    )

    // 4. 画像データの準備
    val imageBytes = Files.readAllBytes(Paths.get(imagePath))
    val imageByteString = ByteString.copyFrom(imageBytes)
    val image = Image.newBuilder().setData(imageByteString).build()

    // 5. テキストプロンプトを作成
    val textPrompt = "Draw a mask that identifies the $objectToDetect in the image"
    val textPart = Part.newBuilder().setText(textPrompt).build()

    // 6. 画像とテキストをまとめたコンテンツを作成
    val imagePart = Part.newBuilder().setImage(image).build()
    val content = Content.newBuilder().addParts(imagePart).addParts(textPart).build()

    // 7. 推論リクエストを作成
    val predictRequest = PredictRequest.newBuilder()
        .setEndpoint(EndpointName.of(project, location, model).toString())
        .addInstances(content.toValue())
        .build()

    try {
        // 8. 推論の実行
        val predictResponse = predictionServiceClient.predict(predictRequest)

        // 9. 推論結果の処理
        val predictions = predictResponse.predictionsList
        if (predictions.isNotEmpty()) {
            val prediction = predictions[0]
            val mask = prediction.structValue.fieldsMap["masks"]
            if(mask != null && mask.listValue.valuesList.isNotEmpty()){
                val maskData = mask.listValue.valuesList[0].stringValue
                val base64Decoded = java.util.Base64.getDecoder().decode(maskData)
                //ここでmaskDataを使ってマスク画像を扱う
                println("Mask data: ${base64Decoded}")
            }else{
                println("No masks detected.")
            }
        }else{
            println("No predictions found.")
        }


    } catch (e: Exception) {
        println("Error during prediction: $e")
        e.printStackTrace()
    } finally {
        predictionServiceClient.close()
    }
}
