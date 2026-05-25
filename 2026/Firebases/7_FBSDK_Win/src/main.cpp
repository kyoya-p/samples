#include <algorithm>
#include <chrono>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <memory>
#include <string>
#include <vector>
#include <thread>
#include <mutex>
#include <atomic>
#include <unordered_map>

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
#include "firebase/auth.h"
#include "utils.hpp"

using namespace ftxui;

// --- Data Models ---
struct AddressEntry {
    std::string id;
    std::string name;
    std::string address;
    bool selected = false;
};

struct Counters {
    int64_t print_count = 0;
    int64_t send_count = 0;
};

// --- Firebase Manager ---
struct FirebaseData {
    firebase::App* app = nullptr;
    firebase::firestore::Firestore* db = nullptr;
    std::vector<AddressEntry> address_book;
    Counters counters;
    std::mutex mutex;
    bool is_loading = false;
    
    firebase::firestore::ListenerRegistration addr_listener;
    firebase::firestore::ListenerRegistration counter_listener;
};

enum class ScreenType { Home, Copy, Scan, Settings };

struct AppState {
    ScreenType current_screen = ScreenType::Home;
    std::string api_key;
    std::atomic<bool> started{false};
    
    // Copy Screen State
    std::string copy_quantity = "1";
    std::string copy_message = "";
    
    // Scan Screen State
    std::string scan_message = "";
    
    // Settings Screen State
    std::string edit_name = "";
    std::string edit_address = "";
    std::string selected_addr_id = "";
};

// --- Helpers ---
std::string LoadApiKey() {
    const char* key = std::getenv("FB_API_KEY");
    if (key) return std::string(key);
    // Fallback to app.conf
    std::ifstream conf_file(GetExecutableDir() + "app.conf");
    std::string line;
    while (std::getline(conf_file, line)) {
        if (line.find("API_KEY=") == 0) return line.substr(8);
    }
    return "";
}

void StartFirebase(FirebaseData& fb, const std::string& api_key, const std::function<void()>& on_update) {
    firebase::AppOptions opts;
    opts.set_api_key(api_key.c_str());
    opts.set_project_id("riot26-70125");
    opts.set_app_id("1:646759465365:web:fc72f377308486d6e8769c");

#ifdef _WIN32
    int pid = _getpid();
#else
    int pid = getpid();
#endif
    fb.app = firebase::App::Create(opts, ("MFPApp_" + std::to_string(pid)).c_str());
    if (!fb.app) return;

    fb.db = firebase::firestore::Firestore::GetInstance(fb.app);
    
    // Listen for address book
    fb.addr_listener = fb.db->Collection("addressbook").AddSnapshotListener([&fb, on_update](const firebase::firestore::QuerySnapshot& snapshot, firebase::firestore::Error error, const std::string& msg) {
        if (error != firebase::firestore::Error::kErrorOk) return;
        std::lock_guard<std::mutex> lock(fb.mutex);
        fb.address_book.clear();
        for (const auto& doc : snapshot.documents()) {
            AddressEntry entry;
            entry.id = doc.id();
            auto data = doc.GetData();
            if (data.count("name") && data["name"].is_string()) entry.name = data["name"].string_value();
            if (data.count("address") && data["address"].is_string()) entry.address = data["address"].string_value();
            fb.address_book.push_back(entry);
        }
        on_update();
    });

    // Listen for counters
    fb.counter_listener = fb.db->Collection("counters").Document("device001").AddSnapshotListener([&fb, on_update](const firebase::firestore::DocumentSnapshot& snapshot, firebase::firestore::Error error, const std::string& msg) {
        if (error != firebase::firestore::Error::kErrorOk || !snapshot.exists()) return;
        std::lock_guard<std::mutex> lock(fb.mutex);
        auto data = snapshot.GetData();
        if (data.count("print_count") && data["print_count"].is_integer()) fb.counters.print_count = data["print_count"].integer_value();
        if (data.count("send_count") && data["send_count"].is_integer()) fb.counters.send_count = data["send_count"].integer_value();
        on_update();
    });
}

// --- UI Components ---

int main() {
    auto screen = ScreenInteractive::Fullscreen();
    FirebaseData fb;
    AppState state;
    auto on_update = [&]() { if (state.started) screen.Post(Event::Custom); };

    state.api_key = LoadApiKey();
    if (!state.api_key.empty()) {
        StartFirebase(fb, state.api_key, on_update);
    }

    // --- Home Screen ---
    auto btn_to_copy = Button("Copy", [&] { state.current_screen = ScreenType::Copy; on_update(); }, ButtonOption::Animated());
    auto btn_to_scan = Button("Scan", [&] { state.current_screen = ScreenType::Scan; on_update(); }, ButtonOption::Animated());
    auto btn_to_settings = Button("Settings", [&] { state.current_screen = ScreenType::Settings; on_update(); }, ButtonOption::Animated());
    
    auto home_renderer = Renderer(Container::Vertical({ btn_to_copy, btn_to_scan, btn_to_settings }), [&] {
        return vbox({
            text("MFP Control Panel") | bold | center,
            separator(),
            filler(),
            hbox({ filler(), btn_to_copy->Render() | size(WIDTH, EQUAL, 20), filler() }),
            hbox({ filler(), btn_to_scan->Render() | size(WIDTH, EQUAL, 20), filler() }),
            hbox({ filler(), btn_to_settings->Render() | size(WIDTH, EQUAL, 20), filler() }),
            filler()
        }) | border;
    });

    // --- Copy Screen ---
    auto input_qty = Input(&state.copy_quantity, "Qty");
    auto btn_copy_exec = Button("Copy Start", [&] {
        int qty = 0;
        try { qty = std::stoi(state.copy_quantity); } catch(...) { qty = 0; }
        if (qty > 0 && fb.db) {
            fb.db->RunTransaction([qty](firebase::firestore::Transaction* transaction, std::string* error_msg) -> firebase::firestore::Error {
                auto doc = transaction->Get(firebase::firestore::Firestore::GetInstance()->Collection("counters").Document("device001"), error_msg);
                int64_t current = 0;
                if (doc.exists()) current = doc.GetData()["print_count"].integer_value();
                std::unordered_map<std::string, firebase::firestore::FieldValue> update;
                update["print_count"] = firebase::firestore::FieldValue::Integer(current + qty);
                transaction->Set(firebase::firestore::Firestore::GetInstance()->Collection("counters").Document("device001"), update, firebase::firestore::SetOptions::Merge());
                return firebase::firestore::kErrorOk;
            }).OnCompletion([&, qty](const firebase::Future<void>& future) {
                state.copy_message = "Copied " + std::to_string(qty) + " pages.";
                on_update();
            });
        }
    }, ButtonOption::Animated());
    auto btn_copy_to_home = Button("Home", [&] { state.current_screen = ScreenType::Home; state.copy_message = ""; on_update(); }, ButtonOption::Animated());
    
    auto copy_renderer = Renderer(Container::Vertical({ input_qty, btn_copy_exec, btn_copy_to_home }), [&] {
        return vbox({
            text("Copy") | bold | center,
            separator(),
            hbox({ text("Quantity: "), input_qty->Render() | size(WIDTH, EQUAL, 10) }),
            btn_copy_exec->Render(),
            text(state.copy_message) | color(Color::Green),
            filler(),
            btn_copy_to_home->Render()
        }) | border;
    });

    // --- Scan Screen ---
    auto addr_list_scan = Container::Vertical({});
    auto btn_scan_exec = Button("Send Start", [&] {
        int count = 0;
        {
            std::lock_guard<std::mutex> lock(fb.mutex);
            for (auto& entry : fb.address_book) if (entry.selected) count++;
        }
        if (count > 0 && fb.db) {
            fb.db->RunTransaction([count](firebase::firestore::Transaction* transaction, std::string* error_msg) -> firebase::firestore::Error {
                auto doc = transaction->Get(firebase::firestore::Firestore::GetInstance()->Collection("counters").Document("device001"), error_msg);
                int64_t current = 0;
                if (doc.exists()) current = doc.GetData()["send_count"].integer_value();
                std::unordered_map<std::string, firebase::firestore::FieldValue> update;
                update["send_count"] = firebase::firestore::FieldValue::Integer(current + count);
                transaction->Set(firebase::firestore::Firestore::GetInstance()->Collection("counters").Document("device001"), update, firebase::firestore::SetOptions::Merge());
                return firebase::firestore::kErrorOk;
            }).OnCompletion([&, count](const firebase::Future<void>& future) {
                state.scan_message = "Sent to " + std::to_string(count) + " destinations.";
                on_update();
            });
        }
    }, ButtonOption::Animated());
    auto btn_scan_to_home = Button("Home", [&] { state.current_screen = ScreenType::Home; state.scan_message = ""; on_update(); }, ButtonOption::Animated());

    auto scan_renderer = Renderer(Container::Vertical({ addr_list_scan, btn_scan_exec, btn_scan_to_home }), [&] {
        addr_list_scan->DetachAllChildren();
        std::lock_guard<std::mutex> lock(fb.mutex);
        for (auto& entry : fb.address_book) {
            addr_list_scan->Add(Checkbox(entry.name + " (" + entry.address + ")", &entry.selected));
        }
        return vbox({
            text("Scan") | bold | center,
            separator(),
            text("Select Destinations:"),
            addr_list_scan->Render() | vscroll_indicator | frame | flex,
            btn_scan_exec->Render(),
            text(state.scan_message) | color(Color::Green),
            filler(),
            btn_scan_to_home->Render()
        }) | border;
    });

    // --- Settings Screen ---
    auto input_name = Input(&state.edit_name, "Name");
    auto input_addr = Input(&state.edit_address, "Address");
    auto btn_addr_add = Button("Add/Update", [&] {
        if (!state.edit_name.empty() && fb.db) {
            std::unordered_map<std::string, firebase::firestore::FieldValue> data;
            data["name"] = firebase::firestore::FieldValue::String(state.edit_name);
            data["address"] = firebase::firestore::FieldValue::String(state.edit_address);
            if (state.selected_addr_id.empty()) {
                fb.db->Collection("addressbook").Add(data);
            } else {
                fb.db->Collection("addressbook").Document(state.selected_addr_id).Set(data, firebase::firestore::SetOptions::Merge());
            }
            state.edit_name = ""; state.edit_address = ""; state.selected_addr_id = "";
        }
    }, ButtonOption::Animated());
    auto btn_addr_delete = Button("Delete Selected", [&] {
        if (!state.selected_addr_id.empty() && fb.db) {
            fb.db->Collection("addressbook").Document(state.selected_addr_id).Delete();
            state.edit_name = ""; state.edit_address = ""; state.selected_addr_id = "";
        }
    }, ButtonOption::Animated());
    
    auto addr_list_settings = Container::Vertical({});
    auto btn_settings_to_home = Button("Home", [&] { state.current_screen = ScreenType::Home; on_update(); }, ButtonOption::Animated());

    auto settings_renderer = Renderer(Container::Vertical({ addr_list_settings, input_name, input_addr, btn_addr_add, btn_addr_delete, btn_settings_to_home }), [&] {
        addr_list_settings->DetachAllChildren();
        std::lock_guard<std::mutex> lock(fb.mutex);
        for (auto& entry : fb.address_book) {
            auto btn = Button(entry.name + " (" + entry.address + ")", [&, entry] {
                state.edit_name = entry.name;
                state.edit_address = entry.address;
                state.selected_addr_id = entry.id;
            }, ButtonOption::Simple());
            addr_list_settings->Add(btn);
        }
        
        return vbox({
            text("Settings") | bold | center,
            separator(),
            hbox({
                vbox({
                    text("Address Book:"),
                    addr_list_settings->Render() | vscroll_indicator | frame | size(HEIGHT, EQUAL, 10),
                    separator(),
                    hbox({ text("Name:    "), input_name->Render() | flex }),
                    hbox({ text("Address: "), input_addr->Render() | flex }),
                    hbox({ btn_addr_add->Render(), btn_addr_delete->Render() })
                }) | flex,
                separator(),
                vbox({
                    text("Counters:"),
                    text("Print: " + std::to_string(fb.counters.print_count)),
                    text("Send:  " + std::to_string(fb.counters.send_count)),
                    filler()
                }) | size(WIDTH, EQUAL, 30)
            }),
            filler(),
            btn_settings_to_home->Render()
        }) | border;
    });

    // --- Main Router ---
    auto main_container = Container::Tab({ home_renderer, copy_renderer, scan_renderer, settings_renderer }, (int*)&state.current_screen);
    
    auto root = CatchEvent(main_container, [&](Event e) {
        if (e == Event::Custom) return true;
        if (e == Event::Escape) { screen.Exit(); return true; }
        return main_container->OnEvent(e);
    });

    state.started = true;
    screen.Loop(root);

    return 0;
}
