#include "ui.hpp"
#include "utils.hpp"
#include "ftxui/dom/elements.hpp"
#include "ftxui/component/component.hpp"
#include <memory>
#include <vector>
#include <random>
#include <algorithm>

#ifdef RGB
#undef RGB
#endif

using namespace ftxui;

std::string GenerateRandomName() {
  static const std::vector<std::string> f = {"Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Heidi"};
  static const std::vector<std::string> l = {"Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller"};
  static std::random_device rd;
  static std::mt19937 gen(rd());
  std::uniform_int_distribution<> d1(0, 7);
  std::uniform_int_distribution<> d2(0, 6);
  std::uniform_int_distribution<> d3(100, 999);
  return f[d1(gen)] + " " + std::to_string(d3(gen)) + " " + l[d2(gen)];
}

std::string GenerateRandomEmail(const std::string& name) {
  std::string e = name;
  std::transform(e.begin(), e.end(), e.begin(), [](unsigned char c) { return (unsigned char)::tolower(c); });
  std::replace(e.begin(), e.end(), ' ', '.');
  return e + "@example.com";
}

Element MakeTableRow(Element name, Element email, Element time, Element op) {
    return hbox({
        std::move(name)  | size(WIDTH, EQUAL, 28),
        std::move(email) | flex,
        std::move(time)  | size(WIDTH, EQUAL, 16),
        std::move(op)    | size(WIDTH, EQUAL, 10) | center,
    });
}

VisibilityObserver::VisibilityObserver(Component child, std::function<void()> on_v) : child_(child), on_v_(on_v) { Add(child_); }
Element VisibilityObserver::Render() { on_v_(); return child_->Render(); }
bool VisibilityObserver::OnEvent(Event e) { return child_->OnEvent(e); }
bool VisibilityObserver::Focusable() const { return child_->Focusable(); }

Component CreateAppUI(FirestoreService& service, std::function<void()> on_exit, bool is_snapshot) {
    auto show_config = std::make_shared<bool>(false);
    static std::string api_key_input_val = "";
    
    const char* env = std::getenv("FB_API_KEY");
    if (!env) env = std::getenv("API_KEY");
    if (api_key_input_val.empty() && env) api_key_input_val = env;

    auto key_input = Input(&api_key_input_val, "API Key");
    auto connect_btn = Button("[Connect]", [=, &service] { if (service.Initialize(api_key_input_val, 20)) { *show_config = false; } }, ButtonOption::Ascii());
    auto cancel_btn = Button("[Cancel]", [=] { *show_config = false; }, ButtonOption::Ascii());
    
    auto config_container = Container::Vertical({ key_input, Container::Horizontal({ connect_btn, cancel_btn }) | center });
    auto config_renderer = Renderer(config_container, [=, &service] {
        if (is_snapshot) return vbox({ text("Configuration Placeholder") | border }) | center;
        return vbox({ text("Configuration") | bold | center, separator(), hbox(text("API Key: "), key_input->Render()) | border, separator(), hbox(connect_btn->Render(), cancel_btn->Render()) | center, text(""), text(service.GetError()) | color(Color::Red) | center }) | center | border | size(WIDTH, GREATER_THAN, 60);
    });

    auto n_name = std::make_shared<std::string>(GenerateRandomName());
    auto n_email = std::make_shared<std::string>(GenerateRandomEmail(*n_name));
    
    auto rows_container = Container::Vertical({});
    auto refresh_ui = [=, &service](const std::vector<Contact>& contacts) {
        rows_container->DetachAllChildren();
        for (size_t i = 0; i < contacts.size(); ++i) {
            const auto& contact = contacts[i];
            auto contact_id = contact.id;
            auto contact_name = contact.name;
            auto contact_email = contact.email;
            auto contact_time = contact.timestamp;
            int idx = (int)i;

            auto on_click = [=] {
                *n_name = contact_name;
                *n_email = contact_email;
            };

            auto remove_btn = Button("[Remove]", [=, &service] { 
                Log("Remove button clicked for Contact ID: " + contact_id);
                service.RemoveContact(contact_id); 
            }, ButtonOption::Ascii());
            
            // Define the row rendering logic
            auto row_renderer = Renderer(remove_btn, [=, &service] {
                Element op = is_snapshot ? text("[Remove]") : remove_btn->Render();
                auto el = MakeTableRow(text(contact_name), text(contact_email), text(contact_time), op);
                if (idx % 2 != 0) el = el | bgcolor(Color::RGB(60, 60, 60));
                if (!is_snapshot && remove_btn->Focused()) return el | inverted;
                return el;
            });

            // Make the whole row clickable without blocking child buttons.
            // We use CatchEvent to handle clicks only if the child (remove button) didn't handle it.
            auto row_clickable = CatchEvent(row_renderer, [=](Event event) {
                // If it's a mouse click
                if (event.is_mouse() && event.mouse().button == Mouse::Left && event.mouse().motion == Mouse::Pressed) {
                    // Check if the click is within the row component's area.
                    // Since CatchEvent doesn't expose the box easily, we rely on the fact that
                    // FTXUI containers only pass mouse events to children if they are within bounds.
                    // To be safe and fix the "blocking" issue, we only trigger on_click if the event
                    // WAS NOT handled by the remove button (child).
                    // BUT, CatchEvent is called BEFORE the child.
                    // This is why we need to call child->OnEvent manually or use a different approach.
                    
                    // The standard way: if child didn't handle it, we do.
                    if (row_renderer->OnEvent(event)) {
                        return true;
                    }
                    on_click();
                    return true;
                }
                return false;
            });

            rows_container->Add(row_clickable);
        }
        if (service.HasMore()) {
            auto loader = Renderer([] { return hbox({ filler(), text("Loading...") | color(Color::Yellow), filler() }); });
            rows_container->Add(std::make_shared<VisibilityObserver>(loader, [&service] { service.LoadMore(20); }));
        }
    };

    auto name_input = Input(n_name.get(), "Name");
    auto email_input = Input(n_email.get(), "Email");
    auto add_btn = Button("[Add]", [=, &service] { 
        if (!n_name->empty()) { 
            service.AddContact(*n_name, *n_email); 
            *n_name = GenerateRandomName(); 
            *n_email = GenerateRandomEmail(*n_name); 
        } 
    }, ButtonOption::Ascii());

    auto add_row_c = Container::Horizontal({name_input, email_input, add_btn});
    auto add_row = Renderer(add_row_c, [=] {
        return vbox({ separator(), MakeTableRow(is_snapshot ? text("Name") : name_input->Render(), is_snapshot ? text("Mail") : email_input->Render(), text("(Now)"), is_snapshot ? text("[Add]") : add_btn->Render()), separator() });
    });

    auto close_btn = Button("[Close]", on_exit, ButtonOption::Ascii());
    auto activate_btn = Button("[Activate]", [=] { *show_config = true; }, ButtonOption::Ascii());
    
    auto main_container = Container::Vertical({ rows_container, add_row, Container::Horizontal({ activate_btn, close_btn }) });
    auto app_renderer = Renderer(main_container, [=, &service] { 
        Elements rows;
        rows.push_back(text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold);
        rows.push_back(separator());
        rows.push_back(vbox({ MakeTableRow(text("Name") | bold, text("Mail") | bold, text("Time") | bold, text("Op") | bold), separator() }));
        if (is_snapshot) rows.push_back(rows_container->Render());
        else rows.push_back(rows_container->Render() | vscroll_indicator | frame | flex);
        rows.push_back(add_row->Render());
        rows.push_back(hbox({ filler(), activate_btn->Render(), text(" "), close_btn->Render() }));
        return vbox(std::move(rows)) | border;
    });

    auto root = Renderer([=] {
        if (*show_config) return dbox({ app_renderer->Render() | color(Color::GrayDark), config_renderer->Render() | center });
        return app_renderer->Render();
    });

    refresh_ui(service.GetContacts());

    return CatchEvent(root, [=, &service](Event event) {
        if (event == Event::Custom) { refresh_ui(service.GetContacts()); return true; }
        if (event == Event::Character('q')) { on_exit(); return true; }
        if (*show_config) return config_container->OnEvent(event);
        return main_container->OnEvent(event);
    });
}
