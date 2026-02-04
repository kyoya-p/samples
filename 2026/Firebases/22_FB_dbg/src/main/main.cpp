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

// --- Helper Functions ---

Element MakeTableRow(Element name, Element email, Element time, Element op) {
    return hbox({
        std::move(name)  | size(WIDTH, EQUAL, 28),
        std::move(email) | flex,
        std::move(time)  | size(WIDTH, EQUAL, 16),
        std::move(op)    | size(WIDTH, EQUAL, 10) | center,
    });
}

std::string GenerateRandomName() {
  static const std::vector<std::string> f = {"Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Heidi"};
  static const std::vector<std::string> l = {"Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller"};
  static std::random_device rd;
  static std::mt19937 gen(rd());
  std::uniform_int_distribution<> d1(0, 7);
  std::uniform_int_distribution<> d2(0, 6);
  std::uniform_int_distribution<> d3(100, 999);
  std::uniform_int_distribution<> d_type(0, 2); 
  std::uniform_int_distribution<> d_char(0, 51);

  auto get_char = [&](int v) -> char {
      if (v < 26) return 'a' + v;
      return 'A' + (v - 26);
  };

  std::string prefix = "";
  int type = d_type(gen);
  if (type == 1) {
      prefix += get_char(d_char(gen));
      prefix += ". ";
  } else if (type == 2) {
      prefix += get_char(d_char(gen));
      prefix += get_char(d_char(gen));
      prefix += ". ";
  }

  return prefix + f[d1(gen)] + " " + std::to_string(d3(gen)) + " " + l[d2(gen)];
}

std::string GenerateRandomEmail(const std::string& name) {
  std::string e = name;
  std::transform(e.begin(), e.end(), e.begin(), [](unsigned char c) { return (unsigned char)::tolower(c); });
  std::replace(e.begin(), e.end(), ' ', '.');
  return e + "@example.com";
}

// --- Configuration Logic ---

std::string LoadApiKey() {
    const char* key = std::getenv("FB_API_KEY");
    if (!key) key = std::getenv("API_KEY");
    if (key) return std::string(key);

    std::string conf_path = GetExecutableDir() + "app.conf";
    std::ifstream conf_file(conf_path);
    std::string line;
    while (std::getline(conf_file, line)) {
        if (line.find("API_KEY=") == 0) {
            std::string api_key = line.substr(8);
            api_key.erase(0, api_key.find_first_not_of(" \t\r\n"));
            api_key.erase(api_key.find_last_not_of(" \t\r\n") + 1);
            return api_key;
        }
    }
    return "";
}

// --- Main Entry Point ---

int main(int argc, char** argv) {
  try {
      std::ofstream(GetLogFilename(), std::ios::trunc);
      firebase::SetLogLevel(firebase::kLogLevelError);

      auto screen = ScreenInteractive::Fullscreen();
      std::atomic<bool> started{false};
      FirestoreService service([&screen, &started]() mutable { if (started) screen.Post(Event::Custom); });
      
      int nAddr = std::max(10, Terminal::Size().dimy + 5);
      std::string api_key = LoadApiKey();
      if (!api_key.empty()) service.Initialize(api_key, "addressbook", nAddr);

      auto show_picker = std::make_shared<bool>(false);
      static std::string scan_new_email = "";

      // --- Address Book Components ---
      static std::string addr_filter_name = "";
      static std::string addr_filter_email = "";
      static int addr_sort_col = 2;
      static bool addr_sort_desc = true;

      auto addr_input_name = Input(&addr_filter_name, "Filter Name");
      auto addr_input_email = Input(&addr_filter_email, "Filter Email");

      auto rows_container = Container::Vertical({});
      auto rows_container_c = CatchEvent(rows_container, [&](Event e) {
          if (e.is_mouse() && (e.mouse().button == Mouse::WheelUp || e.mouse().button == Mouse::WheelDown)) {
              return rows_container->OnEvent(e.mouse().button == Mouse::WheelUp ? Event::ArrowUp : Event::ArrowDown);
          }
          return false;
      });

      size_t last_addr_rendered_count = 0;
      auto refresh_address_list = [&]() {
          service.SetSortOrder((addr_sort_col == 0) ? "name" : (addr_sort_col == 1) ? "email" : "timestamp", addr_sort_desc);
          service.SetFilter(addr_filter_name, addr_filter_email);

          size_t total = service.GetLoadedCount();
          if (total == last_addr_rendered_count && rows_container->ChildCount() > 0) return;
          
          rows_container->DetachAllChildren();
          last_addr_rendered_count = total;
          
          for (size_t i = 0; i < total; ++i) {
              int idx = (int)i;
              auto remove_btn = Button("[Remove]", [=, &service] { 
                  std::string id = service.GetId(idx);
                  if(!id.empty()) service.RemoveContact(id); 
              }, ButtonOption::Ascii());
    
              auto row = Renderer(remove_btn, [=, &service] {
                  auto el = MakeTableRow(text(service.GetData(idx, "name")), text(service.GetData(idx, "email")), text(service.GetData(idx, "timestamp")), remove_btn->Render());
                  if (idx % 2 != 0) el = el | bgcolor(Color::RGB(60, 60, 60));
                  if (remove_btn->Focused()) {
                      if (!service.IsLoading() && idx >= (int)service.GetLoadedCount() - 2 && service.HasMore()) service.LoadMore(10);
                      return el | inverted;
                  }
                  return el;
              });
              rows_container->Add(row);
          }
          if (service.HasMore() || service.IsLoading()) {
              rows_container->Add(Renderer([&service, nAddr] { 
                  if (!service.IsLoading() && (int)service.GetLoadedCount() < nAddr && service.HasMore()) service.LoadMore(10);
                  return hbox({ filler(), text("Loading...") | dim, filler() });
              }));
          }
      };

      static std::string n_name = GenerateRandomName();
      static std::string n_email = GenerateRandomEmail(n_name);
      auto name_input = Input(&n_name, "Name");
      auto email_input = Input(&n_email, "Email");
      auto add_btn = Button("[Add]", [&service] { if (!n_name.empty()) { service.AddContact(n_name, n_email); n_name = GenerateRandomName(); n_email = GenerateRandomEmail(n_name); } }, ButtonOption::Ascii());
      auto add_row = Renderer(Container::Horizontal({name_input, email_input, add_btn}), [=] {
          return vbox({ separator(), MakeTableRow(name_input->Render(), email_input->Render(), text("(Now)"), add_btn->Render()), separator() });
      });

      auto addr_btn_name = Button("Name", [&]{ if(addr_sort_col==0) addr_sort_desc=!addr_sort_desc; else {addr_sort_col=0; addr_sort_desc=false; addr_filter_email="";} screen.Post(Event::Custom); }, ButtonOption::Ascii());
      auto addr_btn_mail = Button("Mail", [&]{ if(addr_sort_col==1) addr_sort_desc=!addr_sort_desc; else {addr_sort_col=1; addr_sort_desc=false; addr_filter_name="";} screen.Post(Event::Custom); }, ButtonOption::Ascii());
      auto addr_btn_time = Button("Time", [&]{ if(addr_sort_col==2) addr_sort_desc=!addr_sort_desc; else {addr_sort_col=2; addr_sort_desc=true; addr_filter_name=""; addr_filter_email="";} screen.Post(Event::Custom); }, ButtonOption::Ascii());

      auto addr_input_name_c = CatchEvent(addr_input_name, [&](Event e){ if(addr_sort_col != 0) return false; bool ret = addr_input_name->OnEvent(e); if(ret) screen.Post(Event::Custom); return ret; });
      auto addr_input_email_c = CatchEvent(addr_input_email, [&](Event e){ if(addr_sort_col != 1) return false; bool ret = addr_input_email->OnEvent(e); if(ret) screen.Post(Event::Custom); return ret; });

      auto close_addr_btn = Button("[Close]", [&] { screen.Exit(); }, ButtonOption::Ascii());
      auto addr_renderer = Renderer(Container::Vertical({ Container::Horizontal({ addr_btn_name, addr_btn_mail, addr_btn_time, addr_input_name_c, addr_input_email_c }), rows_container_c, add_row, Container::Horizontal({ close_addr_btn }) }), [=, &service] {
          auto sort_ind = [&](int col) { return (addr_sort_col != col) ? text("") : text(addr_sort_desc ? " v" : " ^"); };
          auto render_input = [&](int col, Component input) {
              return hbox({ text(" ["), (addr_sort_col == col ? input->Render() : text("          ") | dim), text("]") });
          };
          return vbox({
              text("Address Book Setting") | bold | center, separator(),
              hbox({ hbox({ addr_btn_name->Render(), sort_ind(0), render_input(0, addr_input_name) }) | size(WIDTH, EQUAL, 28), separator(), hbox({ addr_btn_mail->Render(), sort_ind(1), render_input(1, addr_input_email) }) | flex, separator(), hbox({ addr_btn_time->Render(), sort_ind(2) }) | size(WIDTH, EQUAL, 16), text("          ") }),
              separator(), rows_container_c->Render() | vscroll_indicator | frame | flex, add_row->Render(),
              hbox({ text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold, filler(), close_addr_btn->Render() })
          }) | border;
      });

      // --- Main Loop ---
      auto final_component = CatchEvent(addr_renderer, [&](Event event) {
          if (event == Event::Custom) { refresh_address_list(); return true; }
          if (event == Event::Character("\x10")) {
              auto screen_capture = Screen::Create(Terminal::Size());
              Render(screen_capture, addr_renderer->Render());
              SaveSnapshot("addrapp", screen_capture.ToString());
              return true;
          }
          if ((event == Event::Character('q') || event == Event::Escape)) { screen.Exit(); return true; }
          return addr_renderer->OnEvent(event);
      });

      started = true;
      refresh_address_list();
      screen.Loop(final_component);
  } catch (const std::exception& e) {
      std::cerr << "EXCEPTION: " << e.what() << std::endl;
      return 1;
  }
  return 0;
}
