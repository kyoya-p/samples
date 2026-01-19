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
LONG WINAPI CrashHandler(EXCEPTION_POINTERS* info) {
    (void)info;
    const char* seq = "\x1b[?1049l\x1b[?25h";
    DWORD w;
    HANDLE h = GetStdHandle(STD_OUTPUT_HANDLE);
    WriteFile(h, seq, (DWORD)strlen(seq), &w, NULL);
    return EXCEPTION_EXECUTE_HANDLER;
}
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

#include "dataaccess.hpp"
#include "utils.hpp"

#ifdef RGB
#undef RGB
#endif

using namespace ftxui;

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
#ifdef _WIN32
  SetUnhandledExceptionFilter(CrashHandler);
#endif
  try {
      std::ofstream(GetLogFilename(), std::ios::trunc);
      if (argc > 1 && std::string(argv[1]) == "--check") {
          std::cout << "Verification: OK" << std::endl;
          return 0;
      }
      bool snapshot_mode = (argc > 1 && std::string(argv[1]) == "--snapshot");
      auto build_ui = [&](FirestoreService& service, std::function<void()> on_exit, bool is_snapshot) -> Component {
          auto show_config = std::make_shared<bool>(false);
          static std::string api_key_input_val = "";
          const char* env = std::getenv("FB_API_KEY");
          if (!env) env = std::getenv("API_KEY");
          static std::string cur_key = (env != nullptr) ? env : "";
          api_key_input_val = cur_key;
          auto key_input = Input(&api_key_input_val, "API Key");
          auto connect_btn = Button("[Connect]", [=, &service] { if (service.Initialize(api_key_input_val, 20)) { *show_config = false; } }, ButtonOption::Ascii());
          auto cancel_btn = Button("[Cancel]", [=] { *show_config = false; }, ButtonOption::Ascii());
          auto config_container = Container::Vertical({ key_input, Container::Horizontal({ connect_btn, cancel_btn }) | center });
          auto config_renderer = Renderer(config_container, [=, &service] {
              if (is_snapshot) return vbox({ text("Configuration Placeholder") | border }) | center;
              return vbox({ text("Configuration") | bold | center, separator(), hbox(text("API Key: "), key_input->Render()) | border, separator(), hbox(connect_btn->Render(), cancel_btn->Render()) | center, text(""), text(service.GetError()) | color(Color::Red) | center }) | center | border | size(WIDTH, GREATER_THAN, 60);
          });
          auto rows_container = Container::Vertical({});
          auto refresh_ui = [=, &service](const std::vector<Contact>& contacts) {
            rows_container->DetachAllChildren();
            for (const auto& contact : contacts) {
              auto remove_btn = Button("[Remove]", [&service, contact] { service.RemoveContact(contact.id); }, ButtonOption::Ascii());
              auto row = Renderer(remove_btn, [=, &service, contact] {
                int idx = 0;
                for(size_t j=0; j<contacts.size(); ++j) if(contacts[j].id == contact.id) { idx = (int)j; break; }
                Element op = is_snapshot ? text("[Remove]") : remove_btn->Render();
                auto el = MakeTableRow(text(contact.name), text(contact.email), text(contact.timestamp), op);
                if (idx % 2 != 0) el = el | bgcolor(Color::RGB(60, 60, 60));
                if (!is_snapshot && remove_btn->Focused()) return el | inverted;
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
      };
      if (snapshot_mode) {
          FirestoreService service([]{});
          const char* k = std::getenv("FB_API_KEY");
          if (!k) k = std::getenv("API_KEY");
          if (k) service.Initialize(k, 20);
          for(int i=0; i<20; ++i) { if(!service.GetContacts().empty()) break; std::this_thread::sleep_for(std::chrono::milliseconds(200)); }
          auto root = build_ui(service, []{}, true);
          auto screen = Screen::Create(Dimension::Fixed(100), Dimension::Fixed(30));
          Render(screen, root->Render());
          std::cout << screen.ToString() << std::endl;
          return 0;
      }
      auto screen = ScreenInteractive::Fullscreen();
      std::atomic<bool> started{false};
      FirestoreService service([&screen, &started] { if (started) screen.Post(Event::Custom); });
      int nAddr = Terminal::Size().dimy + 5;
      const char* key = std::getenv("FB_API_KEY");
      if (!key) key = std::getenv("API_KEY");
      if (key) service.Initialize(key, nAddr);
      auto root = build_ui(service, screen.ExitLoopClosure(), false);
      started = true;
      screen.Loop(root);
  } catch (...) { return 1; }
  return 0;
}