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
#include <unordered_map>

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
#include "firebase/util.h"

using namespace ftxui;

// Simple logger to file
void Log(const std::string& message) {
    std::ofstream log_file("app.log", std::ios_base::app);
    if (log_file.is_open()) {
        auto now = std::chrono::system_clock::to_time_t(std::chrono::system_clock::now());
        std::string t = std::ctime(&now);
        if (!t.empty()) t.pop_back(); // Remove \n from ctime
        log_file << "[" << t << "] " << message << std::endl;
    }
}

struct Contact {
  std::string id;
  std::string name;
  std::string email;
};

// Helper to define consistent column widths across ALL rows
Element MakeTableRow(Element name, Element email, Element op) {
    return hbox({
        std::move(name)  | size(WIDTH, EQUAL, 30),
        separator(),
        std::move(email) | flex,
        separator(),
        std::move(op)    | size(WIDTH, EQUAL, 14) | hcenter,
    });
}

std::string GenerateRandomName() {
  static const std::vector<std::string> first_names = {
      "Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Heidi"};
  static const std::vector<std::string> last_names = {
      "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller"};
  static std::random_device rd;
  static std::mt19937 gen(rd());
  std::uniform_int_distribution<> dis_first(0, first_names.size() - 1);
  std::uniform_int_distribution<> dis_last(0, last_names.size() - 1);
  return first_names[dis_first(gen)] + " " + last_names[dis_last(gen)];
}

std::string GenerateRandomEmail(const std::string& name) {
  std::string email = name;
  std::transform(email.begin(), email.end(), email.begin(),
                 [](unsigned char c) { return std::tolower(c); });
  std::replace(email.begin(), email.end(), ' ', '.');
  return email + "@example.com";
}

class FirestoreService {
 public:
  FirestoreService(std::function<void()> on_update) : on_update_(on_update) {}
  
  ~FirestoreService() {
      Cleanup();
  }

  void Cleanup() {
      StopListener();
      if (app_) {
          Log("Closing application, deleting Firebase App...");
          delete app_; 
          app_ = nullptr;
          firestore_ = nullptr;
      }
  }

  void StopListener() {
      if (registration_.is_valid()) {
          Log("Removing Firestore listener...");
          registration_.Remove();
          registration_ = firebase::firestore::ListenerRegistration();
      }
  }

  bool Initialize(const std::string& api_key, int initial_limit) {
    Cleanup(); 
    current_api_key_ = api_key;
    current_limit_ = initial_limit;

    if (api_key.empty()) {
        SetError("Error: API Key is empty.");
        return false;
    }

    firebase::AppOptions options;
    options.set_api_key(api_key.c_str());
    options.set_app_id("1:646759465365:web:fc72f377308486d6e8769c");
    options.set_project_id("riot26-70125");
    options.set_messaging_sender_id("646759465365");
    options.set_storage_bucket("riot26-70125.firebasestorage.app");

    app_ = firebase::App::Create(options);
    
    if (!app_) {
        SetError("Failed to create Firebase App. Check Key.");
        return false;
    }

    firestore_ = firebase::firestore::Firestore::GetInstance(app_);
    if (!firestore_) {
         SetError("Failed to get Firestore instance.");
         return false;
    }

    Log("Firebase initialized successfully. Initial Limit: " + std::to_string(initial_limit));
    StartListening(initial_limit);
    return true;
  }

  void UpdateLimit(int new_limit) {
      if (!firestore_ || new_limit == current_limit_ || new_limit <= 0) return;
      Log("Updating Limit to: " + std::to_string(new_limit));
      current_limit_ = new_limit;
      StopListener();
      StartListening(new_limit);
  }

  void StartListening(int limit) {
      if (!firestore_) return;
      registration_ = firestore_->Collection("addressbook")
          .Limit(limit)
          .AddSnapshotListener(
              [this](const firebase::firestore::QuerySnapshot& snapshot,
                     firebase::firestore::Error error, const std::string& error_msg) {
                if (error != firebase::firestore::Error::kErrorOk) {
                  Log("Firestore Error: " + error_msg);
                  SetError("Firestore Error: " + error_msg);
                  on_update_();
                  return;
                }

                std::vector<Contact> new_contacts;
                for (const auto& doc : snapshot.documents()) {
                  Contact c;
                  c.id = doc.id();
                  auto fields = doc.GetData();
                  if (fields.find("name") != fields.end())
                    c.name = fields["name"].string_value();
                  if (fields.find("email") != fields.end())
                    c.email = fields["email"].string_value();
                  new_contacts.push_back(c);
                }

                {
                  std::lock_guard<std::mutex> lock(mutex_);
                  contacts_ = new_contacts;
                  error_message_.clear();
                }
                on_update_();
              });
  }

  void AddContact(const std::string& name, const std::string& email) {
    if (!firestore_) {
        SetError("Not connected. Please Activate first.");
        on_update_();
        return;
    }
    std::unordered_map<std::string, firebase::firestore::FieldValue> data;
    data["name"] = firebase::firestore::FieldValue::String(name);
    data["email"] = firebase::firestore::FieldValue::String(email);
    
    Log("Adding contact: " + name + " <" + email + ">");
    firestore_->Collection("addressbook").Document(name).Set(data).OnCompletion(
        [name](const firebase::Future<void>& result) {
            if (result.error() != firebase::firestore::Error::kErrorOk) {
                Log("Add Error: " + std::string(result.error_message()));
            }
        });
  }

  void RemoveContact(const std::string& id) {
    if (!firestore_) return;
    firestore_->Collection("addressbook").Document(id).Delete();
  }

  std::vector<Contact> GetContacts() {
    std::lock_guard<std::mutex> lock(mutex_);
    return contacts_;
  }

  std::string GetError() {
      std::lock_guard<std::mutex> lock(mutex_);
      return error_message_;
  }

  bool IsConnected() const {
      return firestore_ != nullptr;
  }

 private:
  void SetError(const std::string& msg) {
      Log(msg);
      std::lock_guard<std::mutex> lock(mutex_);
      error_message_ = msg;
  }

  firebase::App* app_ = nullptr;
  firebase::firestore::Firestore* firestore_ = nullptr;
  firebase::firestore::ListenerRegistration registration_; 
  std::vector<Contact> contacts_;
  std::string error_message_;
  std::string current_api_key_;
  int current_limit_ = 0;
  std::mutex mutex_;
  std::function<void()> on_update_;
};

int main(int argc, char** argv) {
  std::ofstream("app.log", std::ios::trunc);
  Log("Starting application...");

  auto screen = ScreenInteractive::Fullscreen();
  auto on_update = [&screen]() { screen.Post(Event::Custom); };
  FirestoreService service(on_update);

  // Helper to calculate required limit based on screen height
  auto calculate_limit = []() {
      auto size = Terminal::Size();
      // overhead: border(2), status(1), error(1?), header(2), add_row(3), footer(1) -> approx 10
      int limit = size.dimy - 10;
      return (limit > 0) ? limit : 1;
  };

  bool show_config = false;
  const char* env_key = std::getenv("API_KEY");
  std::string current_api_key = (env_key != nullptr) ? env_key : "";
  std::string api_key_input = current_api_key;
  
  if (!current_api_key.empty()) {
      service.Initialize(current_api_key, calculate_limit());
  }

  // --- Components ---
  auto key_input = Input(&api_key_input, "API Key");
  auto connect_btn = Button("[Connect]", [&] {
      if (service.Initialize(api_key_input, calculate_limit())) {
          current_api_key = api_key_input; 
          show_config = false;
      }
  }, ButtonOption::Ascii());
  
  auto cancel_btn = Button("[Cancel]", [&] { 
      api_key_input = current_api_key; 
      show_config = false; 
  }, ButtonOption::Ascii());
  
  auto config_container = Container::Vertical({ key_input, Container::Horizontal({ connect_btn, cancel_btn }) | center });
  auto config_renderer = Renderer(config_container, [&] {
      return vbox({
          text("Configuration") | bold | center,
          separator(),
          hbox(text("API Key: "), key_input->Render()) | border,
          separator(),
          hbox(connect_btn->Render(), cancel_btn->Render()) | center,
          text(""),
          text(service.GetError()) | color(Color::Red) | center
      }) | center | border | size(WIDTH, GREATER_THAN, 60);
  });

  auto rows_container = Container::Vertical({});
  auto refresh_ui = [&](const std::vector<Contact>& contacts) {
    rows_container->DetachAllChildren();
    for (const auto& contact : contacts) {
      auto remove_btn = Button("[Remove]", [&service, contact] { service.RemoveContact(contact.id); }, ButtonOption::Ascii());
      auto row = Renderer(remove_btn, [contact, remove_btn] {
        return MakeTableRow(
            text(contact.name),
            text(contact.email),
            remove_btn->Render()
        );
      });
      rows_container->Add(row);
    }
  };

  std::string next_name = GenerateRandomName();
  std::string next_email = GenerateRandomEmail(next_name);
  auto name_input = Input(&next_name, "Name");
  auto email_input = Input(&next_email, "Email");
  auto add_btn = Button("[Add]", [&] {
    if (!next_name.empty() && !next_email.empty()) {
        service.AddContact(next_name, next_email);
        next_name = GenerateRandomName();
        next_email = GenerateRandomEmail(next_name);
    }
  }, ButtonOption::Ascii());

  auto add_row = Renderer(Container::Horizontal({name_input, email_input, add_btn}), [&] {
      return vbox({
          separator(),
          MakeTableRow(
              name_input->Render(),
              email_input->Render(),
              add_btn->Render()
          ),
          separator()
      });
  });

  auto exit_btn = Button("[Exit]", [&screen] { screen.ExitLoopClosure()(); }, ButtonOption::Ascii());
  auto activate_btn = Button("[Activate]", [&] { show_config = true; }, ButtonOption::Ascii());

  auto main_container = Container::Vertical({ rows_container, add_row, Container::Horizontal({ activate_btn, exit_btn }) });
  auto app_renderer = Renderer(main_container, [&] {
    auto contacts = service.GetContacts();
    auto error_msg = service.GetError();
    bool connected = service.IsConnected();
    
    // Auto-update limit on render if size changed
    service.UpdateLimit(calculate_limit());

    Elements rows;
    if (connected) rows.push_back(text("Status: Connected") | color(Color::Green) | bold);
    else rows.push_back(text("Status: Disconnected (Press Activate to connect)") | color(Color::Red) | bold);
    if (!error_msg.empty()) rows.push_back(text(error_msg) | color(Color::Red) | center);
    rows.push_back(separator());
    
    rows.push_back(vbox({
        MakeTableRow(text("Name") | bold, text("Mail Address") | bold, text("Operation") | bold),
        separator()
    }));
    
    // リスト部分（スクロールバーなし、高さ固定）
    rows.push_back(rows_container->Render() | flex);
    rows.push_back(add_row->Render());
    
    rows.push_back(hbox({ filler(), activate_btn->Render(), text(" "), exit_btn->Render() }));
    return vbox(std::move(rows)) | border;
  });

  auto root_renderer = Renderer([&] {
      if (show_config) return dbox({ app_renderer->Render() | color(Color::GrayDark), config_renderer->Render() | center });
      return app_renderer->Render();
  });

  auto root_component = CatchEvent(root_renderer, [&](Event event) {
      if (event == Event::Custom) {
          refresh_ui(service.GetContacts());
          return true;
      }
      if (event == Event::Character('q')) {
          screen.ExitLoopClosure()();
          return true;
      }
      if (show_config) return config_container->OnEvent(event);
      return main_container->OnEvent(event);
  });

  refresh_ui(service.GetContacts());
  screen.Loop(root_component);
  Log("Application closed.");
  return 0;
}