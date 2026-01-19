#include <gtest/gtest.h>
#include "ftxui/component/component.hpp"
#include "ftxui/component/event.hpp"
#include "ftxui/screen/screen.hpp"
#include "dataaccess.hpp"

using namespace ftxui;

// --- UI Interaction Test ---
TEST(UITest, ButtonClick) {
    bool clicked = false;
    auto btn = Button("Click Me", [&] { clicked = true; });

    btn->OnEvent(Event::Return);
    EXPECT_TRUE(clicked);
}

// --- Rendering Test ---
TEST(UITest, Rendering) {
    auto document = vbox({
        text("Hello"),
        separator(),
        text("World"),
    });

    auto screen = Screen::Create(Dimension::Fixed(10), Dimension::Fixed(3));
    Render(screen, document);

    std::string output = screen.ToString();
    EXPECT_TRUE(output.find("Hello") != std::string::npos);
    EXPECT_TRUE(output.find("World") != std::string::npos);
}

// --- Logic Class Test ---
TEST(LogicTest, FirestoreServiceInit) {
    FirestoreService service([]{});
    EXPECT_FALSE(service.IsConnected());
    EXPECT_EQ(service.GetContacts().size(), 0);
}

// --- Input Test ---
TEST(UITest, InputField) {
    std::string val = "";
    auto input = Input(&val, "placeholder");

    input->OnEvent(Event::Character('T'));
    input->OnEvent(Event::Character('e'));
    input->OnEvent(Event::Character('s'));
    input->OnEvent(Event::Character('t'));

    EXPECT_EQ(val, "Test");
}
