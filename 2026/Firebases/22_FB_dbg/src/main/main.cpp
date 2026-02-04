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

struct AppState {
    std::string api_key;
    int nAddr;
    std::atomic<bool> started{false};
};

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
  std::uniform_int_distribution<> d1(0, 7), d2(0, 6), d3(100, 999), d_type(0, 2), d_char(0, 51);
  auto get_char = [&](int v) { return (char)(v < 26 ? 'a' + v : 'A' + (v - 26)); };
  std::string prefix = "";
  int type = d_type(gen);
  if (type > 0) { for(int i=0; i<type; ++i) prefix += get_char(d_char(gen)); prefix += ". "; }
  return prefix + f[d1(gen)] + " " + std::to_string(d3(gen)) + " " + l[d2(gen)];
}

std::string GenerateRandomEmail(const std::string& name) {
  std::string e = name;
  std::transform(e.begin(), e.end(), e.begin(), [](unsigned char c) { return (unsigned char)::tolower(c); });
  std::replace(e.begin(), e.end(), ' ', '.');
  return e + "@example.com";
}

std::string LoadApiKey() {
    const char* key = std::getenv("FB_API_KEY");
    if (!key) key = std::getenv("API_KEY");
    if (key) return std::string(key);
    std::string conf_path = GetExecutableDir() + "app.conf";
    std::ifstream conf_file(conf_path);
    std::string line;
    while (std::getline(conf_file, line)) {
        if (line.find("API_KEY=") == 0) {
            std::string k = line.substr(8);
            k.erase(0, k.find_first_not_of(" \t\r\n"));
            k.erase(k.find_last_not_of(" \t\r\n") + 1);
            return k;
        }
    }
    return "";
}

void RefreshAddressList(FirestoreService& service, Component rows, int sort_col, bool sort_desc, std::string f_name, std::string f_email, size_t& last_count, int nAddr) {
    service.SetSortOrder((sort_col == 0) ? "name" : (sort_col == 1) ? "email" : "timestamp", sort_desc);
    service.SetFilter(f_name, f_email);
    size_t total = service.GetLoadedCount();
    if (total == last_count && rows->ChildCount() > 0) return;
    rows->DetachAllChildren(); last_count = total;
    for (size_t i = 0; i < total; ++i) {
        int idx = (int)i;
        auto btn = Button("[Remove]", [=, &service] { std::string id = service.GetId(idx); if(!id.empty()) service.RemoveContact(id); }, ButtonOption::Ascii());
        rows->Add(Renderer(btn, [=, &service, btn] {
            auto el = MakeTableRow(text(service.GetData(idx, "name")), text(service.GetData(idx, "email")), text(service.GetData(idx, "timestamp")), btn->Render());
            if (idx % 2 != 0) el = el | bgcolor(Color::RGB(60, 60, 60));
            if (btn->Focused() && !service.IsLoading() && idx >= (int)service.GetLoadedCount() - 2 && service.HasMore()) service.LoadMore(10);
            return btn->Focused() ? el | inverted : el;
        }));
    }
    if (service.HasMore() || service.IsLoading()) {
        rows->Add(Renderer([&service, nAddr] { 
            if (!service.IsLoading() && (int)service.GetLoadedCount() < nAddr && service.HasMore()) service.LoadMore(10);
            return hbox({ filler(), text("Loading...") | dim, filler() });
        }));
    }
}

int main(int argc, char** argv) {
  try {
      std::ofstream(GetLogFilename(), std::ios::trunc);
      firebase::SetLogLevel(firebase::kLogLevelError);
      auto screen = ScreenInteractive::Fullscreen();
      AppState state;
      FirestoreService service([&screen, &state]() mutable { if (state.started) screen.Post(Event::Custom); });
      state.nAddr = std::max(10, Terminal::Size().dimy + 5);
      state.api_key = LoadApiKey();
      if (!state.api_key.empty()) service.Initialize(state.api_key, "addressbook", state.nAddr);

      static std::string f_name = "", f_email = "";
      static int sort_col = 2; static bool sort_desc = true;
      auto addr_in_name = Input(&f_name, "Filter Name"), addr_in_email = Input(&f_email, "Filter Email");
      
      auto rows = Container::Vertical({});
      auto rows_c = CatchEvent(rows, [&, rows](Event e) {
          if (e.is_mouse() && (e.mouse().button == Mouse::WheelUp || e.mouse().button == Mouse::WheelDown)) {
              return rows->OnEvent(e.mouse().button == Mouse::WheelUp ? Event::ArrowUp : Event::ArrowDown);
          }
          return false;
      });

      size_t last_count = 0;
      auto refresh = [&]() { RefreshAddressList(service, rows, sort_col, sort_desc, f_name, f_email, last_count, state.nAddr); };

      auto btn_n = Button("Name", [&]{ if(sort_col==0) sort_desc=!sort_desc; else {sort_col=0; sort_desc=false; f_email="";} screen.Post(Event::Custom); }, ButtonOption::Ascii());
      auto btn_m = Button("Mail", [&]{ if(sort_col==1) sort_desc=!sort_desc; else {sort_col=1; sort_desc=false; f_name="";} screen.Post(Event::Custom); }, ButtonOption::Ascii());
      auto btn_t = Button("Time", [&]{ if(sort_col==2) sort_desc=!sort_desc; else {sort_col=2; sort_desc=true; f_name=f_email="";} screen.Post(Event::Custom); }, ButtonOption::Ascii());

      static std::string n_name = GenerateRandomName(), n_email = GenerateRandomEmail(n_name);
      auto add_btn = Button("[Add]", [&service] { if (!n_name.empty()) { service.AddContact(n_name, n_email); n_name = GenerateRandomName(); n_email = GenerateRandomEmail(n_name); } }, ButtonOption::Ascii());
      auto add_row = Renderer(Container::Horizontal({Input(&n_name, "Name"), Input(&n_email, "Email"), add_btn}), [=] {
          return vbox({ separator(), MakeTableRow(text(n_name), text(n_email), text("(Now)"), add_btn->Render()), separator() });
      });

      auto close_btn = Button("[Close]", [&] { screen.Exit(); }, ButtonOption::Ascii());
      auto main_ui = Renderer(Container::Vertical({ Container::Horizontal({ btn_n, btn_m, btn_t, addr_in_name, addr_in_email }), rows_c, add_row, Container::Horizontal({ close_btn }) }), [&] {
          auto sort_ind = [&](int col) { return (sort_col != col) ? text("") : text(sort_desc ? " v" : " ^"); };
          auto render_in = [&](int col, Component in) { return hbox({ text(" ["), (sort_col == col ? in->Render() : text("          ") | dim), text("]") }); };
          return vbox({
              text("Address Book Setting") | bold | center, separator(),
              hbox({
                hbox({ btn_n->Render(), sort_ind(0), render_in(0, addr_in_name) }) | size(WIDTH, EQUAL, 28),
                separator(),
                hbox({ btn_m->Render(), sort_ind(1), render_in(1, addr_in_email) }) | flex,
                separator(),
                hbox({ btn_t->Render(), sort_ind(2) }) | size(WIDTH, EQUAL, 16),
                text("          ")
                }),
              separator(), rows_c->Render() | vscroll_indicator | frame | flex, add_row->Render(),
              hbox({ text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold, filler(), close_btn->Render() })
          }) | border;
      });

      auto final_component = CatchEvent(main_ui, [&](Event e) {
          if (e == Event::Custom) { refresh(); return true; }
          if (e == Event::Character("\x10")) {
              auto cap = Screen::Create(Terminal::Size()); Render(cap, main_ui->Render());
              SaveSnapshot("addrapp", cap.ToString()); return true;
          }
          if (e == Event::Character('q') || e == Event::Escape) { screen.Exit(); return true; }
          return main_ui->OnEvent(e);
      });

      state.started = true; refresh(); screen.Loop(final_component);
  } catch (const std::exception& e) { std::cerr << "EXCEPTION: " << e.what() << std::endl; return 1; }
  return 0;
}
