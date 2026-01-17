#include <iostream>
#include <string>
#include <vector>

#include "ftxui/component/component.hpp"
#include "ftxui/component/screen_interactive.hpp"
#include "ftxui/dom/elements.hpp"

using namespace ftxui;

int main() {
    auto screen = ScreenInteractive::TerminalOutput();

    int counter = 0;
    std::string button_label = "Counter: 0";

    // ボタンの作成
    auto button = Button(&button_label, [&] { 
        counter++; 
        button_label = "Counter: " + std::to_string(counter);
    }, ButtonOption::Animated());

    // チェックボックス
    bool checked = false;
    auto checkbox = Checkbox("Checkbox", &checked);

    // メニュー
    std::vector<std::string> entries = {"Option 1", "Option 2", "Option 3"};
    int selected = 0;
    auto menu = Menu(&entries, &selected);

    // レイアウトの統合
    auto container = Container::Vertical({
        button,
        checkbox,
        menu,
    });

    // 表示内容の定義
    auto renderer = Renderer(container, [&] {
        return vbox({
            text("C++ Mouse TUI Sample") | bold | center,
            separator(),
            hbox({
                vbox({
                    text("Controls") | bold,
                    button->Render(),
                    checkbox->Render(),
                    text("Internal Counter: " + std::to_string(counter)) | color(Color::Green),
                }) | border,
                vbox({
                    text("Menu") | bold,
                    menu->Render() | frame,
                }) | border | flex,
            }),
            separator(),
            text("Use your MOUSE to click buttons and select items.") | dim,
            text("Press 'q' to quit.") | dim,
        }) | border;
    });

    // キーボード操作での終了設定
    auto component = CatchEvent(renderer, [&](Event event) {
        if (event == Event::Character('q') || event == Event::Escape) {
            screen.Exit();
            return true;
        }
        return false;
    });

    screen.Loop(component);

    return 0;
}