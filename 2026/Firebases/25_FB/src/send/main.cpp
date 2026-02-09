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

#include "firebase/app.h"
#include "firebase/firestore.h"
#include "utils.hpp"

#ifdef RGB
#undef RGB
#endif

using namespace ftxui;

struct FirebaseData {
    firebase::firestore::Firestore* db = nullptr;
    std::string collection;
    std::vector<firebase::firestore::QuerySnapshot> snapshots;
    std::vector<firebase::firestore::ListenerRegistration> listeners;
    std::string sort_field = "timestamp";
    bool sort_desc = true;
    std::string filter_name = "";
    std::string filter_email = "";
    bool is_loading = false;
    bool has_more = true;
    std::mutex mutex;
};

std::string GetData(FirebaseData& fb, size_t index, const std::string& field) {
    std::lock_guard<std::mutex> lock(fb.mutex);
    size_t base = 0;
    for (const auto& snap : fb.snapshots) {
        auto docs = snap.documents();
        if (index < base + docs.size()) {
            auto data = docs[index - base].GetData();
            if (field == "timestamp") {
                if (data.count("timestamp") && data["timestamp"].is_timestamp()) {
                    auto ts = data["timestamp"].timestamp_value();
                    std::time_t t = ts.seconds();
                    char buf[32];
                    std::tm tm_struct;
#ifdef _WIN32
                    localtime_s(&tm_struct, &t);
#else
                    localtime_r(&t, &tm_struct);
#endif
                    std::strftime(buf, sizeof(buf), "%m/%d %H:%M", &tm_struct);
                    return buf;
                }
                return "N/A";
            }
            if (data.count(field)) {
                if (data[field].is_string()) return data[field].string_value();
                if (data[field].is_array()) {
                    auto arr = data[field].array_value();
                    std::string res;
                    for (size_t i = 0; i < arr.size(); ++i) { if (i > 0) res += ", "; if (arr[i].is_string()) res += arr[i].string_value(); }
                    return res;
                }
            }
            return "";
        }
        base += docs.size();
    }
    return "";
}

void FetchNextPage(FirebaseData& fb, const std::function<void()>& on_update) {
    std::lock_guard<std::mutex> lock(fb.mutex);
    if (!fb.db || fb.is_loading || !fb.has_more) return;
    fb.is_loading = true;
    size_t page_index = fb.snapshots.size();
    firebase::firestore::Query q = fb.db->Collection(fb.collection.c_str());
    if (fb.sort_field == "name") {
        q = q.OrderBy("name", fb.sort_desc ? firebase::firestore::Query::Direction::kDescending : firebase::firestore::Query::Direction::kAscending);
        if (!fb.filter_name.empty()) q = q.WhereGreaterThanOrEqualTo("name", firebase::firestore::FieldValue::String(fb.filter_name)).WhereLessThanOrEqualTo("name", firebase::firestore::FieldValue::String(fb.filter_name + "\xEF\xA3\xBF"));
    } else if (fb.sort_field == "email") {
        q = q.OrderBy("email", fb.sort_desc ? firebase::firestore::Query::Direction::kDescending : firebase::firestore::Query::Direction::kAscending);
        if (!fb.filter_email.empty()) q = q.WhereGreaterThanOrEqualTo("email", firebase::firestore::FieldValue::String(fb.filter_email)).WhereLessThanOrEqualTo("email", firebase::firestore::FieldValue::String(fb.filter_email + "\xEF\xA3\xBF"));
    } else {
        q = q.OrderBy("timestamp", fb.sort_desc ? firebase::firestore::Query::Direction::kDescending : firebase::firestore::Query::Direction::kAscending);
    }
    if (!fb.snapshots.empty()) { auto last_docs = fb.snapshots.back().documents(); if (!last_docs.empty()) q = q.StartAfter(last_docs.back()); }
    int limit = 10;
    auto reg = q.Limit(limit).AddSnapshotListener([&fb, page_index, limit, on_update](const firebase::firestore::QuerySnapshot& snap, firebase::firestore::Error err, const std::string& m) {
        if (err != firebase::firestore::Error::kErrorOk) return;
        {
            std::lock_guard<std::mutex> lock(fb.mutex);
            if (page_index < fb.snapshots.size()) fb.snapshots[page_index] = snap;
            else if (page_index == fb.snapshots.size()) fb.snapshots.push_back(snap);
            if (page_index == fb.snapshots.size() - 1) { fb.has_more = (snap.documents().size() >= (size_t)limit); fb.is_loading = false; }
        }
        on_update();
    });
    fb.listeners.push_back(std::move(reg));
}

void StartQuery(FirebaseData& fb, const std::function<void()>& on_update) {
    { std::lock_guard<std::mutex> lock(fb.mutex); for (auto& l : fb.listeners) l.Remove(); fb.listeners.clear(); fb.snapshots.clear(); fb.is_loading = false; fb.has_more = true; }
    FetchNextPage(fb, on_update);
}

int main(int argc, char** argv) {
  try {
      GetLogFilename() = "sendapp.log"; std::ofstream(GetLogFilename(), std::ios::trunc);
      auto screen = ScreenInteractive::Fullscreen(); std::atomic<bool> started{false};
      auto on_update = [&]() { if (started) screen.Post(Event::Custom); };

      FirebaseData addr_fb, log_fb; addr_fb.collection = "addressbook"; log_fb.collection = "joblog";
      std::string api_key;
      const char* key = std::getenv("FB_API_KEY"); if (!key) key = std::getenv("API_KEY");
      if (key) api_key = key; else {
          std::ifstream f(GetExecutableDir() + "app.conf"); std::string l;
          while (std::getline(f, l)) if (l.find("API_KEY=") == 0) { api_key = l.substr(8); break; }
      }

      firebase::App* app = nullptr;
      if (!api_key.empty()) {
          firebase::AppOptions opts; opts.set_api_key(api_key.c_str()); opts.set_app_id("1:646759465365:web:fc72f377308486d6e8769c"); opts.set_project_id("riot26-70125");
#ifdef _WIN32
          int pid = _getpid();
#else
          int pid = getpid();
#endif
          app = firebase::App::Create(opts, ("SendApp_" + std::to_string(pid)).c_str());
          if (app) {
              auto db = firebase::firestore::Firestore::GetInstance(app);
              firebase::firestore::Settings s; s.set_persistence_enabled(true); db->set_settings(s);
              addr_fb.db = db; log_fb.db = db;
              StartQuery(addr_fb, on_update); StartQuery(log_fb, on_update);
          }
      }

      auto show_picker = std::make_shared<bool>(false); auto show_sent_dialog = std::make_shared<bool>(false);
      static std::string sent_msg = ""; static std::string scan_new_email = "";
      static std::vector<std::pair<std::string, std::string>> scan_confirmed_emails;

      static std::string p_name = "", p_email = ""; static int p_sort = 2; static bool p_desc = true;
      auto p_input_name = Input(&p_name, "Alice"), p_input_email = Input(&p_email, "Mail");
      auto p_list = Container::Vertical({});
      auto update_picker = [&]() {
          std::string fld = (p_sort == 0) ? "name" : (p_sort == 1) ? "email" : "timestamp";
          // 選択カラム以外のフィルタは無視。空文字になった場合はフィルタ解除として扱われる。
          std::string applied_p_name = (p_sort == 0) ? p_name : "";
          std::string applied_p_email = (p_sort == 1) ? p_email : "";

          if (addr_fb.sort_field != fld || addr_fb.sort_desc != p_desc || addr_fb.filter_name != applied_p_name || addr_fb.filter_email != applied_p_email) {
              addr_fb.sort_field = fld; addr_fb.sort_desc = p_desc; addr_fb.filter_name = applied_p_name; addr_fb.filter_email = applied_p_email; StartQuery(addr_fb, on_update);
          }
          size_t total = 0; { std::lock_guard<std::mutex> l(addr_fb.mutex); for(const auto& s : addr_fb.snapshots) total += s.documents().size(); }
          p_list->DetachAllChildren();
          for (size_t i = 0; i < total; ++i) {
              int idx = (int)i; 
              ButtonOption opt = ButtonOption::Ascii();
              opt.transform = [idx, &addr_fb](const EntryState& s) {
                  auto el = hbox({ text(GetData(addr_fb, idx, "name"))|size(WIDTH,EQUAL,28), text(GetData(addr_fb, idx, "email"))|flex, text(GetData(addr_fb, idx, "timestamp"))|size(WIDTH,EQUAL,20) });
                  if (s.focused) { 
                      el = el | inverted; 
                      if (!addr_fb.is_loading && addr_fb.has_more) {
                          size_t current_total = 0;
                          { std::lock_guard<std::mutex> l(addr_fb.mutex); for (const auto& snap : addr_fb.snapshots) current_total += snap.documents().size(); }
                          if (idx >= (int)current_total - 2) FetchNextPage(addr_fb, []{});
                      }
                  }
                  return el;
              };
              auto btn = Button("", [&, idx] {
                  std::string n = GetData(addr_fb, idx, "name"), e = GetData(addr_fb, idx, "email");
                  if(!e.empty()) { if (std::find_if(scan_confirmed_emails.begin(), scan_confirmed_emails.end(), [&](const auto& p){ return p.second == e; }) == scan_confirmed_emails.end()) scan_confirmed_emails.push_back({n, e}); *show_picker = false; on_update(); }
              }, opt);
              p_list->Add(btn);
          }
      };

      auto p_btn_n = Button("Name", [&]{ if(p_sort==0) p_desc=!p_desc; else {p_sort=0; p_desc=false; p_email="";} on_update(); }, ButtonOption::Ascii());
      auto p_btn_m = Button("Mail", [&]{ if(p_sort==1) p_desc=!p_desc; else {p_sort=1; p_desc=false; p_name="";} on_update(); }, ButtonOption::Ascii());
      auto p_btn_t = Button("Time", [&]{ if(p_sort==2) p_desc=!p_desc; else {p_sort=2; p_desc=true; p_name=p_email="";} on_update(); }, ButtonOption::Ascii());
      auto p_close = Button("[Close]", [&] { *show_picker = false; }, ButtonOption::Ascii());
      auto picker_renderer = Renderer(Container::Vertical({ Container::Horizontal({p_btn_n, p_input_name, p_btn_m, p_input_email, p_btn_t}), p_list, p_close }), [&] {
          return vbox({ text("Select Address")|center, separator(), hbox({ p_btn_n->Render()|size(WIDTH,EQUAL,28), p_btn_m->Render()|flex, p_btn_t->Render()|size(WIDTH,EQUAL,20) }), separator(), p_list->Render()|vscroll_indicator|frame|size(HEIGHT,EQUAL,10), separator(), hbox({ filler(), p_close->Render() }) })|border|size(WIDTH,GREATER_THAN,90)|clear_under;
      });

      auto scan_list = Container::Vertical({});
      auto new_email_in = Input(&scan_new_email, "");
      auto add_manual = [&] { if (!scan_new_email.empty()) { if (std::find_if(scan_confirmed_emails.begin(), scan_confirmed_emails.end(), [&](const auto& p){ return p.second == scan_new_email; }) == scan_confirmed_emails.end()) scan_confirmed_emails.push_back({"", scan_new_email}); scan_new_email = ""; on_update(); } };
      auto new_enter_btn = Button("[Enter]", add_manual, ButtonOption::Ascii());
      auto new_addr_btn = Button("[Address Book]", [&] { p_name = ""; p_email = ""; update_picker(); *show_picker = true; }, ButtonOption::Ascii());
      auto log_list = Container::Vertical({});
      auto refresh_logs = [&]() {
          size_t total = 0; { std::lock_guard<std::mutex> l(log_fb.mutex); for(const auto& s : log_fb.snapshots) total += s.documents().size(); }
          log_list->DetachAllChildren();
          for (size_t i = 0; i < total; ++i) { int idx = (int)i; log_list->Add(Renderer([=, &log_fb] { return hbox({ text(GetData(log_fb, idx, "timestamp"))|size(WIDTH,EQUAL,20), text(" ")|size(WIDTH,EQUAL,10), text(GetData(log_fb, idx, "email"))|flex }); })); }
      };
      auto refresh_scan = [&]() {
          scan_list->DetachAllChildren();
          for (const auto& pair : scan_confirmed_emails) {
              auto rm = Button("[Remove]", [&, pair] { auto it = std::find_if(scan_confirmed_emails.begin(), scan_confirmed_emails.end(), [&](const auto& p){ return p.second == pair.second; }); if (it != scan_confirmed_emails.end()) { scan_confirmed_emails.erase(it); on_update(); } }, ButtonOption::Ascii());
              scan_list->Add(Renderer(rm, [pair, rm] { return hbox({ text("- " + (pair.first.empty() ? pair.second : pair.first + " - " + pair.second))|flex, rm->Render() }); }));
          }
      };

      auto clean_btn = Button("[Clean Logs]", [&] { if(log_fb.db) log_fb.db->Collection("joblog").Get().OnCompletion([&](const firebase::Future<firebase::firestore::QuerySnapshot>& f){ if(f.error()==0) for(const auto& d : f.result()->documents()) d.reference().Delete(); StartQuery(log_fb, on_update); }); }, ButtonOption::Ascii());
      auto send_btn = Button("[<Send>]", [&] {
          if (scan_confirmed_emails.empty() || !log_fb.db) return;
          std::vector<firebase::firestore::FieldValue> ev; for(const auto& p : scan_confirmed_emails) ev.push_back(firebase::firestore::FieldValue::String(p.second));
          std::unordered_map<std::string, firebase::firestore::FieldValue> d; d["task"] = firebase::firestore::FieldValue::String("SEND"); d["email"] = firebase::firestore::FieldValue::Array(ev); d["timestamp"] = firebase::firestore::FieldValue::ServerTimestamp();
          log_fb.db->Collection("joblog").Add(d).OnCompletion([&](const firebase::Future<firebase::firestore::DocumentReference>&){ StartQuery(log_fb, on_update); });
          sent_msg = "Sent to " + std::to_string(scan_confirmed_emails.size()) + " recipients."; *show_sent_dialog = true; scan_confirmed_emails.clear(); on_update();
      }, ButtonOption::Ascii());
      auto close_btn = Button("[Close]", screen.ExitLoopClosure(), ButtonOption::Ascii());

      auto scan_renderer = Renderer(Container::Vertical({ new_email_in, new_enter_btn, new_addr_btn, scan_list, clean_btn, send_btn, close_btn }), [&] {
          return vbox({ text("Send Mail"), separator(), hbox({ text("    To: "), vbox({ text("╭───────────────────────────────────────╮"), hbox({ text("│ "), new_email_in->Render()|size(WIDTH,EQUAL,37), text(" │") }), text("╰───────────────────────────────────────╯") }), text("              "), new_enter_btn->Render(), text("  /  "), new_addr_btn->Render() }), scan_list->Render()|vscroll_indicator|frame|size(HEIGHT,LESS_THAN,10), filler(), separator(), hbox({ text(" Send Log"), filler(), clean_btn->Render() }), log_list->Render()|frame|size(HEIGHT,EQUAL,5), separator(), hbox({ text("  Status: " + (addr_fb.db ? std::string("Connected") : std::string("Disconnected"))), filler(), send_btn->Render(), text("  "), close_btn->Render() }) })|border;
      });

      auto sent_ok = Button("[OK]", [&] { *show_sent_dialog = false; }, ButtonOption::Ascii());
      auto root_renderer = Renderer(scan_renderer, [&] {
          Element c = scan_renderer->Render(); if (*show_picker) return dbox({ c, picker_renderer->Render()|center });
          if (*show_sent_dialog) return dbox({ c, vbox({ text("Notification")|center, separator(), text(sent_msg)|center, separator(), sent_ok->Render()|center })|border|size(WIDTH,GREATER_THAN,40)|clear_under|center });
          return c;
      });

      auto final_comp = CatchEvent(root_renderer, [&](Event e) {
          if (e == Event::Custom) { if (*show_picker) update_picker(); refresh_scan(); refresh_logs(); return true; }
          if (e == Event::Character("\x10")) { auto cap = Screen::Create(Terminal::Size()); Render(cap, root_renderer->Render()); SaveSnapshot("sendapp", cap.ToString()); return true; }
          if ((e == Event::Character('q') || e == Event::Escape) && !*show_picker && !*show_sent_dialog) { screen.Exit(); return true; }
          if (*show_picker) return picker_renderer->OnEvent(e); if (*show_sent_dialog) return sent_ok->OnEvent(e);
          if (e == Event::Return && new_email_in->Focused()) { add_manual(); return true; }
          return scan_renderer->OnEvent(e);
      });

      started = true; refresh_logs(); screen.Loop(final_comp);
      for(auto& l : addr_fb.listeners) l.Remove(); for(auto& l : log_fb.listeners) l.Remove();
      if(app) delete app;
  } catch (const std::exception& e) { std::cerr << "EXCEPTION: " << e.what() << std::endl; return 1; }
  return 0;
}