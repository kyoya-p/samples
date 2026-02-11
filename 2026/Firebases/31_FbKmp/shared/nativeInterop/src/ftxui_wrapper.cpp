#include "ftxui_wrapper.h"
#include <ftxui/dom/elements.hpp>
#include <ftxui/screen/screen.hpp>
#include <ftxui/component/component.hpp>
#include <ftxui/component/screen_interactive.hpp>
#include <iostream>

using namespace ftxui;

extern "C" {

void hello_ftxui() {
    auto document = vbox({
        text("Hello from FTXUI (Real!)") | border,
    });
    auto screen = Screen::Create(
        Dimension::Fixed(50),
        Dimension::Fixed(3)
    );
    Render(screen, document);
    screen.Print();
}

void start_ftxui_loop() {
    auto screen = ScreenInteractive::TerminalOutput();
    
    int count = 0;
    
    auto renderer = Renderer([&] {
        return vbox({
            text("FTXUI Event Loop in Kotlin Native") | bold | color(Color::Green),
            separator(),
            text("Count: " + std::to_string(count)),
            separator(),
            hbox({
                text("Press 'q' to quit.") | border,
                text("Press 'c' to count.") | border,
            })
        }) | border;
    });

    renderer |= CatchEvent([&](Event event) {
        if (event == Event::Character('q')) {
            screen.ExitLoopClosure()();
            return true;
        }
        if (event == Event::Character('c')) {
            count++;
            return true;
        }
        return false;
    });

    screen.Loop(renderer);
}

}
