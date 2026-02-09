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
#define NOMINMAX
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

// テーブルの1行（名前、メール、時間、操作ボタン）を構成する
Element MakeTableRow(Element name, Element email, Element time, Element op) {
    return hbox({
        std::move(name)  | size(WIDTH, EQUAL, 28),
        std::move(email) | flex,
        std::move(time)  | size(WIDTH, EQUAL, 16),
        std::move(op)    | size(WIDTH, EQUAL, 10) | center,
    });
}

// ランダムな名前（敬称、名、姓の組み合わせ）を生成する
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

// 名前を元に小文字・ドット区切りのランダムなメールアドレスを生成する
std::string GenerateRandomEmail(const std::string& name) {
  std::string e = name;
  std::transform(e.begin(), e.end(), e.begin(), [](unsigned char c) { return (unsigned char)::tolower(c); });
  std::replace(e.begin(), e.end(), ' ', '.');
  return e + "@example.com";
}

// 環境変数または設定ファイルからFirebaseのAPIキーを読み込む
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

// SDKのインスタンス群を保持する構造体（メソッドを持たないデータ保持用）
struct FirebaseData {
    firebase::App* app = nullptr;
    firebase::firestore::Firestore* db = nullptr;
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

struct AppState {
    std::string api_key;
    int nAddr;
    std::atomic<bool> started{false};
    FirebaseData fb;
};

// 指定したインデックスのデータをSDKのスナップショットから直接取得する
std::string GetValueFromSnapshots(FirebaseData& fb, size_t index, const std::string& field) {
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
            return (data.count(field) && data[field].is_string()) ? data[field].string_value() : "";
        }
        base += docs.size();
    }
    return "";
}

// クエリを初期化し、最初のページを取得する
void StartQuery(FirebaseData& fb, const std::function<void()>& on_update);

// 次のページを取得し、リスナーを登録する
void FetchNextPage(FirebaseData& fb, const std::function<void()>& on_update) {
    std::lock_guard<std::mutex> lock(fb.mutex);
    if (!fb.db || fb.is_loading || !fb.has_more) return;
    fb.is_loading = true;

    size_t page_index = fb.snapshots.size();
    firebase::firestore::Query query = fb.db->Collection("addressbook");

    if (fb.sort_field == "name") {
        query = query.OrderBy("name", fb.sort_desc ? firebase::firestore::Query::Direction::kDescending : firebase::firestore::Query::Direction::kAscending)
            .WhereGreaterThanOrEqualTo("name", firebase::firestore::FieldValue::String(fb.filter_name))
            .WhereLessThanOrEqualTo("name", firebase::firestore::FieldValue::String(fb.filter_name + "\xEF\xA3\xBF"));
    } else if (fb.sort_field == "email") {
        query = query.OrderBy("email", fb.sort_desc ? firebase::firestore::Query::Direction::kDescending : firebase::firestore::Query::Direction::kAscending)
            .WhereGreaterThanOrEqualTo("email", firebase::firestore::FieldValue::String(fb.filter_email))
            .WhereLessThanOrEqualTo("email", firebase::firestore::FieldValue::String(fb.filter_email + "\xEF\xA3\xBF"));
        }
    } else {
        query = query.OrderBy("timestamp", fb.sort_desc ? firebase::firestore::Query::Direction::kDescending : firebase::firestore::Query::Direction::kAscending);
    }

    if (!fb.snapshots.empty()) {
        auto last_docs = fb.snapshots.back().documents();
        if (!last_docs.empty()) query = query.StartAfter(last_docs.back());
    }

    int limit = (page_index == 0) ? 20 : 10;
    auto registration = query.Limit(limit).AddSnapshotListener([&fb, page_index, limit, on_update](const firebase::firestore::QuerySnapshot& snapshot, firebase::firestore::Error error, const std::string& msg) {
        if (error != firebase::firestore::Error::kErrorOk) { Log("Firestore Error: " + msg); return; }
        {
            std::lock_guard<std::mutex> lock(fb.mutex);
            if (page_index < fb.snapshots.size()) {
                fb.snapshots[page_index] = snapshot;
            } else if (page_index == fb.snapshots.size()) {
                fb.snapshots.push_back(snapshot);
            }
            
            auto docs = snapshot.documents();
            std::string res_log = "Firestore Update (Page=" + std::to_string(page_index) + "): Count=" + std::to_string(docs.size());
            if (!docs.empty()) res_log += " FirstID=" + docs[0].id();
            Log(res_log);

            if (page_index == fb.snapshots.size() - 1) {
                fb.has_more = (snapshot.documents().size() >= (size_t)limit);
                fb.is_loading = false;
            }
        }
        on_update();
    });
    Log("Firestore Listen Started: Collection=addressbook Page=" + std::to_string(page_index) + " Limit=" + std::to_string(limit) + " Sort=" + fb.sort_field + " FilterName=" + fb.filter_name);
    fb.listeners.push_back(std::move(registration));
}

void StartQuery(FirebaseData& fb, const std::function<void()>& on_update) {
    {
        std::lock_guard<std::mutex> lock(fb.mutex);
        for (auto& l : fb.listeners) l.Remove();
        fb.listeners.clear();
        fb.snapshots.clear();
        fb.is_loading = false;
        fb.has_more = true;
    }
    FetchNextPage(fb, on_update);
}

// サービスからデータを取得し、FTXUIのコンポーネントとして住所録一覧を再構築する
void RefreshAddressList(FirebaseData& fb, Component rows, int sort_col, bool sort_desc, std::string f_name, std::string f_email, size_t& last_count, int nAddr, const std::function<void()>& on_update) {
    std::string new_field = (sort_col == 0) ? "name" : (sort_col == 1) ? "email" : "timestamp";
    // 選択されていないカラムのフィルタ文字列は無視（空文字として扱う）
    // 入力が空になった場合もここで空文字が設定され、後続のクエリ構築でフィルタなし（全件取得）となる
    std::string applied_f_name = (sort_col == 0) ? f_name : "";
    std::string applied_f_email = (sort_col == 1) ? f_email : "";

    // Debug Log
    // Log("Refresh: Sort=" + new_field + " FilterName='" + applied_f_name + "' CurrentFilter='" + fb.filter_name + "'");

    if (fb.sort_field != new_field || fb.sort_desc != sort_desc || fb.filter_name != applied_f_name || fb.filter_email != applied_f_email) {
        Log("Filter Changed: '" + fb.filter_name + "' -> '" + applied_f_name + "'");
        fb.sort_field = new_field; fb.sort_desc = sort_desc; fb.filter_name = applied_f_name; fb.filter_email = applied_f_email;
        StartQuery(fb, on_update);
    }

    size_t total = 0;
    {
        std::lock_guard<std::mutex> lock(fb.mutex);
        for (const auto& s : fb.snapshots) total += s.documents().size();
    }
    
    if (!fb.is_loading && total == last_count && rows->ChildCount() > 0) return;
    rows->DetachAllChildren(); 
    last_count = total;

    for (size_t i = 0; i < total; ++i) {
        int idx = (int)i;
        std::string name_val = GetValueFromSnapshots(fb, idx, "name");
        std::string email_val = GetValueFromSnapshots(fb, idx, "email");
        std::string time_val = GetValueFromSnapshots(fb, idx, "timestamp");
        
        std::string contact_id = "";
        {
            std::lock_guard<std::mutex> lock(fb.mutex);
            size_t base = 0;
            for(const auto& s : fb.snapshots) {
                auto docs = s.documents();
                if(i < base + docs.size()) { contact_id = docs[i - base].id(); break; }
                base += docs.size();
            }
        }

        auto select_opt = ButtonOption::Ascii();
        select_opt.transform = [name_val, email_val, time_val](const EntryState& s) {
            return hbox({ text(name_val) | size(WIDTH, EQUAL, 28), text(email_val) | flex, text(time_val) | size(WIDTH, EQUAL, 16) });
        };
        auto select_btn = Button("", []{}, select_opt);
        
        auto remove_btn = Button("[Remove]", [&fb, contact_id, on_update] { 
            if(!contact_id.empty() && fb.db) fb.db->Collection("addressbook").Document(contact_id).Delete().OnCompletion([&fb, on_update](const firebase::Future<void>&){ StartQuery(fb, on_update); });
        }, ButtonOption::Ascii());

        auto row = Renderer(Container::Horizontal({select_btn, remove_btn}), [idx, &fb, select_btn, remove_btn, on_update] {
            bool is_selected = select_btn->Focused() || remove_btn->Focused();
            if (is_selected && !fb.is_loading && fb.has_more) {
                size_t current_total = 0;
                { std::lock_guard<std::mutex> lock(fb.mutex); for (const auto& s : fb.snapshots) current_total += s.documents().size(); }
                if (idx >= (int)current_total - 2) FetchNextPage(fb, on_update);
            }
            return hbox({ text(is_selected ? "> " : "  "), select_btn->Render() | flex, remove_btn->Render() | size(WIDTH, EQUAL, 10) | center });
        });
        rows->Add(CatchEvent(row, [select_btn](Event e) { if (e.is_mouse() && e.mouse().button == Mouse::Left && e.mouse().motion == Mouse::Pressed) { select_btn->TakeFocus(); } return false; }));
    }
    if (fb.has_more || fb.is_loading) {
        rows->Add(Renderer([&fb, nAddr, on_update] { 
            size_t current_total = 0;
            { std::lock_guard<std::mutex> lock(fb.mutex); for (const auto& s : fb.snapshots) current_total += s.documents().size(); }
            if (!fb.is_loading && (int)current_total < nAddr && fb.has_more) FetchNextPage(fb, on_update);
            return hbox({ filler(), text("Loading..."), filler() });
        }));
    }
}

// アプリケーションのエントリポイント：UIの初期化とメインループを実行する
int main(int argc, char** argv) {
  try {
      Log("--- AddrApp Starting ---");
      std::ofstream(GetLogFilename(), std::ios::trunc);
      Log("--- Log File Truncated ---");
      auto screen = ScreenInteractive::Fullscreen();
      AppState state;
      auto on_update = [&]() { if (state.started) screen.Post(Event::Custom); };

      state.nAddr = (std::max)(10, Terminal::Size().dimy + 5);
      state.api_key = LoadApiKey();
      if (!state.api_key.empty()) {
          firebase::AppOptions opts;
          opts.set_api_key(state.api_key.c_str());
          opts.set_app_id("1:646759465365:web:fc72f377308486d6e8769c");
          opts.set_project_id("riot26-70125");
#ifdef _WIN32
          int pid = _getpid();
#else
          int pid = getpid();
#endif
          state.fb.app = firebase::App::Create(opts, ("AddrApp_" + std::to_string(pid)).c_str());
          if (state.fb.app) {
              state.fb.db = firebase::firestore::Firestore::GetInstance(state.fb.app);
              firebase::firestore::Settings s; s.set_persistence_enabled(true);
              state.fb.db->set_settings(s);
              StartQuery(state.fb, on_update);
          }
      }

            static std::string f_name = "", f_email = "";

            static int sort_col = 2; static bool sort_desc = true;

            

            InputOption in_opt;

            in_opt.on_change = on_update;

            auto addr_in_name = Input(&f_name, "Filter Name", in_opt);

            auto addr_in_email = Input(&f_email, "Filter Email", in_opt);

            

            auto rows = Container::Vertical({});
      size_t last_count = 0;
      auto refresh = [&]() { RefreshAddressList(state.fb, rows, sort_col, sort_desc, f_name, f_email, last_count, state.nAddr, on_update); };

      auto btn_n = Button("Name", [&]{ if(sort_col==0) sort_desc=!sort_desc; else {sort_col=0; sort_desc=false; f_email="";} on_update(); }, ButtonOption::Ascii());
      auto btn_m = Button("Mail", [&]{ if(sort_col==1) sort_desc=!sort_desc; else {sort_col=1; sort_desc=false; f_name="";} on_update(); }, ButtonOption::Ascii());
      auto btn_t = Button("Time", [&]{ if(sort_col==2) sort_desc=!sort_desc; else {sort_col=2; sort_desc=true; f_name=f_email="";} on_update(); }, ButtonOption::Ascii());

      static std::string n_name = GenerateRandomName(), n_email = GenerateRandomEmail(n_name);
      auto name_in = Input(&n_name, "Name"), email_in = Input(&n_email, "Email");
      auto add_btn = Button("[Add]", [&] { 
          if (!n_name.empty() && state.fb.db) { 
              std::unordered_map<std::string, firebase::firestore::FieldValue> d;
              d["name"] = firebase::firestore::FieldValue::String(n_name); d["email"] = firebase::firestore::FieldValue::String(n_email); d["timestamp"] = firebase::firestore::FieldValue::ServerTimestamp();
              state.fb.db->Collection("addressbook").Document(n_name).Set(d).OnCompletion([&](const firebase::Future<void>&){ StartQuery(state.fb, on_update); });
              n_name = GenerateRandomName(); n_email = GenerateRandomEmail(n_name); 
          } 
      }, ButtonOption::Ascii());
      
      auto close_btn = Button("[Close]", [&] { screen.Exit(); }, ButtonOption::Ascii());
      auto main_ui = Renderer(Container::Vertical({ Container::Horizontal({ btn_n, btn_m, btn_t, addr_in_name, addr_in_email }), rows, Container::Horizontal({name_in, email_in, add_btn}), close_btn }), [&] {
          auto sort_ind = [&](int col) { return (sort_col != col) ? text("") : text(sort_desc ? " v" : " ^"); };
          return vbox({
              hbox({ hbox({ btn_n->Render(), sort_ind(0), text(" ["), (sort_col==0?addr_in_name->Render():text("          ")), text("]") })|size(WIDTH,EQUAL,28), hbox({ btn_m->Render(), sort_ind(1), text(" ["), (sort_col==1?addr_in_email->Render():text("          ")), text("]") })|flex, hbox({ btn_t->Render(), sort_ind(2) })|size(WIDTH,EQUAL,16), text("          ") }),
              separator(), rows->Render() | vscroll_indicator | frame | flex, separator(),
              hbox({ name_in->Render()|size(WIDTH,EQUAL,28), email_in->Render()|flex, text("(Now)")|size(WIDTH,EQUAL,16), add_btn->Render()|size(WIDTH,EQUAL,10)|center }),
              separator(), hbox({ text(state.fb.db ? "Status: Connected" : "Status: Disconnected"), filler(), close_btn->Render() })
          }) | border;
      });

      auto final_component = CatchEvent(main_ui, [&](Event e) {
          if (e == Event::Custom) { refresh(); return true; }
          if (e == Event::Character("\x10")) { auto cap = Screen::Create(Terminal::Size()); Render(cap, main_ui->Render()); SaveSnapshot("addrapp", cap.ToString()); return true; }
          if (e == Event::Character('q') || e == Event::Escape) { screen.Exit(); return true; }
          return main_ui->OnEvent(e);
      });

      state.started = true; refresh(); screen.Loop(final_component);
      for (auto& l : state.fb.listeners) l.Remove();
      if (state.fb.app) delete state.fb.app;
  } catch (const std::exception& e) { std::cerr << "EXCEPTION: " << e.what() << std::endl; return 1; }
  return 0;
}
