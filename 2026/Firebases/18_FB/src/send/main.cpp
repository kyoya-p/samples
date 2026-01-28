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
      if (!api_key_str.empty()) service.Initialize(api_key_str, nAddr);

      auto show_picker = std::make_shared<bool>(false);
      auto show_sent_dialog = std::make_shared<bool>(false);
      static std::string sent_msg_content = "";

      static std::string scan_new_email = "";
      static std::vector<std::string> scan_confirmed_emails;
      
      // Forward declaration for refresh
      std::function<void()> refresh_scan_ui;

      // --- Picker Dialog ---
      static std::string p_filter_name = "";
      static std::string p_filter_email = "";
      static int p_sort_col = 2; // 0:Name, 1:Email, 2:Time
      static bool p_sort_desc = true;
      
      auto p_input_name = Input(&p_filter_name, "Name Search");
      auto p_input_email = Input(&p_filter_email, "Mail Search");
      
      auto p_list_container = Container::Vertical({});
      
      auto update_picker_list = [&, service_ptr=&service]() {
          p_list_container->DetachAllChildren();
          
          // Use DB Query for filtering and sorting
          std::string sort_key = (p_sort_col == 0) ? "name" : (p_sort_col == 1) ? "email" : "timestamp";
          service_ptr->SetSortOrder(sort_key, p_sort_desc);
          service_ptr->SetFilter(p_filter_name, p_filter_email);

          auto contacts = service_ptr->GetContacts();
          for (const auto& contact : contacts) {
              auto email = contact.email;
              auto name = contact.name;
              std::string label_name = name;
              if (label_name.length() > 20) label_name = label_name.substr(0, 20); else label_name.resize(20, ' ');
              std::string label_email = email;
              if (label_email.length() > 30) label_email = label_email.substr(0, 30); else label_email.resize(30, ' ');

              p_list_container->Add(Button(label_name + " " + label_email + " " + contact.timestamp, [&, email] { 
                  scan_confirmed_emails.push_back(email);
                  refresh_scan_ui(); 
                  *show_picker = false;
                  p_filter_name = ""; p_filter_email = "";
                  // Also reset DB query to default? Not strictly necessary but clean.
                  service_ptr->SetFilter("", "");
              }, ButtonOption::Ascii()));
          }
      };

      auto p_btn_name = Button("Name", [&]{ 
          if(p_sort_col!=0) { p_sort_col=0; p_sort_desc=false; p_filter_email=""; } // Switch to Name, clear Email filter
          else { p_sort_desc=!p_sort_desc; }
          update_picker_list(); 
      }, ButtonOption::Ascii());
      
      auto p_btn_mail = Button("Mail", [&]{ 
          if(p_sort_col!=1) { p_sort_col=1; p_sort_desc=false; p_filter_name=""; } // Switch to Mail, clear Name filter
          else { p_sort_desc=!p_sort_desc; }
          update_picker_list(); 
      }, ButtonOption::Ascii());
      
      auto p_btn_time = Button("Time", [&]{ 
          if(p_sort_col!=2) { p_sort_col=2; p_sort_desc=true; p_filter_name=""; p_filter_email=""; } // Switch to Time, clear both
          else { p_sort_desc=!p_sort_desc; }
          update_picker_list(); 
      }, ButtonOption::Ascii());

      auto p_picker_close_btn = Button("[Close]", [=] { *show_picker = false; }, ButtonOption::Ascii());
      
      // Inputs catch events only if their column is active
      auto p_input_name_c = CatchEvent(p_input_name, [&](Event e){ 
          if(p_sort_col != 0) return false; // Disable if not active column
          bool ret = p_input_name->OnEvent(e); 
          if(ret) update_picker_list(); 
          return ret; 
      });
      auto p_input_email_c = CatchEvent(p_input_email, [&](Event e){ 
          if(p_sort_col != 1) return false; // Disable if not active column
          bool ret = p_input_email->OnEvent(e); 
          if(ret) update_picker_list(); 
          return ret; 
      });

      auto p_main_container_gen = Container::Vertical({
          Container::Horizontal({ p_btn_name, p_btn_mail, p_btn_time, p_input_name_c, p_input_email_c }),
          p_list_container,
          p_picker_close_btn
      });

      auto picker_renderer = Renderer(p_main_container_gen, [=, &service] {
          auto sort_indicator = [&](int col) { return (p_sort_col != col) ? text("") : text(p_sort_desc ? " v" : " ^"); };

          // Helper to render input with visual disabled state
          auto render_input = [&](int col, Component input) {
              if (p_sort_col == col) return hbox({ text(" ["), input->Render(), text("]") });
              return hbox({ text(" ["), text("          ") | dim, text("]") }); // Disabled placeholder
          };

          auto name_header = hbox({
              p_btn_name->Render(), sort_indicator(0),
              render_input(0, p_input_name)
          });
          auto mail_header = hbox({
              p_btn_mail->Render(), sort_indicator(1),
              render_input(1, p_input_email)
          });
          auto time_header = hbox({
              p_btn_time->Render(), sort_indicator(2)
          });

          return vbox({
              text("Select Address") | bold | center,
              separator(),
              hbox({ 
                  name_header, text("  "),
                  mail_header, text("  "),
                  filler(),
                  time_header
              }),
              separator(),
              p_list_container->Render() | vscroll_indicator | frame | size(HEIGHT, EQUAL, 10),
              separator(),
              hbox({ text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold, filler(), p_picker_close_btn->Render() })
          }) | border | bgcolor(Color::Blue) | size(WIDTH, GREATER_THAN, 80) | clear_under;
      });

      // --- Sent Dialog ---
      auto sent_ok_btn = Button("[OK]", [=] { *show_sent_dialog = false; }, ButtonOption::Ascii());
      auto sent_container = Container::Vertical({ sent_ok_btn });
      auto sent_renderer = Renderer(sent_container, [=] {
          return vbox({
              text("Notification") | bold | center,
              separator(),
              text(sent_msg_content) | center,
              separator(),
              sent_ok_btn->Render() | center
          }) | border | bgcolor(Color::Blue) | size(WIDTH, GREATER_THAN, 40) | clear_under;
      });

      // --- Send Screen ---
      auto scan_list_container = Container::Vertical({});
      auto new_email_input = Input(&scan_new_email, "Address");
      
      auto new_enter_btn = Button("[Enter]", [&] {
          if (!scan_new_email.empty()) {
              scan_confirmed_emails.push_back(scan_new_email);
              scan_new_email = "";
              refresh_scan_ui();
          }
      }, ButtonOption::Ascii());

      auto new_addr_btn = Button("[Address Book]", [&] { 
          p_filter_name = ""; p_filter_email = ""; update_picker_list(); *show_picker = true; 
      }, ButtonOption::Ascii());

      auto new_email_input_c = CatchEvent(new_email_input, [&](Event e) {
          if (e == Event::Return) {
              if (!scan_new_email.empty()) {
                  scan_confirmed_emails.push_back(scan_new_email);
                  scan_new_email = "";
                  refresh_scan_ui();
              }
              return true; 
          }
          return new_email_input->OnEvent(e);
      });

      auto new_row_container = Container::Horizontal({
          new_email_input_c,
          new_enter_btn,
          new_addr_btn
      });

      auto new_row_renderer = Renderer(new_row_container, [&] {
          return hbox({
              text("- "),
              new_email_input->Render() | border | flex,
              text("  "),
              new_enter_btn->Render(),
              text(" / "),
              new_addr_btn->Render()
          });
      });

      refresh_scan_ui = [&]() {
          scan_list_container->DetachAllChildren();
          for (const auto& email : scan_confirmed_emails) {
              auto remove_btn = Button("[Remove]", [=, &refresh_scan_ui] {
                   auto it = std::find(scan_confirmed_emails.begin(), scan_confirmed_emails.end(), email);
                   if (it != scan_confirmed_emails.end()) {
                       scan_confirmed_emails.erase(it);
                       refresh_scan_ui();
                   }
              }, ButtonOption::Ascii());
              
              auto row_container = Container::Horizontal({ remove_btn });
              auto row_renderer = Renderer(row_container, [email, remove_btn] {
                  return hbox({
                      text("- " + email) | flex,
                      remove_btn->Render()
                  });
              });
              scan_list_container->Add(row_renderer);
          }
      };
      refresh_scan_ui();

            auto send_btn = Button("[Send]", [&] {

                if (scan_confirmed_emails.empty()) return;

                

                sent_msg_content = "Sent to " + std::to_string(scan_confirmed_emails.size()) + " recipients.";

                *show_sent_dialog = true;

      

                scan_confirmed_emails.clear();

                refresh_scan_ui();

            }, ButtonOption::Ascii());

      auto close_app_btn = Button("[Close]", screen.ExitLoopClosure(), ButtonOption::Ascii());

      auto scan_footer_container = Container::Horizontal({ send_btn, close_app_btn });
      auto scan_footer_renderer = Renderer(scan_footer_container, [&] {
          return hbox({
              filler(),
              send_btn->Render(),
              text(" "),
              close_app_btn->Render()
          });
      });

      auto scan_main_container = Container::Vertical({
          new_row_renderer,
          scan_list_container,
          scan_footer_renderer
      });

      auto scan_renderer = Renderer(scan_main_container, [&] {
          return vbox({
              text("Send") | bold,
              separator(),
              text("To:"),
              new_row_renderer->Render(),
              scan_list_container->Render() | vscroll_indicator | frame | flex, 
              separator(),
              scan_footer_renderer->Render()
          }) | border;
      });

      auto root_renderer = Renderer(scan_renderer, [&] {
          Element content = scan_renderer->Render();
          if (*show_picker) return dbox({ content | color(Color::GrayDark), picker_renderer->Render() | center });
          if (*show_sent_dialog) return dbox({ content | color(Color::GrayDark), sent_renderer->Render() | center });
          return content;
      });

      auto final_component = CatchEvent(root_renderer, [&](Event event) {
          if (event == Event::Custom) {
              if (*show_picker) update_picker_list();
              refresh_scan_ui();
              return true;
          }
          if ((event == Event::Character('q') || event == Event::Escape) && !*show_picker && !*show_sent_dialog) {
              screen.Exit();
              return true;
          }
          if (*show_picker) {
              if (event == Event::Escape) { *show_picker = false; return true; }
              return p_main_container_gen->OnEvent(event);
          }
          if (*show_sent_dialog) return sent_container->OnEvent(event);
          return scan_main_container->OnEvent(event);
      });

      started = true;
      screen.Loop(final_component);

  } catch (const std::exception& e) {
      std::cerr << "EXCEPTION: " << e.what() << std::endl;
      return 1;
  }
  return 0;
}