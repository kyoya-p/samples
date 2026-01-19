#ifndef UI_HPP
#define UI_HPP

#include "ftxui/component/component.hpp"
#include "dataaccess.hpp"

// Component to observe when it's rendered, used for infinite scrolling
class VisibilityObserver : public ftxui::ComponentBase {
public:
    VisibilityObserver(ftxui::Component child, std::function<void()> on_v);
    ftxui::Element Render() override;
    bool OnEvent(ftxui::Event e) override;
    bool Focusable() const override;
private:
    ftxui::Component child_;
    std::function<void()> on_v_;
};

// Main function to build the application UI
ftxui::Component CreateAppUI(FirestoreService& service, std::function<void()> on_exit, bool is_snapshot = false);

// UI Helpers
std::string GenerateRandomName();
std::string GenerateRandomEmail(const std::string& name);
ftxui::Element MakeTableRow(ftxui::Element name, ftxui::Element email, ftxui::Element time, ftxui::Element op);

#endif // UI_HPP