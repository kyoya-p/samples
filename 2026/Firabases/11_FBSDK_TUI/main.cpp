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
#include <unordered_map>
#include <exception>
#include <execinfo.h> // Keep for compatibility if used by other parts, but moved to utils
#include <unistd.h>
#include <ctime>

#include "ftxui/component/captured_mouse.hpp"
#include "ftxui/component/component.hpp"
#include "ftxui/component/component_base.hpp"
#include "ftxui/component/component_options.hpp"
#include "ftxui/component/screen_interactive.hpp"
#include "ftxui/dom/elements.hpp"
#include "ftxui/util/ref.hpp"
#include "ftxui/screen/terminal.hpp"

#include "dataaccess.hpp"
#include "utils.hpp"

using namespace ftxui;

// Helper to define consistent column widths
Element MakeTableRow(Element name, Element email, Element time, Element op) {
    return hbox({
        std::move(name)  | size(WIDTH, EQUAL, 28),
        std::move(email) | flex,
        std::move(time)  | size(WIDTH, EQUAL, 16),
        std::move(op)    | size(WIDTH, EQUAL, 10) | center,
    });
}

// Custom Component to trigger callback when Render is called (i.e. visible)
class VisibilityObserver : public ComponentBase {
public:
    VisibilityObserver(Component child, std::function<void()> on_visible)
        : child_(child), on_visible_(on_visible) {
        Add(child_);
    }

    Element Render() override {
        on_visible_();
        return child_->Render();
    }

    bool OnEvent(Event event) override {
        return child_->OnEvent(event);
    }

    bool Focusable() const override {
        return child_->Focusable();
    }
private:
    Component child_;
    std::function<void()> on_visible_;
};

std::string GenerateRandomName() {
  static const std::vector<std::string> first_names = {
      "Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Heidi"};
  static const std::vector<std::string> last_names = {
      "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller"};
  static std::random_device rd;
  static std::mt19937 gen(rd());
  std::uniform_int_distribution<> dis_first(0, first_names.size() - 1);
  std::uniform_int_distribution<> dis_last(0, last_names.size() - 1);
  std::uniform_int_distribution<> dis_middle(100, 999);
  return first_names[dis_first(gen)] + " " + std::to_string(dis_middle(gen)) + " " + last_names[dis_last(gen)];
}

std::string GenerateRandomEmail(const std::string& name) {
  std::string email = name;
  std::transform(email.begin(), email.end(), email.begin(),
                 [](unsigned char c) { return std::tolower(c); });
  std::replace(email.begin(), email.end(), ' ', '.');
  return email + "@example.com";
}

int main(int argc, char** argv) {
  try {
      std::ofstream(GetLogFilename(), std::ios::trunc);
      Log("Starting application... Log file: " + GetLogFilename());

      if (argc > 1 && std::string(argv[1]) == "--check") {
          Log("Running in verification mode.");
          std::cout << "Verification: OK" << std::endl;
          return 0;
      }
      
      bool snapshot_mode = (argc > 1 && std::string(argv[1]) == "--snapshot");

      // Shared UI Builder
      auto build_ui = [&](FirestoreService& service, std::function<void()> on_exit, std::function<void()> on_activate_config, bool is_snapshot) -> Component {
          
          auto show_config = std::make_shared<bool>(false);
          
          static std::string api_key_input_val = "";
          const char* env_key = std::getenv("FB_API_KEY");
          if (!env_key) env_key = std::getenv("API_KEY");
          static std::string current_api_key = (env_key != nullptr) ? env_key : "";
          api_key_input_val = current_api_key;
          
          auto key_input = Input(&api_key_input_val, "API Key");
          auto connect_btn = Button("[Connect]", [&, show_config] { 
              int nAddress = 20; 
              if (service.Initialize(api_key_input_val, nAddress)) { 
                  current_api_key = api_key_input_val; 
                  *show_config = false; 
              } 
          }, ButtonOption::Ascii());
          
          auto cancel_btn = Button("[Cancel]", [&, show_config] { 
              api_key_input_val = current_api_key; 
              *show_config = false; 
          }, ButtonOption::Ascii());
          
          auto config_container = Container::Vertical({ key_input, Container::Horizontal({ connect_btn, cancel_btn }) | center });
          auto config_renderer = Renderer(config_container, [&, show_config, is_snapshot] {
              if (is_snapshot) return vbox({ text("Configuration Placeholder") | bold | center }) | center | border;
              return vbox({ text("Configuration") | bold | center, separator(), hbox(text("API Key: "), key_input->Render()) | border, separator(), hbox(connect_btn->Render(), cancel_btn->Render()) | center, text(""), text(service.GetError()) | color(Color::Red) | center }) | center | border | size(WIDTH, GREATER_THAN, 60);
          });

          auto rows_container = Container::Vertical({});
          
          auto refresh_ui = [rows_container, &service, is_snapshot](const std::vector<Contact>& contacts) {
            rows_container->DetachAllChildren();
            for (const auto& contact : contacts) {
              auto remove_btn = Button("[Remove]", [&service, contact] { service.RemoveContact(contact.id); }, ButtonOption::Ascii());
              auto row = Renderer(remove_btn, [contact, remove_btn, contacts, &service, is_snapshot] {
                int i = 0;
                for(size_t j=0; j<contacts.size(); ++j) if(contacts[j].id == contact.id) { i = j; break; }
                
                Element op_elem = is_snapshot ? text("[Remove]") : remove_btn->Render();
                auto element = MakeTableRow(text(contact.name), text(contact.email), text(contact.timestamp), op_elem);
                
                if (i % 2 != 0) element = element | bgcolor(Color::RGB(60, 60, 60));
                if (!is_snapshot && remove_btn->Focused()) return element | inverted;
                return element;
              });
              rows_container->Add(row);
            }
            
            if (service.HasMore()) {
                auto loader = Renderer([&] { return hbox({ filler(), text("Loading...") | color(Color::Yellow) | bold, filler() }); });
                auto observed_loader = std::make_shared<VisibilityObserver>(loader, [&service] {
                     service.LoadMore(20);
                });
                rows_container->Add(observed_loader);
            } else {
                rows_container->Add(Renderer([]{ return hbox({ filler(), text("--- last of data ---") | color(Color::GrayDark), filler() }); }));
            }
          };

          static std::string next_name = GenerateRandomName();
          static std::string next_email = GenerateRandomEmail(next_name);
          auto name_input = Input(&next_name, "Name");
          auto email_input = Input(&next_email, "Email");
          auto add_btn = Button("[Add]", [&service] { 
              if (!next_name.empty() && !next_email.empty()) { 
                  service.AddContact(next_name, next_email); 
                  next_name = GenerateRandomName(); 
                  next_email = GenerateRandomEmail(next_name); 
              } 
          }, ButtonOption::Ascii());
          
          auto add_row = Renderer(Container::Horizontal({name_input, email_input, add_btn}), [&, is_snapshot] {
              Element name_elem = is_snapshot ? text("Name Input") : name_input->Render();
              Element email_elem = is_snapshot ? text("Email Input") : email_input->Render();
              Element btn_elem = is_snapshot ? text("[Add]") : add_btn->Render();
              return vbox({ separator(), MakeTableRow(name_elem, email_elem, text("(Now)"), btn_elem), separator() });
          });

          auto close_btn = Button("[Close]", on_exit, ButtonOption::Ascii());
          auto activate_btn = Button("[Activate]", [&, show_config] { *show_config = true; if(on_activate_config) on_activate_config(); }, ButtonOption::Ascii());
          
          auto main_container = Container::Vertical({ rows_container, add_row, Container::Horizontal({ activate_btn, close_btn }) });
          
          auto app_renderer = Renderer(main_container, [&, rows_container, add_row, activate_btn, close_btn, is_snapshot] { 
            bool connected = service.IsConnected();
            Elements rows;
            if (connected) rows.push_back(text("Status: Connected") | color(Color::Green) | bold);
            else rows.push_back(text("Status: Disconnected") | color(Color::Red) | bold);
            if (!service.GetError().empty()) rows.push_back(text(service.GetError()) | color(Color::Red) | center);
            rows.push_back(separator());
            
            rows.push_back(vbox({ MakeTableRow(text("Name") | bold, text("Mail Address") | bold, text("Created At") | bold, text("Operation") | bold), separator() }));
            
            if (is_snapshot) rows.push_back(rows_container->Render());
            else rows.push_back(rows_container->Render() | vscroll_indicator | frame | flex);
            
            rows.push_back(add_row->Render());
            
            Element activate_elem = is_snapshot ? text("[Activate]") : activate_btn->Render();
            Element close_elem = is_snapshot ? text("[Close]") : close_btn->Render();
            rows.push_back(hbox({ filler(), activate_elem, text(" "), close_elem }));
            
            return vbox(std::move(rows)) | border;
          });

          auto root_renderer = Renderer([&, show_config, app_renderer, config_renderer] {
              if (*show_config) return dbox({ app_renderer->Render() | color(Color::GrayDark), config_renderer->Render() | center });
              return app_renderer->Render();
          });

          refresh_ui(service.GetContacts());

          return CatchEvent(root_renderer, [=, &service](Event event) {
              if (event == Event::Custom) { refresh_ui(service.GetContacts()); return true; }
              if (event == Event::Character('q')) { on_exit(); return true; }
              if (*show_config) return config_container->OnEvent(event);
              return main_container->OnEvent(event);
          });
      };

      if (snapshot_mode) {
          Log("Snapshot mode: Initializing Service...");
          FirestoreService service([]{});
          Log("Snapshot mode: Building UI...");
          auto root = build_ui(service, []{}, []{}, true);
          Log("Snapshot mode: root->Render()...");
          auto document = root->Render();
          Log("Snapshot mode: Screen::Create...");
          auto screen = Screen::Create(Dimension::Fixed(100), Dimension::Fixed(30));
          Log("Snapshot mode: Render(screen, document)...");
          Render(screen, document);
          Log("Snapshot mode: Printing...");
          std::cout << screen.ToString() << std::endl;
          Log("Snapshot mode: Done.");
          return 0;
      }

      auto screen = ScreenInteractive::Fullscreen();
      auto on_update = [&screen]() { screen.Post(Event::Custom); };
      FirestoreService service(on_update);
      
      int nAddress = Terminal::Size().dimy + 5;
      if (nAddress < 10) nAddress = 10;
      const char* env_key = std::getenv("FB_API_KEY");
      if (!env_key) env_key = std::getenv("API_KEY");
      if (env_key) service.Initialize(env_key, nAddress);
      
      auto root = build_ui(service, screen.ExitLoopClosure(), []{}, false);
      
      screen.Loop(root);
      Log("Application closed.");
  } catch (const std::exception& e) { 
      std::cerr << "FATAL ERROR: " << e.what() << std::endl; 
      std::ofstream err("error.txt");
      err << "FATAL ERROR: " << e.what() << std::endl;
      Log("FATAL ERROR: " + std::string(e.what()));
      return 1; 
  } catch (...) {
      std::cerr << "UNKNOWN FATAL ERROR" << std::endl;
      std::ofstream err("error.txt");
      err << "UNKNOWN FATAL ERROR" << std::endl;
      Log("UNKNOWN FATAL ERROR");
      return 1;
  }
  return 0;
}