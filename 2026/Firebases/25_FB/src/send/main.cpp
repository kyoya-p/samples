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

int main(int argc, char** argv) {
  try {
      GetLogFilename() = "sendapp.log";
      std::ofstream(GetLogFilename(), std::ios::trunc);
      firebase::SetLogLevel(firebase::kLogLevelError);

      auto screen = ScreenInteractive::Fullscreen();
      std::atomic<bool> started{false};
      
      // Separate services for Address Book (Picker) and Job Log (Main Screen History)
      FirestoreService addr_service([&screen, &started]() mutable { if (started) screen.Post(Event::Custom); });
      FirestoreService log_service([&screen, &started]() mutable { if (started) screen.Post(Event::Custom); });
      
      std::string api_key_str;
      const char* key = std::getenv("FB_API_KEY");
      if (!key) key = std::getenv("API_KEY");
      if (key) {
          api_key_str = key;
      } else {
          std::string conf_path = GetExecutableDir() + "app.conf";
          std::ifstream conf_file(conf_path);
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
          addr_service.Initialize(api_key_str, "addressbook", 10);
          log_service.Initialize(api_key_str, "joblog", 10);
      }

      auto show_picker = std::make_shared<bool>(false);
      auto show_sent_dialog = std::make_shared<bool>(false);
      static std::string sent_msg_content = "";

      static std::string scan_new_email = "";
      static std::vector<std::pair<std::string, std::string>> scan_confirmed_emails;

      // --- Picker Dialog Components ---
      static std::string p_filter_name = "";
      static std::string p_filter_email = "";
      static int p_sort_col = 2; // Default to Time (timestamp)
      static bool p_sort_desc = true;
      
      auto p_input_name = Input(&p_filter_name, "Alice");
      auto p_input_email = Input(&p_filter_email, "Mail");
      auto p_list_container = Container::Vertical({});
      auto p_list_container_c = CatchEvent(p_list_container, [&](Event e) {
          if (e.is_mouse() && (e.mouse().button == Mouse::WheelUp || e.mouse().button == Mouse::WheelDown)) {
              if (e.mouse().button == Mouse::WheelUp) return p_list_container->OnEvent(Event::ArrowUp);
              if (e.mouse().button == Mouse::WheelDown) return p_list_container->OnEvent(Event::ArrowDown);
          }
          return false;
      });
      
      size_t last_addr_count = 0;
      auto update_picker_list = [&]() {
          std::string sort_key = (p_sort_col == 0) ? "name" : (p_sort_col == 1) ? "email" : "timestamp";
          addr_service.SetSortOrder(sort_key, p_sort_desc);
          addr_service.SetFilter(p_filter_name, p_filter_email);

          size_t total = addr_service.GetLoadedCount();
          if (total == last_addr_count && p_list_container->ChildCount() > 0) return;

          p_list_container->DetachAllChildren();
          last_addr_count = total;
          
          for (size_t i = 0; i < total; ++i) {
              int idx = (int)i;
              
              ButtonOption option = ButtonOption::Ascii();
              option.transform = [=, &addr_service](const EntryState& s) {
                  std::string name = addr_service.GetData(idx, "name");
                  std::string email = addr_service.GetData(idx, "email");
                  std::string time = addr_service.GetData(idx, "timestamp");
                  
                  auto el = hbox({
                      text(name) | size(WIDTH, EQUAL, 28),
                      text(email) | flex,
                      text(time) | size(WIDTH, EQUAL, 20)
                  });
                  if (s.focused) {
                      el = el | inverted;
                      if (!addr_service.IsLoading() && idx >= (int)addr_service.GetLoadedCount() - 2 && addr_service.HasMore()) {
                          addr_service.LoadMore(10);
                      }
                  }
                  return el;
              };

              auto btn = Button("", [&, idx] {
                  std::string name = addr_service.GetData(idx, "name");
                  std::string email = addr_service.GetData(idx, "email");
                  if(!email.empty()) {
                      auto it = std::find_if(scan_confirmed_emails.begin(), scan_confirmed_emails.end(), 
                          [&](const auto& p){ return p.second == email; });
                      if (it == scan_confirmed_emails.end()) {
                          scan_confirmed_emails.push_back({name, email});
                      }
                      *show_picker = false;
                      screen.Post(Event::Custom);
                  }
              }, option);

              p_list_container->Add(btn);
          }
          if (addr_service.IsLoading()) {
              p_list_container->Add(Renderer([&] { return text("Loading...") | center; }));
          }
      };

      auto p_btn_name = Button("Name", [&]{ if(p_sort_col==0) p_sort_desc=!p_sort_desc; else {p_sort_col=0; p_sort_desc=false; p_filter_email="";} update_picker_list(); }, ButtonOption::Ascii());
      auto p_btn_mail = Button("Mail", [&]{ if(p_sort_col==1) p_sort_desc=!p_sort_desc; else {p_sort_col=1; p_sort_desc=false; p_filter_name="";} update_picker_list(); }, ButtonOption::Ascii());
      auto p_btn_time = Button("Time", [&]{ if(p_sort_col==2) p_sort_desc=!p_sort_desc; else {p_sort_col=2; p_sort_desc=true; p_filter_name=""; p_filter_email="";} update_picker_list(); }, ButtonOption::Ascii());
      auto p_close_btn = Button("[Close]", [=] { *show_picker = false; }, ButtonOption::Ascii());
      
      auto p_input_name_c = CatchEvent(p_input_name, [&](Event e){ if(p_sort_col != 0) return false; bool ret = p_input_name->OnEvent(e); if(ret) update_picker_list(); return ret; });
      auto p_input_email_c = CatchEvent(p_input_email, [&](Event e){ if(p_sort_col != 1) return false; bool ret = p_input_email->OnEvent(e); if(ret) update_picker_list(); return ret; });

      // Name Column Renderer
      auto name_col_renderer = Renderer(Container::Horizontal({p_btn_name, p_input_name_c}), [&] {
          auto sort_ind = (p_sort_col == 0) ? text(p_sort_desc ? " v" : " ^") : text("");
          return hbox({
              p_btn_name->Render(),
              sort_ind,
              text(p_sort_col==0 ? " [" : "  "),
              p_input_name->Render(),
              text(p_sort_col==0 ? "]" : " ")
          }) | size(WIDTH, EQUAL, 28);
      });

      // Mail Column Renderer
      auto mail_col_renderer = Renderer(Container::Horizontal({p_btn_mail, p_input_email_c}), [&] {
          auto sort_ind = (p_sort_col == 1) ? text(p_sort_desc ? " v" : " ^") : text("");
           return hbox({
              p_btn_mail->Render(),
              sort_ind | flex, 
              text(p_sort_col==1 ? " [" : "  "),
              p_input_email->Render(),
              text(p_sort_col==1 ? "]" : " ")
          }) | flex;
      });

      // Time Column Renderer
      auto time_col_renderer = Renderer(p_btn_time, [&] {
          auto sort_ind = (p_sort_col == 2) ? text(p_sort_desc ? " v" : " ^") : text("");
          return hbox({
              p_btn_time->Render(),
              sort_ind
          }) | size(WIDTH, EQUAL, 20);
      });

      auto header_container = Container::Horizontal({
          name_col_renderer,
          mail_col_renderer,
          time_col_renderer
      });

      auto p_main_container = Container::Vertical({
          header_container,
          p_list_container_c,
          p_close_btn
      });

      auto picker_renderer = Renderer(p_main_container, [&] {
          return vbox({
              text("Select Address") | center,
              separator(),
              header_container->Render(),
              separator(),
              p_list_container_c->Render() | vscroll_indicator | frame | size(HEIGHT, EQUAL, 10),
              separator(),
              hbox({ filler(), p_close_btn->Render() })
          }) | border | size(WIDTH, GREATER_THAN, 90) | clear_under;
      });

      // --- Main Send Screen ---
      auto scan_list_container = Container::Vertical({});
      auto scan_list_container_c = CatchEvent(scan_list_container, [&](Event e) {
          if (e.is_mouse() && (e.mouse().button == Mouse::WheelUp || e.mouse().button == Mouse::WheelDown)) {
              if (e.mouse().button == Mouse::WheelUp) return scan_list_container->OnEvent(Event::ArrowUp);
              if (e.mouse().button == Mouse::WheelDown) return scan_list_container->OnEvent(Event::ArrowDown);
          }
          return false;
      });

      auto new_email_input = Input(&scan_new_email, "");
      auto add_manual_email = [&] {
          if (!scan_new_email.empty()) { 
              auto it = std::find_if(scan_confirmed_emails.begin(), scan_confirmed_emails.end(), 
                  [&](const auto& p){ return p.second == scan_new_email; });
              if (it == scan_confirmed_emails.end()) {
                  scan_confirmed_emails.push_back({"", scan_new_email}); 
              }
              scan_new_email = ""; 
              screen.Post(Event::Custom); 
          } 
      };
      auto new_enter_btn = Button("[Enter]", add_manual_email, ButtonOption::Ascii());
      auto new_addr_btn = Button("[Address Book]", [&] { p_filter_name = ""; p_filter_email = ""; update_picker_list(); *show_picker = true; }, ButtonOption::Ascii());
      
      auto new_email_input_c = CatchEvent(new_email_input, [&](Event e) { 
          if (e == Event::Return) { 
              add_manual_email();
              return true; 
          } 
          return new_email_input->OnEvent(e); 
      });

      auto log_list_container = Container::Vertical({});
      size_t last_log_count = 0;
      auto refresh_log_list = [&]() {
          size_t total = log_service.GetLoadedCount();
          if (total == last_log_count && log_list_container->ChildCount() > 0) return;
          log_list_container->DetachAllChildren();
          last_log_count = total;
          for (size_t i = 0; i < total; ++i) {
              int idx = (int)i;
              log_list_container->Add(Renderer([=, &log_service] {
                  return hbox({
                      text(log_service.GetData(idx, "timestamp")) | size(WIDTH, EQUAL, 20),
                      text(" ") | size(WIDTH, EQUAL, 10),
                      text(log_service.GetData(idx, "email")) | flex
                  });
              }));
          }
      };

      auto refresh_scan_ui = [&]() {
          scan_list_container->DetachAllChildren();
          for (const auto& pair : scan_confirmed_emails) {
              auto remove_btn = Button("[Remove]", [=, &screen] { 
                  auto it = std::find_if(scan_confirmed_emails.begin(), scan_confirmed_emails.end(), 
                      [&](const auto& p){ return p.second == pair.second; });
                  if (it != scan_confirmed_emails.end()) { 
                      scan_confirmed_emails.erase(it); 
                      screen.Post(Event::Custom); 
                  } 
              }, ButtonOption::Ascii());
              scan_list_container->Add(Renderer(remove_btn, [pair, remove_btn] { 
                  std::string display = pair.first.empty() ? pair.second : pair.first + " - " + pair.second;
                  return hbox({ text("- " + display) | flex, remove_btn->Render() }); 
              }));
          }
      };

      auto clean_log_btn = Button("[Clean Logs]", [&] { 
          log_service.ClearCollection("joblog");
      }, ButtonOption::Ascii());
      auto send_btn = Button("[<Send>]", [&] { 
          if (scan_confirmed_emails.empty()) return; 
          std::vector<std::string> emails;
          for(const auto& p : scan_confirmed_emails) emails.push_back(p.second);
          log_service.AddJobLog(emails);
          sent_msg_content = "Sent to " + std::to_string(scan_confirmed_emails.size()) + " recipients."; 
          *show_sent_dialog = true; 
          scan_confirmed_emails.clear(); 
          screen.Post(Event::Custom); 
      }, ButtonOption::Ascii());
      auto close_app_btn = Button("[Close]", screen.ExitLoopClosure(), ButtonOption::Ascii());

      auto scan_renderer = Renderer(Container::Vertical({ new_email_input_c, new_enter_btn, new_addr_btn, scan_list_container_c, clean_log_btn, send_btn, close_app_btn }), [&] {
          return vbox({
              text("Send Mail"),
              separator(),
              hbox({
                  text("    "),
                  text("To: ") | center,
                  vbox({
                      text("╭───────────────────────────────────────╮"),
                      hbox({ text("│ "), new_email_input->Render() | size(WIDTH, EQUAL, 37), text(" │") }),
                      text("╰───────────────────────────────────────╯"),
                  }),
                  text("              "),
                  new_enter_btn->Render(),
                  text("  /  "),
                  new_addr_btn->Render()
              }),
              scan_list_container_c->Render() | vscroll_indicator | frame | size(HEIGHT, LESS_THAN, 10),
              filler(),
              separator(),
              hbox({ text(" Send Log"), filler(), clean_log_btn->Render() }),
              log_list_container->Render() | frame | size(HEIGHT, EQUAL, 5),
              separator(),
              hbox({ 
                  text("  Status: " + (addr_service.IsConnected() ? std::string("Connected") : std::string("Disconnected"))),
                  filler(), 
                  send_btn->Render(), 
                  text("  "), 
                  close_app_btn->Render() 
              })
          }) | border;
      });

      // --- Sent Dialog ---
      auto sent_ok_btn = Button("[OK]", [=] { *show_sent_dialog = false; }, ButtonOption::Ascii());
      auto sent_renderer = Renderer(sent_ok_btn, [=] {
          return vbox({
              text("Notification") | center,
              separator(),
              text(sent_msg_content) | center,
              separator(),
              sent_ok_btn->Render() | center
          }) | border | size(WIDTH, GREATER_THAN, 40) | clear_under;
      });

      auto root_renderer = Renderer(scan_renderer, [&] {
          Element content = scan_renderer->Render();
          if (*show_picker) return dbox({ content, picker_renderer->Render() | center });
          if (*show_sent_dialog) return dbox({ content, sent_renderer->Render() | center });
          return content;
      });

      auto final_component = CatchEvent(root_renderer, [&](Event event) {
          if (event == Event::Custom) { 
              if (*show_picker) update_picker_list(); 
              refresh_scan_ui(); 
              refresh_log_list();
              return true; 
          }
          if (event == Event::Character("\x10")) {
              auto screen_capture = Screen::Create(Terminal::Size());
              Render(screen_capture, root_renderer->Render());
              SaveSnapshot("sendapp", screen_capture.ToString());
              return true;
          }
          if ((event == Event::Character('q') || event == Event::Escape) && !*show_picker && !*show_sent_dialog) { screen.Exit(); return true; }
          if (*show_picker) return picker_renderer->OnEvent(event);
          if (*show_sent_dialog) return sent_ok_btn->OnEvent(event);
          return scan_renderer->OnEvent(event);
      });

      started = true;
      refresh_log_list();
      screen.Loop(final_component);
  } catch (const std::exception& e) {
      std::cerr << "EXCEPTION: " << e.what() << std::endl;
      return 1;
  }
  return 0;
}