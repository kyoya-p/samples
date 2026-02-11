
#ifndef FTXUI_WRAPPER_H
#define FTXUI_WRAPPER_H

#ifdef __cplusplus
extern "C" {
#endif

// Kotlin側から渡される描画用コールバック（現在の状態に応じた文字列を返す）
typedef const char* (*RenderCallback)();

// FTXUIのイベントループを開始する
// 内部で screen.Loop(renderer) を実行し、描画時にコールバックを呼び出す
void start_ftxui_app(RenderCallback callback);

#ifdef __cplusplus
}
#endif

#endif // FTXUI_WRAPPER_H
