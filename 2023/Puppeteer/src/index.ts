/**

ヘッドレスWebブラウザから画面の画像を連続的に(画面の変更があった場合に)抽出する方法は、次のとおりです。

ヘッドレスWebブラウザを起動します。
スクリーンショットを撮る関数を定義します。
スクリーンショットを撮る関数を繰り返し呼び出します。
画面の変更があった場合に、スクリーンショットを保存します。
*/


function takeScreenshot() {
    // 現在の画面をキャプチャします。
    const canvas = document.createElement('canvas');
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    const ctx = canvas.getContext('2d')!;
    ctx.drawImage(document.body, 0, 0);
  
    // キャプチャした画像をバイナリデータとして取得します。
    const data = canvas.toDataURL();
  
    // バイナリデータを画像ファイルとして保存します。
    const imageFile = new File([data], 'screenshot.png');
    imageFile.saveAs('screenshot.png');
  }
  
