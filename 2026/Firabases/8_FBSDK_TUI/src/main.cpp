#include <iostream>
#include <string>
#include <vector>
#include <memory>
#include <thread>
#include <chrono>
#include <mutex>
#include <algorithm>
#include <cstdlib>
#include <unordered_map>
#include <functional>

// Firebase
#include "firebase/app.h"
#include "firebase/auth.h"
#include "firebase/firestore.h"
#include "firebase/util.h"

// FTXUI
#include "ftxui/component/component.hpp"
#include "ftxui/component/screen_interactive.hpp"
#include "ftxui/dom/elements.hpp"
#include "ftxui/component/component_options.hpp" 

using namespace ftxui;

// --- Configuration ---
// FB_API_KEY is fetched from environment variable for security.
const char* kProjectId = "riot26-70125";
const char* kAppId = "1:646759465365:web:fc72f377308486d6e8769c";

// --- Data Model ---
struct User {
    std::string id;
    std::string name;
    std::string email;
};

// --- Helper ---
std::string GenerateRandomId(size_t length) {
    const char charset[] = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    const size_t max_index = sizeof(charset) - 1;
    std::string id;
    id.reserve(length);
    // Note: For simplicity using rand(), consider std::mt19937 for better randomness
    for (size_t i = 0; i < length; ++i) {
        id += charset[std::rand() % max_index];
    }
    return id;
}

std::string GetEnv(const char* name) {
#ifdef _WIN32
    char* buf = nullptr;
    size_t sz = 0;
    if (_dupenv_s(&buf, &sz, name) == 0 && buf != nullptr) {
        std::string val(buf);
        free(buf);
        return val;
    }
#else
    const char* val = std::getenv(name);
    if (val) return std::string(val);
#endif
    return "";
}

// --- Firebase Logic Class ---
class FirebaseManager {
public:
    FirebaseManager() : app_(nullptr), auth_(nullptr), db_(nullptr) {
        std::srand(static_cast<unsigned int>(std::time(nullptr)));
    }
    
    ~FirebaseManager() {
        if (app_) delete app_;
    }

    bool Initialize() {
        std::string api_key = GetEnv("FB_API_KEY");

        if (api_key.empty()) {
            last_error_ = "Error: Environment variable FB_API_KEY is not set.";
            return false;
        }
        if (std::string(kAppId) == "your-app-id") {
            last_error_ = "Error: Please set kAppId in src/main.cpp";
            return false;
        }

        firebase::AppOptions options;
        options.set_api_key(api_key.c_str());
        options.set_project_id(kProjectId);
        options.set_app_id(kAppId);

        app_ = firebase::App::Create(options);
        if (!app_) {
            last_error_ = "Failed to create Firebase App.";
            return false;
        }

        auth_ = firebase::auth::Auth::GetAuth(app_);
        db_ = firebase::firestore::Firestore::GetInstance(app_);
        
        return true;
    }

    bool SignIn() {
        if (!auth_) return false;
        auto future = auth_->SignInAnonymously();
        WaitForFuture(future);
        if (future.error() != 0) {
            last_error_ = future.error_message();
            return false;
        }
        return true;
    }

    void FetchUsers(std::function<void(std::vector<User>)> callback) {
        if (!db_) return;
        
        std::thread([this, callback]() {
            firebase::firestore::CollectionReference collection = db_->Collection("samples");
            auto future = collection.Get();
            WaitForFuture(future);

            std::vector<User> users;
            if (future.error() == 0) {
                const firebase::firestore::QuerySnapshot* snapshot = future.result();
                for (const auto& doc : snapshot->documents()) {
                    User user;
                    user.id = doc.id();
                    if (doc.Get("name").is_string()) {
                        user.name = doc.Get("name").string_value();
                    }
                    if (doc.Get("email").is_string()) {
                        user.email = doc.Get("email").string_value();
                    }
                    users.push_back(user);
                }
            }
            callback(users);
        }).detach();
    }

    void AddUser(const std::string& name, const std::string& email, std::function<void(bool)> callback) {
        if (!db_) return;

        std::thread([this, name, email, callback]() {
            firebase::firestore::CollectionReference collection = db_->Collection("samples");
            std::unordered_map<std::string, firebase::firestore::FieldValue> data;
            
            // Add ID field explicitly
            std::string random_id = GenerateRandomId(20);
            data["id"] = firebase::firestore::FieldValue::String(random_id);
            
            data["name"] = firebase::firestore::FieldValue::String(name);
            data["email"] = firebase::firestore::FieldValue::String(email);
            data["createdAt"] = firebase::firestore::FieldValue::ServerTimestamp();

            auto future = collection.Add(data);
            WaitForFuture(future);
            
            callback(future.error() == 0);
        }).detach();
    }

    void DeleteUser(const std::string& id, std::function<void(bool)> callback) {
        if (!db_) return;

        std::thread([this, id, callback]() {
             firebase::firestore::DocumentReference doc = db_->Collection("samples").Document(id);
             auto future = doc.Delete();
             WaitForFuture(future);
             
             callback(future.error() == 0);
        }).detach();
    }

    std::string GetLastError() const { return last_error_; }

private:
    firebase::App* app_;
    firebase::auth::Auth* auth_;
    firebase::firestore::Firestore* db_;
    std::string last_error_;

    template <typename T>
    void WaitForFuture(const firebase::Future<T>& future) {
        while (future.status() == firebase::kFutureStatusPending) {
            std::this_thread::sleep_for(std::chrono::milliseconds(50));
        }
    }
    
    void WaitForFuture(const firebase::Future<void>& future) {
        while (future.status() == firebase::kFutureStatusPending) {
            std::this_thread::sleep_for(std::chrono::milliseconds(50));
        }
    }
};

// --- UI Components ---

int main(int argc, char* argv[]) {
    // 1. Init Firebase
    static FirebaseManager fb;
    if (!fb.Initialize()) {
        std::cerr << "Init Error: " << fb.GetLastError() << std::endl;
        return 1;
    }
    if (!fb.SignIn()) {
        std::cerr << "Auth Error: " << fb.GetLastError() << std::endl;
        return 1;
    }

    auto screen = ScreenInteractive::Fullscreen();

    // State
    std::string input_name_val = "User_" + GenerateRandomId(4);
    std::string input_email_val = GenerateRandomId(8) + "@example.com";
    std::string status_msg = "Ready";
    bool is_loading = false;

    // Components
    InputOption option;
    auto name_input = Input(&input_name_val, "new user", option);
    auto email_input = Input(&input_email_val, "new-user@xxxx.co.jp", option);

    // List Container (Dynamic)
    auto list_container = Container::Vertical({});

    // Actions
    std::function<void()> reload_data;

    auto btn_add = Button("[Add]", [&] {
        if (input_name_val.empty() || input_email_val.empty()) {
            status_msg = "Error: Empty fields";
            return;
        }
        is_loading = true;
        status_msg = "Adding...";
        
        std::string n = input_name_val;
        std::string e = input_email_val;
        
        // Prepare next random values for demo
        input_name_val = "User_" + GenerateRandomId(4);
        input_email_val = GenerateRandomId(8) + "@example.com";

        fb.AddUser(n, e, [&](bool success) {
            if (success) {
                status_msg = "Added successfully";
                screen.Post([&]{ reload_data(); });
            } else {
                status_msg = "Add Failed";
                is_loading = false;
                screen.Post(Event::Custom);
            }
        });
    }, ButtonOption::Animated());

    reload_data = [&] {
        is_loading = true;
        status_msg = "Fetching...";
        screen.Post(Event::Custom);

        fb.FetchUsers([&](std::vector<User> users) {
            screen.Post([&, users] {
                list_container->DetachAllChildren();
                
                // Add Header
                list_container->Add(Renderer([&]{
                    return hbox({
                        text("Name") | center | size(WIDTH, EQUAL, 20) | color(Color::Black),
                        separator(),
                        text("Mail Address") | center | size(WIDTH, EQUAL, 30) | color(Color::Black),
                        separator(),
                        text("Operation") | center | flex | color(Color::Black)
                    }) | bgcolor(Color::RGB(225, 213, 231));
                }));

                // Add Rows
                for (const auto& user : users) {
                    auto delete_btn = Button("[Remove]", [&, user] {
                        is_loading = true;
                        status_msg = "Deleting " + user.name + "...";
                        
                        fb.DeleteUser(user.id, [&](bool success) {
                            if (success) {
                                status_msg = "Deleted.";
                                screen.Post([&]{ reload_data(); });
                            } else {
                                status_msg = "Delete Failed.";
                                is_loading = false;
                                screen.Post(Event::Custom);
                            }
                        });
                    }, ButtonOption::Ascii());

                    auto row_renderer = Renderer(delete_btn, [user, delete_btn] {
                        return hbox({
                            text(user.name) | center | size(WIDTH, EQUAL, 20),
                            separator(),
                            text(user.email) | center | size(WIDTH, EQUAL, 30),
                            separator(),
                            delete_btn->Render() | center | flex | color(Color::Red)
                        });
                    });
                    
                    list_container->Add(row_renderer);
                }
                
                is_loading = false;
                status_msg = "Updated: " + std::to_string(users.size()) + " users found.";
            });
        });
    };

    // Initial Load
    reload_data();

    auto btn_close = Button("[Close]", [&] {
        screen.ExitLoopClosure()();
    }, ButtonOption::Animated());

    auto add_area = Container::Horizontal({
        name_input,
        email_input,
        btn_add
    });

    auto add_area_renderer = Renderer(add_area, [&] {
        return hbox({
            name_input->Render() | size(WIDTH, EQUAL, 20),
            separator(),
            email_input->Render() | size(WIDTH, EQUAL, 30),
            separator(),
            btn_add->Render() | center | flex | color(Color::Green)
        });
    });

    auto main_container = Container::Vertical({
        list_container,
        add_area,
        btn_close
    });

    auto renderer = Renderer(main_container, [&] {
        return vbox({
            text("Address Dialog") | bold | hcenter | bgcolor(Color::Blue),
            separator(),
            list_container->Render() | flex,
            // separator(), // Separator is handled by borders of rows
            add_area_renderer->Render(),
            separator(),
            hbox({
                text(status_msg) | color(is_loading ? Color::Yellow : Color::GrayDark) | flex,
                btn_close->Render() | align_right | color(Color::Cyan)
            })
        }) | border;
    });

    screen.Loop(renderer);

    return 0;
}
