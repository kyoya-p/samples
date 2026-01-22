#include <algorithm>
#include <chrono>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <memory>
#include <random>
#include <string>
#include <vector>
#include <thread>
#include <mutex>
#include <atomic>
#include <unordered_map>
#include <exception>
#include <ctime>

#ifdef _WIN32
#include <process.h>
#include <windows.h>
#else
#include <unistd.h>
#endif

#include "ftxui/component/captured_mouse.hpp"
#include "ftxui/component/component.hpp"
#include "ftxui/component/component_base.hpp"
#include "ftxui/component/component_options.hpp"
#include "ftxui/component/screen_interactive.hpp"
#include "ftxui/dom/elements.hpp"
#include "ftxui/util/ref.hpp"
#include "ftxui/screen/terminal.hpp"

#include "ftxui/dom/table.hpp"

#include "dataaccess.hpp"
#include "utils.hpp"

using namespace ftxui;

class VisibilityObserver : public ComponentBase {
public:
    VisibilityObserver(Component child, std::function<void()> on_v) : child_(child), on_v_(on_v) { Add(child_); }
    Element Render() override { on_v_(); return child_->Render(); }
    bool OnEvent(Event e) override { return child_->OnEvent(e); }
    bool Focusable() const override { return child_->Focusable(); }
private:
    Component child_;
    std::function<void()> on_v_;
};

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

int main(int argc, char** argv) {
  try {
      std::ofstream(GetLogFilename(), std::ios::trunc);
      auto build_ui = [&](FirestoreService& service, std::function<void()> on_exit) -> Component {
          auto show_config = std::make_shared<bool>(false);
          static std::string api_key_input_val = "";
          const char* env = std::getenv("FB_API_KEY");
          if (!env) env = std::getenv("API_KEY");
          static std::string cur_key = (env != nullptr) ? env : "";
          api_key_input_val = cur_key;

          auto on_connect = [=, &service] { if (service.Initialize(api_key_input_val, 20)) { *show_config = false; } };
          auto on_cancel_config = [=] { *show_config = false; };

          auto key_input = Input(&api_key_input_val, "API Key");
          auto connect_btn = Button("[Connect]", on_connect, ButtonOption::Ascii());
          auto cancel_btn = Button("[Cancel]", on_cancel_config, ButtonOption::Ascii());
          auto config_container = Container::Vertical({ key_input, Container::Horizontal({ connect_btn, cancel_btn }) | center });
          auto config_renderer = Renderer(config_container, [=, &service] {
              return vbox({ text("Configuration") | bold | center, separator(), hbox(text("API Key: "), key_input->Render()) | border, separator(), hbox(connect_btn->Render(), cancel_btn->Render()) | center, text(""), text(service.GetError()) | color(Color::Red) | center }) | center | border | size(WIDTH, GREATER_THAN, 60);
          });
          auto rows_container = Container::Vertical({});
          auto selected_row = std::make_shared<int>(-1);

          static std::string n_name = GenerateRandomName();
          static std::string n_email = GenerateRandomEmail(n_name);

          auto current_sort_field = std::make_shared<FirestoreService::SortField>(FirestoreService::SortField::Timestamp);
          auto current_sort_dir = std::make_shared<FirestoreService::SortDirection>(FirestoreService::SortDirection::Descending);

          auto header_btn = [&](std::string title, FirestoreService::SortField field) {
              auto option = ButtonOption::Ascii();
              option.transform = [=](const EntryState& s) {
                  std::string arrow = "";
                  if (*current_sort_field == field) {
                      arrow = (*current_sort_dir == FirestoreService::SortDirection::Ascending) ? "▲" : "▼";
                  }
                  auto element = text(title + arrow);
                  if (s.focused) return element | inverted;
                  return element;
              };
              return Button(title, [=, &service] {
                  if (*current_sort_field == field) {
                      *current_sort_dir = (*current_sort_dir == FirestoreService::SortDirection::Ascending) 
                                          ? FirestoreService::SortDirection::Descending 
                                          : FirestoreService::SortDirection::Ascending;
                  } else {
                      *current_sort_field = field;
                      *current_sort_dir = FirestoreService::SortDirection::Descending;
                  }
                  service.SetSort(*current_sort_field, *current_sort_dir);
              }, option);
          };

          auto name_h_btn = header_btn("Name", FirestoreService::SortField::Name);
          auto mail_h_btn = header_btn("Mail Address", FirestoreService::SortField::Email);
          auto time_h_btn = header_btn("Created At", FirestoreService::SortField::Timestamp);
          auto dummy_op = Renderer([]{ return text("Operation"); });

          auto header_container = Container::Horizontal({
              name_h_btn | size(WIDTH, EQUAL, 28),
              mail_h_btn | flex,
              time_h_btn | size(WIDTH, EQUAL, 16),
              dummy_op   | size(WIDTH, EQUAL, 10)
          });

          auto header_final_renderer = Renderer(header_container, [=] {
               return hbox({
                   name_h_btn->Render() | size(WIDTH, EQUAL, 28) | bold,
                   text(" "),
                   mail_h_btn->Render() | flex | bold,
                   text(" "),
                   time_h_btn->Render() | size(WIDTH, EQUAL, 16) | bold,
                   text(" "),
                   dummy_op->Render() | size(WIDTH, EQUAL, 10) | center | bold
               });
          });

          auto refresh_ui = [=, &service](const std::vector<Contact>& contacts) {
            rows_container->DetachAllChildren();
            for (size_t i = 0; i < contacts.size(); ++i) {
              const auto& contact = contacts[i];
              auto contact_id = contact.id;
              auto contact_name = contact.name;
              auto contact_email = contact.email;
              auto contact_time = contact.timestamp;
              int idx = (int)i;

              auto remove_btn = Button("[Remove]", [=, &service] { service.RemoveContact(contact_id); }, ButtonOption::Ascii());
              
              auto row_renderer = Renderer(remove_btn, [=, &service] {
                Element op = remove_btn->Render();
                auto el = hbox({
                    text(contact_name) | size(WIDTH, EQUAL, 28),
                    text(" "),
                    text(contact_email) | flex,
                    text(" "),
                    text(contact_time) | size(WIDTH, EQUAL, 16),
                    text(" "),
                    op | size(WIDTH, EQUAL, 10) | center
                });

                if (idx == *selected_row) {
                    el = el | inverted;
                } else if (idx % 2 != 0) {
                     el = el | bgcolor(Color::RGB(40, 40, 40));
                }
                
                if (remove_btn->Focused()) return el | inverted;
                return el;
              });

              auto row_component = CatchEvent(row_renderer, [=, &service](Event event) {
                  if (event.is_mouse() && event.mouse().button == Mouse::Left && event.mouse().motion == Mouse::Pressed) {
                       *selected_row = idx;
                       // Copy data to input fields
                       n_name = contact_name;
                       n_email = contact_email;
                       return true; 
                  }
                  if (idx == *selected_row && (event == Event::Delete || event == Event::Character((char)127))) {
                      service.RemoveContact(contact_id);
                      return true;
                  }
                  return false;
              });
              
              rows_container->Add(row_component);
            }
            if (service.HasMore()) {
                auto loader = Renderer([] { return hbox({ filler(), text("Loading...") | color(Color::Yellow), filler() }); });
                rows_container->Add(std::make_shared<VisibilityObserver>(loader, [&service] { service.LoadMore(20); }));
            } else {
                 rows_container->Add(Renderer([] { return hbox({ filler(), text("last of data") | color(Color::GrayDark), filler() }); }));
            }
          };

          auto on_add = [&service] { 
            if (!n_name.empty()) { 
                service.AddContact(n_name, n_email); 
                n_name = GenerateRandomName(); 
                n_email = GenerateRandomEmail(n_name); 
            } 
          };

          auto name_input = Input(&n_name, "Name");
          auto email_input = Input(&n_email, "Email");
          auto add_btn = Button("[Add]", on_add, ButtonOption::Ascii());
          auto add_row_c = Container::Horizontal({name_input, email_input, add_btn});
          auto add_row = Renderer(add_row_c, [=] {
              // Align with columns
              return vbox({ 
                  separator(), 
                  hbox({
                      name_input->Render() | size(WIDTH, EQUAL, 28),
                      text(" "),
                      email_input->Render() | flex,
                      text(" "),
                      text("(Now)") | size(WIDTH, EQUAL, 16),
                      text(" "),
                      add_btn->Render() | size(WIDTH, EQUAL, 10) | center
                  }), 
                  separator() 
              });
          });

          auto on_activate = [=] { *show_config = true; };
          auto close_btn = Button("[Close]", on_exit, ButtonOption::Ascii());
          
          auto btn_base = Button("", on_activate, ButtonOption::Ascii());
          auto activate_btn = Renderer(btn_base, [=] {
              return hbox({
                  text("["),
                  text("A") | underlined,
                  text("ctivate]")
              }) | (btn_base->Focused() ? inverted : nothing);
          });
          
          // Layout structure: Header -> Body -> Footer
          auto main_container = Container::Vertical({ 
              header_final_renderer,
              rows_container, 
              add_row, 
              Container::Horizontal({ activate_btn, close_btn }) 
          });
          
          auto app_renderer = Renderer(main_container, [=, &service] { 
            Elements rows;
            rows.push_back(text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold);
            rows.push_back(separator());
            
            // Header is now part of main_container
            rows.push_back(header_final_renderer->Render());
            rows.push_back(separator());

            auto rendered_rows = rows_container->Render();
            rows.push_back(rendered_rows | vscroll_indicator | frame | flex);
            
            rows.push_back(add_row->Render());
            rows.push_back(hbox({ filler(), activate_btn->Render(), text(" "), close_btn->Render() }));
            return vbox(std::move(rows)) | border;
          });
          auto root = Renderer([=] {
              if (*show_config) return dbox({ app_renderer->Render() | color(Color::GrayDark), config_renderer->Render() | center });
              return app_renderer->Render();
          });
          refresh_ui(service.GetContacts());
          return CatchEvent(root, [=, &service, on_add, on_connect, on_cancel_config, on_activate](Event event) {
              if (event == Event::Custom) { refresh_ui(service.GetContacts()); return true; }
              
              // Shortcuts
              if (event == Event::Character('q') || event == Event::Escape) { 
                  if (*show_config) { on_cancel_config(); return true; }
                  on_exit(); return true; 
              }
              if (event == Event::Character('a')) { // 'a' to Activate
                  on_activate(); return true;
              }
              if (event == Event::Return) {
                  if (*show_config) { on_connect(); return true; }
                  on_add(); return true;
              }

              if (*show_config) return config_container->OnEvent(event);
              return main_container->OnEvent(event);
          });
      };

      auto screen = ScreenInteractive::Fullscreen();
      std::atomic<bool> started{false};
      FirestoreService service([&screen, &started] { if (started) screen.Post(Event::Custom); });
      int nAddr = Terminal::Size().dimy + 5;
      const char* key = std::getenv("FB_API_KEY");
      if (!key) key = std::getenv("API_KEY");
      if (key) service.Initialize(key, nAddr);
      auto root = build_ui(service, screen.ExitLoopClosure());
      started = true;
      screen.Loop(root);
  } catch (...) { return 1; }
  return 0;
}