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
      
      int nAddr = 10;
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

      auto active_screen = AppMain;
      auto show_picker = std::make_shared<bool>(false);

      static std::string scan_new_email = "";
      static std::vector<std::string> scan_confirmed_emails;

            // --- Address Book Screen Components ---
            auto rows_container = Container::Vertical({});
            size_t last_addr_rendered_count = 0;
            auto refresh_address_list = [&]() {
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
                  // "表示するときにクエリ" - SDKのスナップショットから直接取得
                  auto el = MakeTableRow(
                      text(service.GetData(idx, "name")), 
                      text(service.GetData(idx, "email")), 
                      text(service.GetData(idx, "timestamp")), 
                      remove_btn->Render()
                  );
                  if (idx % 2 != 0) el = el | bgcolor(Color::RGB(60, 60, 60));
                  if (remove_btn->Focused()) {
                      if (!service.IsLoading() && idx >= (int)service.GetLoadedCount() - 2) service.LoadMore(10);
                      return el | inverted;
                  }
                  return el;
                });
                rows_container->Add(row);
              }
      
                              if (service.HasMore()) {
      
                                  rows_container->Add(Renderer([&service] { 
      
                                      // 画面表示が10数件なので、20件（2ページ分）までは自動取得を許可
      
                                      if (!service.IsLoading() && service.GetLoadedCount() < 20) {
      
                                          service.LoadMore(10);
      
                                      }
      
                                      return hbox({ filler(), text("Loading...") | dim, filler() }); 
      
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

      // --- Picker Dialog ---
      static std::string p_filter_name = "";
      static std::string p_filter_email = "";
      static int p_sort_col = 2;
      static bool p_sort_desc = true;
      
      auto p_input_name = Input(&p_filter_name, "Filter Name");
      auto p_input_email = Input(&p_filter_email, "Filter Email");
      auto p_list_container = Container::Vertical({});
      
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
          p_list_container,
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
              p_list_container->Render() | vscroll_indicator | frame | size(HEIGHT, EQUAL, 10),
              separator(),
              hbox({ text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold, filler(), p_cancel_btn->Render() })
          }) | border | bgcolor(Color::Blue) | size(WIDTH, GREATER_THAN, 80) | clear_under;
      });

      // --- Scan & Send Screen ---
      auto scan_list_container = Container::Vertical({});
      auto new_email_input = Input(&scan_new_email, "Address");
      auto new_enter_btn = Button("[Enter]", [&] { if (!scan_new_email.empty()) { scan_confirmed_emails.push_back(scan_new_email); scan_new_email = ""; screen.Post(Event::Custom); } }, ButtonOption::Ascii());
      auto new_addr_btn = Button("[Address Book]", [&] { p_filter_name = ""; p_filter_email = ""; update_picker_list(); *show_picker = true; }, ButtonOption::Ascii());
      auto new_email_input_c = CatchEvent(new_email_input, [&](Event e) { if (e == Event::Return) { if (!scan_new_email.empty()) { scan_confirmed_emails.push_back(scan_new_email); scan_new_email = ""; screen.Post(Event::Custom); } return true; } return new_email_input->OnEvent(e); });
      auto new_row_container = Container::Horizontal({ new_email_input_c, new_enter_btn, new_addr_btn });
      auto new_row_renderer = Renderer(new_row_container, [&] { return hbox({ text("- "), new_email_input->Render() | border | flex, text("  "), new_enter_btn->Render(), text(" / "), new_addr_btn->Render() }); });
      std::function<void()> refresh_scan_ui = [&]() {
          scan_list_container->DetachAllChildren();
          for (const auto& email : scan_confirmed_emails) {
              auto remove_btn = Button("[Remove]", [=, &screen] { auto it = std::find(scan_confirmed_emails.begin(), scan_confirmed_emails.end(), email); if (it != scan_confirmed_emails.end()) { scan_confirmed_emails.erase(it); screen.Post(Event::Custom); } }, ButtonOption::Ascii());
              scan_list_container->Add(Renderer(remove_btn, [email, remove_btn] { return hbox({ text("- " + email) | flex, remove_btn->Render() }); }));
          }
      };

      auto send_btn = Button("[Send]", [&] { scan_confirmed_emails.clear(); screen.Post(Event::Custom); }, ButtonOption::Ascii());
      auto back_btn = Button("[Back]", [&] { active_screen = AppMain; }, ButtonOption::Ascii());
      auto scan_footer_container = Container::Horizontal({ send_btn, back_btn });
      auto scan_footer_renderer = Renderer(scan_footer_container, [&] { return hbox({ filler(), send_btn->Render(), text(" "), back_btn->Render() }); });
      auto scan_renderer = Renderer(Container::Vertical({ new_row_renderer, scan_list_container, scan_footer_renderer }), [&] {
          return vbox({
              text("Scan & Send") | bold | center,
              separator(),
              text("To:"),
              new_row_renderer->Render(),
              scan_list_container->Render() | vscroll_indicator | frame | flex,
              separator(),
              scan_footer_renderer->Render()
          }) | border;
      });

      // --- Main Menu ---
      auto btn_scan = Button("Scan & Send", [&] { active_screen = AppScanSend; }, ButtonOption::Ascii());
      auto btn_addr = Button("Address Book Edit", [&] { active_screen = AppAddressBook; }, ButtonOption::Ascii());
      auto btn_exit = Button("[Exit]", screen.ExitLoopClosure(), ButtonOption::Ascii());
      auto main_menu_renderer = Renderer(Container::Vertical({ btn_scan, btn_addr, btn_exit }), [&] {
          auto make_box = [&](Component c) { auto el = c->Render() | center | size(WIDTH, EQUAL, 31) | size(HEIGHT, EQUAL, 3); if (c->Focused()) el = el | inverted; return window(text(""), el); };
          return vbox({
              text("QuantumRoast Scanner") | bold | center,
              separator(),
              filler(),
              make_box(btn_scan) | center,
              text(""),
              make_box(btn_addr) | center,
              filler(),
              separator(),
              hbox({ text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold, filler(), btn_exit->Render() }),
          }) | border;
      });

      auto root_container = Container::Tab({ main_menu_renderer, addr_renderer, scan_renderer }, (int*)&active_screen);
      auto root_renderer = Renderer(root_container, [&] {
          Element content;
          if (active_screen == AppMain) content = main_menu_renderer->Render();
          else if (active_screen == AppAddressBook) content = addr_renderer->Render();
          else content = scan_renderer->Render();
          if (*show_picker) return dbox({ content | color(Color::GrayDark), picker_renderer->Render() | center });
          return content;
      });

      auto final_component = CatchEvent(root_renderer, [&](Event event) {
          if (event == Event::Custom) { refresh_address_list(); refresh_scan_ui(); return true; }
          if (event == Event::Character('S')) {
              auto screen_capture = Screen::Create(Terminal::Size());
              Render(screen_capture, root_renderer->Render());
              SaveSnapshot("addrapp", screen_capture.ToString());
              return true;
          }
          if (event == Event::Character('q') && !*show_picker) { if (active_screen == AppMain) screen.Exit(); else active_screen = AppMain; return true; }
          if (*show_picker) return p_main_container_gen->OnEvent(event);
          return root_container->OnEvent(event);
      });

      started = true;
      screen.Loop(final_component);
  } catch (const std::exception& e) {
      std::cerr << "EXCEPTION: " << e.what() << std::endl;
      return 1;
  }
  return 0;
}
