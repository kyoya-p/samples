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

// Helper function from main/main.cpp
Element MakeTableRow(Element name, Element email, Element time, Element op) {
    return hbox({
        std::move(name)  | size(WIDTH, EQUAL, 28),
        std::move(email) | flex,
        std::move(time)  | size(WIDTH, EQUAL, 16),
        std::move(op)    | size(WIDTH, EQUAL, 10) | center,
    });
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
          // Try reading from app.conf in CWD (project root usually)
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
            // No need to write back in this app, or maybe yes? 
            // The main app does it. Let's keep it read-only mostly or same behavior.
        }
      }

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
      
      auto p_input_name = Input(&p_filter_name, "Filter Name");
      auto p_input_email = Input(&p_filter_email, "Filter Email");
      
      auto p_list_container = Container::Vertical({});
      
      auto update_picker_list = [&, service_ptr=&service]() {
          p_list_container->DetachAllChildren();
          auto contacts = service_ptr->GetContacts();
          
          auto it = std::remove_if(contacts.begin(), contacts.end(), [&](const Contact& c){
              if (p_sort_col == 0 && !p_filter_name.empty() && c.name.find(p_filter_name) == std::string::npos) return true;
              if (p_sort_col == 1 && !p_filter_email.empty() && c.email.find(p_filter_email) == std::string::npos) return true;
              return false;
          });
          contacts.erase(it, contacts.end());

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
              if (label_name.length() > 20) label_name = label_name.substr(0, 20); else label_name.resize(20, ' ');
              std::string label_email = email;
              if (label_email.length() > 30) label_email = label_email.substr(0, 30); else label_email.resize(30, ' ');

              p_list_container->Add(Button(label_name + " " + label_email + " " + contact.timestamp, [&, email] { 
                  scan_confirmed_emails.push_back(email);
                  refresh_scan_ui(); // Need to refresh the main UI list
                  // *show_picker = false; // "ダイアログを閉じる" is implied? Specification says "ダイアログを閉じる" (close dialog).
                  // "アイテムをクリックしたらそのアイテムを選択し、送信画面状のリストに追加する。" -> Add to list.
                  // Does it close? "アイテムをクリックしたらそのアイテムを選択し、送信画面状のリストに追加する。" - Doesn't explicitly say close in spec provided in prompt, 
                  // BUT previous spec said "Close dialog". 
                  // Wait, "アイテムをクリックしたらそのアイテムを選択し、ダイアログを閉じる" is in the ASCII art description for Address Book!
                  // "アイテムをクリックしたらそのアイテムを選択し、送信画面状のリストに追加する。" is also there.
                  // Combining: Add to list AND close.
                  *show_picker = false;
                  p_filter_name = ""; p_filter_email = "";
              }, ButtonOption::Ascii()));
          }
      };

      auto p_btn_name = Button("Name", [&]{ if(p_sort_col==0) p_sort_desc=!p_sort_desc; else {p_sort_col=0; p_sort_desc=false; p_filter_email="";} update_picker_list(); }, ButtonOption::Ascii());
      auto p_btn_mail = Button("Mail", [&]{ if(p_sort_col==1) p_sort_desc=!p_sort_desc; else {p_sort_col=1; p_sort_desc=false; p_filter_name="";} update_picker_list(); }, ButtonOption::Ascii());
      auto p_btn_time = Button("Time", [&]{ if(p_sort_col==2) p_sort_desc=!p_sort_desc; else {p_sort_col=2; p_sort_desc=true; p_filter_name=""; p_filter_email="";} update_picker_list(); }, ButtonOption::Ascii());
      auto p_cancel_btn = Button("[Close]", [=] { *show_picker = false; }, ButtonOption::Ascii());
      
      auto p_input_name_c = CatchEvent(p_input_name, [&](Event e){ bool ret = p_input_name->OnEvent(e); if(ret) update_picker_list(); return ret; });
      auto p_input_email_c = CatchEvent(p_input_email, [&](Event e){ bool ret = p_input_email->OnEvent(e); if(ret) update_picker_list(); return ret; });

      auto p_main_container_gen = Container::Vertical({
          Container::Horizontal({ p_btn_name, p_btn_mail, p_btn_time, p_input_name_c, p_input_email_c }),
          p_list_container,
          p_cancel_btn
      });

      auto picker_renderer = Renderer(p_main_container_gen, [=, &service] {
          auto sort_indicator = [&](int col) { return (p_sort_col != col) ? text("  ") : text(p_sort_desc ? " v" : " ^"); };
          Element input_area = (p_sort_col == 0) ? p_input_name->Render() : (p_sort_col == 1) ? p_input_email->Render() : text("                ");

          return vbox({
              text("Address Book") | bold | center,
              separator(),
              hbox({ p_btn_name->Render(), sort_indicator(0), text(" | "), p_btn_mail->Render(), sort_indicator(1), text(" | "), p_btn_time->Render(), sort_indicator(2) }),
              separator(),
              hbox({ text("Search: "), input_area }) | size(HEIGHT, EQUAL, 1), 
              separator(),
              p_list_container->Render() | vscroll_indicator | frame | size(HEIGHT, EQUAL, 10),
              separator(),
              hbox({ text(service.IsConnected() ? "Status: Connected" : "Status: Disconnected") | bold, filler(), p_cancel_btn->Render() })
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
      auto new_addr_btn = Button("[Address Book]", [&] { 
          p_filter_name = ""; p_filter_email = ""; update_picker_list(); *show_picker = true; 
      }, ButtonOption::Ascii());
      
      // std::function<void()> refresh_scan_ui; // Removed local declaration

      auto new_enter_btn = Button("[Enter]", [&] {
          if (!scan_new_email.empty()) {
              scan_confirmed_emails.push_back(scan_new_email);
              scan_new_email = "";
              refresh_scan_ui();
          }
      }, ButtonOption::Ascii());

      // Trigger Enter logic on Return key
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
          Renderer([]{ return text("- "); }),
          Renderer(new_email_input_c, [&] { return new_email_input->Render() | border | flex; }) | flex,
          Renderer([]{ return text(" "); }),
          Container::Vertical({
              new_enter_btn,
              new_addr_btn
          })
      });

      refresh_scan_ui = [&]() {
          scan_list_container->DetachAllChildren();
          for (int i = 0; i < (int)scan_confirmed_emails.size(); ++i) {
              std::string email = scan_confirmed_emails[i];
              auto remove_btn = Button("[Remove]", [=, &refresh_scan_ui] {
                   auto it = std::find(scan_confirmed_emails.begin(), scan_confirmed_emails.end(), email);
                   if (it != scan_confirmed_emails.end()) {
                       scan_confirmed_emails.erase(it);
                       refresh_scan_ui();
                   }
              }, ButtonOption::Ascii());
              auto row = Container::Horizontal({ Renderer([email]{ return text("- " + email) | flex; }), remove_btn });
              scan_list_container->Add(row);
          }
      };
      refresh_scan_ui();

      auto send_btn = Button("[Send]", [&] {
          if (scan_confirmed_emails.empty()) return;
          std::string log_msg = "Sending to: ";
          for (const auto& email : scan_confirmed_emails) log_msg += email + ", ";
          Log(log_msg);
          
          sent_msg_content = "Sent to " + std::to_string(scan_confirmed_emails.size()) + " recipients.";
          *show_sent_dialog = true;

          scan_confirmed_emails.clear();
          refresh_scan_ui();
      }, ButtonOption::Ascii());

      // No [Back] button per ASCII art, or maybe Exit?
      // ASCII art shows only [Send]. But "SendApp.exe" might need a way to close.
      // Usually "q" or [Exit]. I'll stick to ASCII art: No Exit button on screen.
      // User can use 'q' (global handler) or window close.
      
      auto close_btn = Button("[Close]", screen.ExitLoopClosure(), ButtonOption::Ascii());

      auto scan_main_container = Container::Vertical({
          Renderer([]{ return text("To:"); }),
          new_row_container,
          scan_list_container | flex, 
          Container::Horizontal({ Renderer([]{ return filler(); }), send_btn, Renderer([]{ return text(" "); }), close_btn }) 
      });
      auto scan_renderer = Renderer(scan_main_container, [&] {
          return vbox({
              text("Send") | bold, // Title "Send"
              separator(),
              scan_main_container->Render() | flex
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
              update_picker_list();
              refresh_scan_ui();
              return true;
          }
          if (event == Event::Character('q') && !*show_picker && !*show_sent_dialog) {
              screen.Exit();
              return true;
          }
          if (*show_picker) return picker_renderer->OnEvent(event);
          if (*show_sent_dialog) return sent_container->OnEvent(event);
          return scan_main_container->OnEvent(event);
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
