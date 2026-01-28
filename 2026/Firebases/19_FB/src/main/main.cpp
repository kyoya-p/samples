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

std::string GenerateRandomName() {
  static const std::vector<std::string> f = {"Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Heidi"};
  static const std::vector<std::string> l = {"Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller"};
  static std::random_device rd;
  static std::mt19937 gen(rd());
  std::uniform_int_distribution<> d1(0, 7);
  std::uniform_int_distribution<> d2(0, 6);
  std::uniform_int_distribution<> d3(100, 999);
  std::uniform_int_distribution<> d_type(0, 2); // 0: None, 1: Single, 2: Double
  std::uniform_int_distribution<> d_char(0, 51); // a-z, A-Z

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

int main(int argc, char** argv) {
  try {
      std::ofstream(GetLogFilename(), std::ios::trunc);
      firebase::SetLogLevel(firebase::kLogLevelError);

      auto screen = ScreenInteractive::Fullscreen();
      std::atomic<bool> started{false};
      FirestoreService service([&screen, &started]() mutable { if (started) screen.Post(Event::Custom); });
      
      // 画面の高さに合わせて初期取得件数を決定 (高さ + バッファ5行)
      int nAddr = Terminal::Size().dimy + 5;
      if (nAddr < 10) nAddr = 10; 

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
      if (!api_key_str.empty()) service.Initialize(api_key_str, nAddr);

      auto active_screen = AppAddressBook;
      auto show_picker = std::make_shared<bool>(false);
      auto show_api_dialog = std::make_shared<bool>(false);
      static std::string temp_api_key = api_key_str;

      static std::string scan_new_email = "";
      static std::vector<std::string> scan_confirmed_emails;

            // --- Address Book Screen Components ---
            static std::string addr_filter_name = "";
            static std::string addr_filter_email = "";
            static int addr_sort_col = 2;
            static bool addr_sort_desc = true;

            auto addr_input_name = Input(&addr_filter_name, "Filter Name");
            auto addr_input_email = Input(&addr_filter_email, "Filter Email");

            auto rows_container = Container::Vertical({});
            auto rows_container_c = CatchEvent(rows_container, [&](Event e) {
                if (e.is_mouse() && (e.mouse().button == Mouse::WheelUp || e.mouse().button == Mouse::WheelDown)) {
                    if (e.mouse().button == Mouse::WheelUp) return rows_container->OnEvent(Event::ArrowUp);
                    if (e.mouse().button == Mouse::WheelDown) return rows_container->OnEvent(Event::ArrowDown);
                }
                return false;
            });
            size_t last_addr_rendered_count = 0;
            auto refresh_address_list = [&]() {
              // フィルタ/ソート条件が変更された可能性があるため、最新の状態をサービスに同期
              // 注意: サービス内部で条件が変わっていれば自動的に再取得(Reset)が走る
              std::string sort_key = (addr_sort_col == 0) ? "name" : (addr_sort_col == 1) ? "email" : "timestamp";
              service.SetSortOrder(sort_key, addr_sort_desc);
              service.SetFilter(addr_filter_name, addr_filter_email);

              size_t total = service.GetLoadedCount();
              // データ件数に変更がない場合は、コンポーネントツリーを再構築しない（クエリ爆発防止）
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
                  // 完全バッファレス・レンダリング: 描画の瞬間にスナップショットからデータを抽出
                  auto el = MakeTableRow(
                      text(service.GetData(idx, "name")), 
                      text(service.GetData(idx, "email")), 
                      text(service.GetData(idx, "timestamp")), 
                      remove_btn->Render()
                  );
                  if (idx % 2 != 0) el = el | bgcolor(Color::RGB(60, 60, 60));
                  if (remove_btn->Focused()) {
                      // スクロールによる追加読み込み
                      if (!service.IsLoading() && idx >= (int)service.GetLoadedCount() - 2 && service.HasMore()) {
                          service.LoadMore(10);
                      }
                      return el | inverted;
                  }
                  return el;
                });
                rows_container->Add(row);
              }
      
              // 次のページがある場合、または初回読み込み中
              if (service.HasMore() || service.IsLoading()) {
                  rows_container->Add(Renderer([&service, nAddr] { 
                      // 起動直後またはフィルタ変更直後は、画面を埋めるのに必要な件数(nAddr)までは自動取得を許可
                      if (!service.IsLoading() && (int)service.GetLoadedCount() < nAddr && service.HasMore()) {
                          service.LoadMore(10);
                      }
                      return hbox({ filler(), text(service.IsLoading() ? "Loading..." : "") | dim, filler() }); 
                  }));
              }
            };      static std::string n_name = GenerateRandomName();
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

      // --- API Key Dialog ---
      auto api_input = Input(&temp_api_key, "Enter Firebase API Key");
      auto api_ok_btn = Button("[OK]", [&] {
          if (!temp_api_key.empty()) {
              service.Initialize(temp_api_key, nAddr);
              api_key_str = temp_api_key;
          }
          *show_api_dialog = false;
      }, ButtonOption::Ascii());
      auto api_cancel_btn = Button("[Cancel]", [&] { *show_api_dialog = false; }, ButtonOption::Ascii());
      auto api_dialog_container = Container::Vertical({ api_input, Container::Horizontal({ api_ok_btn, api_cancel_btn }) });
      auto api_dialog_renderer = Renderer(api_dialog_container, [&] {
          return vbox({
              text("Firebase API Key Setting") | bold | center,
              separator(),
              text("API Key:"),
              api_input->Render() | border,
              separator(),
              hbox({ filler(), api_ok_btn->Render(), text(" "), api_cancel_btn->Render() })
          }) | border | bgcolor(Color::Blue) | size(WIDTH, GREATER_THAN, 60) | clear_under;
      });

      auto addr_btn_name = Button("Name", [&]{ if(addr_sort_col==0) addr_sort_desc=!addr_sort_desc; else {addr_sort_col=0; addr_sort_desc=false; addr_filter_email="";} screen.Post(Event::Custom); }, ButtonOption::Ascii());
      auto addr_btn_mail = Button("Mail", [&]{ if(addr_sort_col==1) addr_sort_desc=!addr_sort_desc; else {addr_sort_col=1; addr_sort_desc=false; addr_filter_name="";} screen.Post(Event::Custom); }, ButtonOption::Ascii());
      auto addr_btn_time = Button("Time", [&]{ if(addr_sort_col==2) addr_sort_desc=!addr_sort_desc; else {addr_sort_col=2; addr_sort_desc=true; addr_filter_name=""; addr_filter_email="";} screen.Post(Event::Custom); }, ButtonOption::Ascii());

      auto addr_input_name_c = CatchEvent(addr_input_name, [&](Event e){ if(addr_sort_col != 0) return false; bool ret = addr_input_name->OnEvent(e); if(ret) screen.Post(Event::Custom); return ret; });
      auto addr_input_email_c = CatchEvent(addr_input_email, [&](Event e){ if(addr_sort_col != 1) return false; bool ret = addr_input_email->OnEvent(e); if(ret) screen.Post(Event::Custom); return ret; });

      auto close_addr_btn = Button("[Close]", screen.ExitLoopClosure(), ButtonOption::Ascii());
      auto activate_btn = Button("[Activate]", [&] { temp_api_key = api_key_str; *show_api_dialog = true; }, ButtonOption::Ascii());
      auto addr_header_container = Container::Horizontal({ addr_btn_name, addr_btn_mail, addr_btn_time, addr_input_name_c, addr_input_email_c });
      auto addr_nav_container = Container::Horizontal({ activate_btn, close_addr_btn });
      auto addr_main_container = Container::Vertical({ addr_header_container, rows_container_c, add_row, addr_nav_container });

      auto addr_renderer = Renderer(addr_main_container, [=, &service] {
          auto sort_ind = [&](int col) { return (addr_sort_col != col) ? text("") : text(addr_sort_desc ? " v" : " ^"); };
          auto render_input = [&](int col, Component input) {
              if (addr_sort_col == col) return hbox({ text(" ["), input->Render(), text("]") });
              return hbox({ text(" ["), text("          ") | dim, text("]") });
          };
          return vbox({
              text("Address Book Setting") | bold | center,
              separator(),
              hbox({ 
                  hbox({ addr_btn_name->Render(), sort_ind(0), render_input(0, addr_input_name) }) | size(WIDTH, EQUAL, 28),
                  separator(),
                  hbox({ addr_btn_mail->Render(), sort_ind(1), render_input(1, addr_input_email) }) | flex,
                  separator(),
                  hbox({ addr_btn_time->Render(), sort_ind(2) }) | size(WIDTH, EQUAL, 16),
                  text("          ")
              }),
              separator(),
              rows_container_c->Render() | vscroll_indicator | frame | flex,
              add_row->Render(),
              hbox({ text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold, filler(), activate_btn->Render(), text(" "), close_addr_btn->Render() })
          }) | border;
      });

      // --- Picker Dialog ---
      static std::string p_filter_name = "";
      static std::string p_filter_email = "";
      static int p_sort_col = 2;
      static bool p_sort_desc = true;
      
      auto p_input_name = Input(&p_filter_name, "Filter Name");
      auto p_input_email = Input(&p_filter_email, "Filter Email");
      auto p_list_container = Container::Vertical({});
      auto p_list_container_c = CatchEvent(p_list_container, [&](Event e) {
          if (e.is_mouse() && (e.mouse().button == Mouse::WheelUp || e.mouse().button == Mouse::WheelDown)) {
              if (e.mouse().button == Mouse::WheelUp) return p_list_container->OnEvent(Event::ArrowUp);
              if (e.mouse().button == Mouse::WheelDown) return p_list_container->OnEvent(Event::ArrowDown);
          }
          return false;
      });
      
      size_t last_picker_rendered_count = 0;
      auto update_picker_list = [&, service_ptr=&service]() mutable {
          std::string sort_key = (p_sort_col == 0) ? "name" : (p_sort_col == 1) ? "email" : "timestamp";
          service_ptr->SetSortOrder(sort_key, p_sort_desc);
          service_ptr->SetFilter(p_filter_name, p_filter_email);

          size_t total = service_ptr->GetLoadedCount();
          if (total == last_picker_rendered_count && p_list_container->ChildCount() > 0) return;

          p_list_container->DetachAllChildren();
          last_picker_rendered_count = total;
          
          for (size_t i = 0; i < total; ++i) {
              int idx = (int)i;
              auto label = std::make_shared<std::string>();
              auto btn = Button(label.get(), [&, idx, service_ptr] { 
                  scan_new_email = service_ptr->GetData(idx, "email");
                  *show_picker = false;
                  p_filter_name = ""; p_filter_email = "";
                  service_ptr->SetFilter("", "");
              }, ButtonOption::Ascii());

              auto item = Renderer(btn, [=, service_ptr] {
                  std::string name = service_ptr->GetData(idx, "name");
                  std::string email = service_ptr->GetData(idx, "email");
                  std::string time = service_ptr->GetData(idx, "timestamp");
                  if (name.length() > 20) name = name.substr(0, 20); else name.resize(20, ' ');
                  if (email.length() > 30) email = email.substr(0, 30); else email.resize(30, ' ');
                  *label = name + " " + email + " " + time;

                  if (btn->Focused() && !service_ptr->IsLoading() && idx >= (int)service_ptr->GetLoadedCount() - 2) {
                      service_ptr->LoadMore(10);
                  }
                  return btn->Render();
              });
              p_list_container->Add(item);
          }

          if (service_ptr->HasMore()) {
              p_list_container->Add(Renderer([service_ptr] {
                  if (!service_ptr->IsLoading() && service_ptr->GetLoadedCount() < 20) {
                      service_ptr->LoadMore(10);
                  }
                  return hbox({ filler(), text("Loading...") | dim, filler() });
              }));
          }
      };

      auto p_btn_name = Button("Name", [&]{ if(p_sort_col==0) p_sort_desc=!p_sort_desc; else {p_sort_col=0; p_sort_desc=false; p_filter_email="";} update_picker_list(); }, ButtonOption::Ascii());
      auto p_btn_mail = Button("Mail", [&]{ if(p_sort_col==1) p_sort_desc=!p_sort_desc; else {p_sort_col=1; p_sort_desc=false; p_filter_name="";} update_picker_list(); }, ButtonOption::Ascii());
      auto p_btn_time = Button("Time", [&]{ if(p_sort_col==2) p_sort_desc=!p_sort_desc; else {p_sort_col=2; p_sort_desc=true; p_filter_name=""; p_filter_email="";} update_picker_list(); }, ButtonOption::Ascii());
      auto p_cancel_btn = Button("[Cancel]", [=] { *show_picker = false; }, ButtonOption::Ascii());
      
      auto p_input_name_c = CatchEvent(p_input_name, [&](Event e){ if(p_sort_col != 0) return false; bool ret = p_input_name->OnEvent(e); if(ret) update_picker_list(); return ret; });
      auto p_input_email_c = CatchEvent(p_input_email, [&](Event e){ if(p_sort_col != 1) return false; bool ret = p_input_email->OnEvent(e); if(ret) update_picker_list(); return ret; });

      auto p_main_container_gen = Container::Vertical({
          Container::Horizontal({ p_btn_name, p_btn_mail, p_btn_time, p_input_name_c, p_input_email_c }),
          p_list_container_c,
          p_cancel_btn
      });

      auto picker_renderer = Renderer(p_main_container_gen, [=, &service] {
          auto sort_indicator = [&](int col) { return (p_sort_col != col) ? text("") : text(p_sort_desc ? " v" : " ^"); };
          auto render_input = [&](int col, Component input) {
              if (p_sort_col == col) return hbox({ text(" ["), input->Render(), text("]") });
              return hbox({ text(" ["), text("          ") | dim, text("]") });
          };
          return vbox({
              text("Select Address") | bold | center, separator(),
              hbox({ p_btn_name->Render(), sort_indicator(0), render_input(0, p_input_name), text("  "), p_btn_mail->Render(), sort_indicator(1), render_input(1, p_input_email), text("  "), filler(), p_btn_time->Render(), sort_indicator(2) }),
              separator(),
              p_list_container_c->Render() | vscroll_indicator | frame | size(HEIGHT, EQUAL, 10),
              separator(),
              hbox({ text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold, filler(), p_cancel_btn->Render() })
          }) | border | bgcolor(Color::Blue) | size(WIDTH, GREATER_THAN, 80) | clear_under;
      });

      // --- Main Renderer ---
      auto root_renderer = Renderer(addr_renderer, [&] {
          Element content = addr_renderer->Render();
          if (*show_picker) return dbox({ content | color(Color::GrayDark), picker_renderer->Render() | center });
          if (*show_api_dialog) return dbox({ content | color(Color::GrayDark), api_dialog_renderer->Render() | center });
          return content;
      });

      auto final_component = CatchEvent(root_renderer, [&](Event event) {
          if (event == Event::Custom) { refresh_address_list(); return true; }
          if (event == Event::Character('S')) {
              auto screen_capture = Screen::Create(Terminal::Size());
              Render(screen_capture, root_renderer->Render());
              SaveSnapshot("addrapp", screen_capture.ToString());
              return true;
          }
          if ((event == Event::Character('q') || event == Event::Escape) && !*show_picker && !*show_api_dialog) { screen.Exit(); return true; }
          if (*show_picker) return p_main_container_gen->OnEvent(event);
          if (*show_api_dialog) return api_dialog_container->OnEvent(event);
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
