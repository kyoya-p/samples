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

enum AppScreen {
    AppMain,
    AppAddressBook,
    AppScanSend
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
      FirestoreService service([&screen, &started]() mutable { if (started) screen.Post(Event::Custom); });
      
      int nAddr = Terminal::Size().dimy + 5;
      std::string api_key_str;
      const char* key = std::getenv("FB_API_KEY");
      if (!key) key = std::getenv("API_KEY");
      if (key) {
          api_key_str = key;
      } else {
          std::ifstream conf_file("app.conf");
          std::string line;
          while (std::getline(conf_file, line)) {
              if (line.find("API_KEY=") == 0) {
                  api_key_str = line.substr(8);
                  api_key_str.erase(0, api_key_str.find_first_not_of(" \t\r\n"));
                  api_key_str.erase(api_key_str.find_last_not_of(" \t\r\n") + 1);
                  break;
              }
          }
      }
      if (!api_key_str.empty()) {
        if (service.Initialize(api_key_str, nAddr)) {
            std::ofstream conf_out("app.conf");
            conf_out << "API_KEY=" << api_key_str << std::endl;
        }
      }

      auto active_screen = AppMain;
      auto show_picker = std::make_shared<bool>(false);
      auto picking_index = std::make_shared<int>(-1);

      static std::string scan_new_email = "";
      static std::vector<std::string> scan_confirmed_emails;

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

      auto close_addr_btn = Button("[Close]", [&] { active_screen = AppMain; }, ButtonOption::Ascii());
      auto addr_nav_container = Container::Horizontal({ close_addr_btn });
      auto addr_main_container = Container::Vertical({ rows_container, add_row, addr_nav_container });

      auto addr_renderer = Renderer(addr_main_container, [=, &service] {
          Elements rows;
          rows.push_back(text("Address Book Setting") | bold | center);
          rows.push_back(separator());
          rows.push_back(vbox({ MakeTableRow(text("Name") | bold, text("Mail") | bold, text("Time") | bold, text("Op") | bold), separator() }));
          rows.push_back(rows_container->Render() | vscroll_indicator | frame | flex);
          rows.push_back(add_row->Render());
          std::string err = service.GetError();
          auto status_txt = text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold;
          if (!err.empty()) status_txt = hbox({ status_txt, text(" " + err) | color(Color::Red) });

          rows.push_back(hbox({ status_txt, filler(), close_addr_btn->Render() }));
          return vbox(std::move(rows)) | border;
      });

      // --- Picker Dialog (for Scan & Send) ---
      static std::string p_filter_name = "";
      static std::string p_filter_email = "";
      static int p_sort_col = 2; // 0:Name, 1:Email, 2:Time
      static bool p_sort_desc = true; // Default Descending for Time
      
      auto p_input_name = Input(&p_filter_name, "Filter Name");
      auto p_input_email = Input(&p_filter_email, "Filter Email");
      
      auto p_list_container = Container::Vertical({});
      
      auto update_picker_list = [&, service_ptr=&service]() {
          p_list_container->DetachAllChildren();
          auto contacts = service_ptr->GetContacts();
          
          // Filter (Only apply filter for the active column if applicable)
          auto it = std::remove_if(contacts.begin(), contacts.end(), [&](const Contact& c){
              // If sorting by name (0), filter by name. If email (1), filter by email.
              if (p_sort_col == 0 && !p_filter_name.empty() && c.name.find(p_filter_name) == std::string::npos) return true;
              if (p_sort_col == 1 && !p_filter_email.empty() && c.email.find(p_filter_email) == std::string::npos) return true;
              return false;
          });
          contacts.erase(it, contacts.end());

          // Sort
          std::sort(contacts.begin(), contacts.end(), [&](const Contact& a, const Contact& b){
              bool res = false;
              if (p_sort_col == 0) res = a.name < b.name;
              else if (p_sort_col == 1) res = a.email < b.email;
              else res = a.timestamp < b.timestamp;
              return p_sort_desc ? !res : res;
          });

          for (const auto& contact : contacts) {
              auto email = contact.email;
              auto name = contact.name;
              
              std::string label_name = name;
              if (label_name.length() > 20) label_name = label_name.substr(0, 20);
              else label_name.resize(20, ' ');

              std::string label_email = email;
              if (label_email.length() > 30) label_email = label_email.substr(0, 30);
              else label_email.resize(30, ' ');

              p_list_container->Add(Button(label_name + " " + label_email + " " + contact.timestamp, [&, email] { 
                  scan_new_email = email;
                  *show_picker = false;
                  // Reset filters
                  p_filter_name = ""; p_filter_email = "";
              }, ButtonOption::Ascii()));
          }
      };

      auto p_btn_name = Button("Name", [&]{ 
          if(p_sort_col==0) p_sort_desc=!p_sort_desc; 
          else {p_sort_col=0; p_sort_desc=false; p_filter_email="";} // Switch to Name, reset others
          update_picker_list(); 
      }, ButtonOption::Ascii());
      
      auto p_btn_mail = Button("Mail", [&]{ 
          if(p_sort_col==1) p_sort_desc=!p_sort_desc; 
          else {p_sort_col=1; p_sort_desc=false; p_filter_name="";} 
          update_picker_list(); 
      }, ButtonOption::Ascii());
      
      auto p_btn_time = Button("Time", [&]{ 
          if(p_sort_col==2) p_sort_desc=!p_sort_desc; 
          else {p_sort_col=2; p_sort_desc=true; p_filter_name=""; p_filter_email="";} // Default Time Desc
          update_picker_list(); 
      }, ButtonOption::Ascii());

      auto p_cancel_btn = Button("[Cancel]", [=] { *show_picker = false; }, ButtonOption::Ascii());
      
      // Wrap inputs to trigger update
      auto p_input_name_c = CatchEvent(p_input_name, [&](Event e){ 
          bool ret = p_input_name->OnEvent(e); 
          if(ret) update_picker_list(); 
          return ret; 
      });
      auto p_input_email_c = CatchEvent(p_input_email, [&](Event e){ 
          bool ret = p_input_email->OnEvent(e); 
          if(ret) update_picker_list(); 
          return ret; 
      });

      // Dynamic header composition based on selected column
      auto p_main_container_gen = Container::Vertical({
          Container::Horizontal({ p_btn_name, p_btn_mail, p_btn_time, p_input_name_c, p_input_email_c }), // Flattened for registration
          p_list_container,
          p_cancel_btn
      });

      auto picker_renderer = Renderer(p_main_container_gen, [=, &service] {
          auto sort_indicator = [&](int col) {
              if (p_sort_col != col) return text("  "); // No indicator if not selected
              return text(p_sort_desc ? " v" : " ^");
          };

          Element input_area = text("");
          if (p_sort_col == 0) input_area = p_input_name->Render();
          else if (p_sort_col == 1) input_area = p_input_email->Render();
          else input_area = text("                "); // Placeholder for time

          return vbox({
              text("Address Book") | bold | center,
              separator(),
              hbox({
                  p_btn_name->Render(), sort_indicator(0), text(" | "),
                  p_btn_mail->Render(), sort_indicator(1), text(" | "),
                  p_btn_time->Render(), sort_indicator(2)
              }),
              separator(),
              hbox({ text("Search: "), input_area }) | size(HEIGHT, EQUAL, 1), 
              separator(),
              p_list_container->Render() | vscroll_indicator | frame | size(HEIGHT, EQUAL, 10),
              separator(),
              hbox({ text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold, filler(), p_cancel_btn->Render() })
          }) | border | bgcolor(Color::Blue) | size(WIDTH, GREATER_THAN, 80) | clear_under;
      });

      
      // Initial update
      // update_picker_list(); // Call this when showing picker or initially? 
      // We will call it in the refresh_picker_list lambda replacement.
      
      auto refresh_picker_list_stub = [&](){ update_picker_list(); };

      // --- Scan & Send Screen Components ---
      auto scan_list_container = Container::Vertical({});

      // New Entry Row Components
      auto new_email_input = Input(&scan_new_email, "Address");
      auto new_addr_btn = Button("[Address Book]", [&] { 
          p_filter_name = "";
          p_filter_email = "";
          update_picker_list();
          *show_picker = true;
      }, ButtonOption::Ascii());
      
      // Forward declaration for refresh
      std::function<void()> refresh_scan_ui;

      auto new_enter_btn = Button("[Enter]", [&] {
          if (!scan_new_email.empty()) {
              scan_confirmed_emails.push_back(scan_new_email);
              scan_new_email = "";
              refresh_scan_ui();
          }
      }, ButtonOption::Ascii());

      auto new_row_container = Container::Horizontal({
          Renderer([]{ return text("- "); }),
          Renderer(new_email_input, [&] { return new_email_input->Render() | border | flex; }) | flex,
          Renderer([]{ return text(" "); }),
          new_addr_btn,
          Renderer([]{ return text(" "); }),
          new_enter_btn
      });

      refresh_scan_ui = [&]() {
          scan_list_container->DetachAllChildren();
          for (int i = 0; i < (int)scan_confirmed_emails.size(); ++i) {
              std::string email = scan_confirmed_emails[i];
              auto remove_btn = Button("[Remove]", [=, &refresh_scan_ui] {
                   // Capture by value/copy index is tricky if list changes, but here we redraw on every change.
                   // Finding by value is safer or just rebuilding properly.
                   auto it = std::find(scan_confirmed_emails.begin(), scan_confirmed_emails.end(), email);
                   if (it != scan_confirmed_emails.end()) {
                       scan_confirmed_emails.erase(it);
                       refresh_scan_ui();
                   }
              }, ButtonOption::Ascii());

              auto row = Container::Horizontal({
                  Renderer([email]{ return text("- " + email) | flex; }),
                  remove_btn
              });
              scan_list_container->Add(row);
          }
      };
      
      // Initial refresh
      refresh_scan_ui();

      // Hook to auto-refresh if new email input needs it? 
      // Actually, if we pick from address book, scan_new_email updates, but Input component refers to it by pointer, so it renders new value automatically.
      // But we might want to focus it?
      
      auto send_btn = Button("[Send]", [&] {
          scan_confirmed_emails.clear();
          refresh_scan_ui();
      }, ButtonOption::Ascii());
      auto back_btn = Button("[Back]", [&] { active_screen = AppMain; }, ButtonOption::Ascii());

      auto scan_main_container = Container::Vertical({
          Renderer([]{ return text("To:"); }),
          new_row_container,
          scan_list_container | flex, 
          Container::Horizontal({ Renderer([]{ return filler(); }), send_btn, Renderer([]{ return text("      "); }), back_btn }) 
      });

      auto scan_renderer = Renderer(scan_main_container, [&] {
          return vbox({
              text("Scan & Send") | bold,
              separator(),
              scan_main_container->Render() | flex
          }) | border;
      });


      // --- Main Menu Screen ---
      auto btn_scan = Button("Scan & Send", [&] { active_screen = AppScanSend; }, ButtonOption::Ascii());
      auto btn_addr = Button("Address Book Edit", [&] { active_screen = AppAddressBook; }, ButtonOption::Ascii());
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
              separator(),
              hbox({ text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold, filler(), btn_exit->Render() }),
          }) | border;
      });

      // --- Root Router ---
      auto root_container = Container::Tab({
          main_menu_renderer,
          addr_renderer,
          scan_renderer
      }, (int*)&active_screen);

      auto root_renderer = Renderer(root_container, [&] {
          Element content;
          if (active_screen == AppMain) content = main_menu_renderer->Render();
          else if (active_screen == AppAddressBook) content = addr_renderer->Render();
          else if (active_screen == AppScanSend) content = scan_renderer->Render();

          if (*show_picker) return dbox({ content | color(Color::GrayDark), picker_renderer->Render() | center });
          return content;
      });

      refresh_address_list(service.GetContacts());
      refresh_scan_ui();

      auto final_component = CatchEvent(root_renderer, [&](Event event) {
          if (event == Event::Custom) {
              refresh_address_list(service.GetContacts());
              update_picker_list();
              refresh_scan_ui();
              return true;
          }
          if (event == Event::Character('q') && !*show_picker) {
              if (active_screen == AppMain) screen.Exit();
              else active_screen = AppMain;
              return true;
          }
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
