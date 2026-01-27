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

#include "firebase/log.h" 
#include "firebase/util.h" 

#include "dataaccess.hpp"
#include "utils.hpp"

#ifdef RGB
#undef RGB
#endif

using namespace ftxui;

enum class Screen {
    Main,
    AddressBook,
    ScanSend
};

Element MakeTableRow(Element name, Element email, Element time, Element op) {
    return hbox({
        std::move(name)  | size(WIDTH, EQUAL, 28),
        std::move(email) | flex,
        std::move(time)  | size(WIDTH, EQUAL, 16),
        std::move(op)    | size(WIDTH, EQUAL, 10) | center,
    });
}

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
      firebase::SetLogLevel(firebase::kLogLevelError);

      auto screen = ScreenInteractive::Fullscreen();
      std::atomic<bool> started{false};
      FirestoreService service([&screen, &started] { if (started) screen.Post(Event::Custom); });
      
      int nAddr = Terminal::Size().dimy + 5;
      const char* key = std::getenv("FB_API_KEY");
      if (!key) key = std::getenv("API_KEY");
      if (key) service.Initialize(key, nAddr);

      auto active_screen = Screen::Main;
      auto show_config = std::make_shared<bool>(false);
      auto show_picker = std::make_shared<bool>(false);
      auto picking_index = std::make_shared<int>(-1);

      // --- Address Book Screen Components ---
      auto rows_container = Container::Vertical({});
      auto refresh_address_list = [=, &service](const std::vector<Contact>& contacts) {
        rows_container->DetachAllChildren();
        for (size_t i = 0; i < contacts.size(); ++i) {
          const auto& contact = contacts[i];
          auto contact_id = contact.id;
          auto contact_name = contact.name;
          auto contact_email = contact.email;
          auto contact_time = contact.timestamp;
          int idx = (int)i;

          auto remove_btn = Button("[Remove]", [=, &service] { service.RemoveContact(contact_id); }, ButtonOption::Ascii());
          auto row = Renderer(remove_btn, [=, &service] {
            auto el = MakeTableRow(text(contact_name), text(contact_email), text(contact_time), remove_btn->Render());
            if (idx % 2 != 0) el = el | bgcolor(Color::RGB(60, 60, 60));
            if (remove_btn->Focused()) return el | inverted;
            return el;
          });
          rows_container->Add(row);
        }
        if (service.HasMore()) {
            auto loader = Renderer([] { return hbox({ filler(), text("Loading...") | color(Color::Yellow), filler() }); });
            rows_container->Add(std::make_shared<VisibilityObserver>(loader, [&service] { service.LoadMore(20); }));
        }
      };

      static std::string n_name = GenerateRandomName();
      static std::string n_email = GenerateRandomEmail(n_name);
      auto name_input = Input(&n_name, "Name");
      auto email_input = Input(&n_email, "Email");
      auto add_btn = Button("[Add]", [&service] { if (!n_name.empty()) { service.AddContact(n_name, n_email); n_name = GenerateRandomName(); n_email = GenerateRandomEmail(n_name); } }, ButtonOption::Ascii());
      auto add_row_c = Container::Horizontal({name_input, email_input, add_btn});
      auto add_row = Renderer(add_row_c, [=] {
          return vbox({ 
              separator(), 
              MakeTableRow(name_input->Render(), email_input->Render(), text("(Now)"), add_btn->Render()), 
              separator() 
          });
      });

      auto close_addr_btn = Button("[Close]", [&] { active_screen = Screen::Main; }, ButtonOption::Ascii());
      auto activate_btn = Button("[Activate]", [=] { *show_config = true; }, ButtonOption::Ascii());
      auto addr_nav_container = Container::Horizontal({ activate_btn, close_addr_btn });
      auto addr_main_container = Container::Vertical({ rows_container, add_row, addr_nav_container });

      auto addr_renderer = Renderer(addr_main_container, [=, &service] {
          Elements rows;
          rows.push_back(text("Address Book Setting") | bold | center);
          rows.push_back(separator());
          rows.push_back(vbox({ MakeTableRow(text("Name") | bold, text("Mail") | bold, text("Time") | bold, text("Op") | bold), separator() }));
          rows.push_back(rows_container->Render() | vscroll_indicator | frame | flex);
          rows.push_back(add_row->Render());
          rows.push_back(hbox({ text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold, filler(), activate_btn->Render(), text(" "), close_addr_btn->Render() }));
          return vbox(std::move(rows)) | border;
      });

      // --- Picker Dialog (for Scan & Send) ---
      auto picker_list_container = Container::Vertical({});
      auto refresh_picker_list = [=, &service](std::function<void(std::string)> on_pick) {
          picker_list_container->DetachAllChildren();
          auto contacts = service.GetContacts();
          for (const auto& contact : contacts) {
              auto email = contact.email;
              auto name = contact.name;
              picker_list_container->Add(Button(name + " <" + email + ">", [=] { on_pick(email); }, ButtonOption::Ascii()));
          }
      };

      auto cancel_picker_btn = Button("[Cancel]", [=] { *show_picker = false; }, ButtonOption::Ascii());
      auto picker_renderer = Renderer(Container::Vertical({ picker_list_container, cancel_picker_btn }), [=] {
          return vbox({
              text("Select Address") | bold | center,
              separator(),
              picker_list_container->Render() | vscroll_indicator | frame | size(HEIGHT, EQUAL, 10),
              separator(),
              cancel_picker_btn->Render() | center
          }) | border | center | bgcolor(Color::Blue) | size(WIDTH, GREATER_THAN, 50);
      });

      // --- Scan & Send Screen Components ---
      struct DestRow {
          std::string email;
          Component input;
          Component select_btn;
          Component remove_btn;
          Component container;
      };
      static std::vector<std::shared_ptr<DestRow>> scan_rows;
      auto scan_list_container = Container::Vertical({});

      auto add_scan_row = [&](std::string initial_email = "") {
          auto row = std::make_shared<DestRow>();
          row->email = initial_email;
          row->input = Input(&row->email, "Address");
          
          int idx = (int)scan_rows.size();
          row->select_btn = Button("[Select]", [=] { 
              *picking_index = idx;
              *show_picker = true;
          }, ButtonOption::Ascii());

          row->remove_btn = Button("[Remove]", [=] {
              auto it = std::find_if(scan_rows.begin(), scan_rows.end(), [&](const std::shared_ptr<DestRow>& r) { return r.get() == row.get(); });
              if (it != scan_rows.end()) {
                  scan_rows.erase(it);
                  screen.Post(Event::Custom);
              }
          }, ButtonOption::Ascii());

          row->container = Container::Horizontal({ row->input, row->select_btn, row->remove_btn });
          scan_rows.push_back(row);
          return row;
      };

      auto refresh_scan_ui = [&]() {
          scan_list_container->DetachAllChildren();
          for (auto& row : scan_rows) {
              auto renderer = Renderer(row->container, [row] {
                  return hbox({
                      text("- "),
                      row->input->Render() | flex,
                      row->select_btn->Render(),
                      row->remove_btn->Render()
                  });
              });
              scan_list_container->Add(renderer);
          }
      };

      if (scan_rows.empty()) add_scan_row();

      auto add_row_btn = Button("(+)", [&] { add_scan_row(); refresh_scan_ui(); }, ButtonOption::Ascii());
      auto send_btn = Button("[Send]", [&] {
          std::string log_msg = "Scan & Send: Sending to ";
          for (auto& r : scan_rows) if (!r->email.empty()) log_msg += r->email + ", ";
          Log(log_msg);
          scan_rows.clear();
          add_scan_row();
          refresh_scan_ui();
      }, ButtonOption::Ascii());
      auto back_btn = Button("[Back]", [&] { active_screen = Screen::Main; }, ButtonOption::Ascii());

      auto scan_main_container = Container::Vertical({ scan_list_container, add_row_btn, Container::Horizontal({send_btn, back_btn}) });
      auto scan_renderer = Renderer(scan_main_container, [&] {
          return vbox({
              text("Scan & Send") | bold | center,
              separator(),
              text("To:"),
              scan_list_container->Render() | vscroll_indicator | frame | flex,
              add_row_btn->Render(),
              filler(),
              separator(),
              hbox({ filler(), send_btn->Render(), text(" "), back_btn->Render() })
          }) | border;
      });

      // --- Main Menu Screen ---
      auto btn_scan = Button("Scan & Send", [&] { active_screen = Screen::ScanSend; }, ButtonOption::Ascii());
      auto btn_addr = Button("Address Book Edit", [&] { active_screen = Screen::AddressBook; }, ButtonOption::Ascii());
      auto btn_exit = Button("[Exit]", screen.ExitLoopClosure(), ButtonOption::Ascii());
      
      auto menu_container = Container::Vertical({ btn_scan, btn_addr, btn_exit });
      auto main_menu_renderer = Renderer(menu_container, [&] {
          auto make_box = [&](Component c, std::string label) {
              auto element = c->Render() | center | size(WIDTH, EQUAL, 31) | size(HEIGHT, EQUAL, 3);
              if (c->Focused()) element = element | inverted;
              return window(text(""), element);
          };

          return vbox({
              text("QuantumRoast Scanner") | bold | center,
              separator(),
              filler(),
              make_box(btn_scan, "Scan & Send") | center,
              text(""),
              make_box(btn_addr, "Address Book Edit") | center,
              filler(),
              hbox({ filler(), btn_exit->Render() }),
          }) | border;
      });

      // --- Configuration Dialog ---
      static std::string api_key_input_val = "";
      auto key_input = Input(&api_key_input_val, "API Key");
      auto config_connect_btn = Button("[Connect]", [=, &service] { if (service.Initialize(api_key_input_val, 20)) { *show_config = false; } }, ButtonOption::Ascii());
      auto config_cancel_btn = Button("[Cancel]", [=] { *show_config = false; }, ButtonOption::Ascii());
      auto config_container = Container::Vertical({ key_input, Container::Horizontal({ config_connect_btn, config_cancel_btn }) | center });
      auto config_renderer = Renderer(config_container, [=, &service] {
          return vbox({ 
              text("Configuration") | bold | center, 
              separator(), 
              hbox(text("API Key: "), key_input->Render()) | border, 
              hbox(config_connect_btn->Render(), config_cancel_btn->Render()) | center, 
              text(service.GetError()) | color(Color::Red) | center 
          }) | center | border | size(WIDTH, GREATER_THAN, 60);
      });

      // --- Root Router ---
      auto root_container = Container::Tab({
          main_menu_renderer,
          addr_renderer,
          scan_renderer
      }, (int*)&active_screen);

      auto root_renderer = Renderer(root_container, [&] {
          Element content;
          if (active_screen == Screen::Main) content = main_menu_renderer->Render();
          else if (active_screen == Screen::AddressBook) content = addr_renderer->Render();
          else if (active_screen == Screen::ScanSend) content = scan_renderer->Render();

          if (*show_config) return dbox({ content | color(Color::GrayDark), config_renderer->Render() | center });
          if (*show_picker) return dbox({ content | color(Color::GrayDark), picker_renderer->Render() | center });
          return content;
      });

      refresh_address_list(service.GetContacts());
      refresh_scan_ui();

      auto final_component = CatchEvent(root_renderer, [&](Event event) {
          if (event == Event::Custom) {
              refresh_address_list(service.GetContacts());
              refresh_picker_list([&](std::string email) {
                  if (*picking_index >= 0 && *picking_index < (int)scan_rows.size()) {
                      scan_rows[*picking_index]->email = email;
                  }
                  *show_picker = false;
                  screen.Post(Event::Custom);
              });
              refresh_scan_ui();
              return true;
          }
          if (event == Event::Character('q') && !*show_config && !*show_picker) {
              if (active_screen == Screen::Main) screen.Exit();
              else active_screen = Screen::Main;
              return true;
          }
          if (*show_config) return config_container->OnEvent(event);
          if (*show_picker) return picker_renderer->OnEvent(event);
          return root_container->OnEvent(event);
      });

      started = true;
      screen.Loop(final_component);

  } catch (const std::exception& e) {
      std::cerr << "EXCEPTION: " << e.what() << std::endl;
      return 1;
  } catch (...) {
      return 1;
  }
  return 0;
}
