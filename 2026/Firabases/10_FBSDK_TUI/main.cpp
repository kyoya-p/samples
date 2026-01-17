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
#include <exception>
#include <execinfo.h>
#include <unistd.h>

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

// Helper to access the log filename safely
std::string& GetLogFilename() {
    static std::string filename = "app.log";
    return filename;
}

// Simple logger to file
void Log(const std::string& message) {
    std::ofstream log_file(GetLogFilename(), std::ios_base::app);
    if (log_file.is_open()) {
        auto now = std::chrono::system_clock::to_time_t(std::chrono::system_clock::now());
        std::tm tm = *std::localtime(&now);
        char buffer[32];
        std::strftime(buffer, sizeof(buffer), "%Y-%m-%d %H:%M:%S", &tm);
        log_file << "[" << buffer << "] " << message << std::endl;
        log_file.flush();
    }
}

// Helper to capture stack trace
std::string GetStackTrace() {
    void* array[20];
    size_t size;
    char** strings;
    std::string trace;

    size = backtrace(array, 20);
    strings = backtrace_symbols(array, size);

    if (strings) {
        for (size_t i = 0; i < size; i++) {
            trace += strings[i];
            trace += "\n";
        }
        free(strings);
    }
    return trace;
}

// Macro to log and throw exception
#define THROW_LOG(msg) \
    do { \
        std::string full_msg = std::string("EXCEPTION THROWN at ") + __FILE__ + ":" + std::to_string(__LINE__) + "\n"; \
        full_msg += "Reason: " + std::string(msg) + "\n"; \
        full_msg += "[Stack Trace]\n" + GetStackTrace(); \
        Log(full_msg); \
        throw std::runtime_error(msg); \
    } while (0)

struct Contact {
  std::string id;
  std::string name;
  std::string email;
};

// Helper to define consistent column widths
Element MakeTableRow(Element name, Element email, Element op) {
    return hbox({
        std::move(name)  | size(WIDTH, EQUAL, 30),
        separator(),
        std::move(email) | flex, 
        separator(),
        std::move(op)    | size(WIDTH, EQUAL, 14) | hcenter,
    });
}

// Custom Component to trigger callback when Render is called (i.e. visible)
class VisibilityObserver : public ComponentBase {
public:
    VisibilityObserver(Component child, std::function<void()> on_visible)
        : child_(child), on_visible_(on_visible) {
        Add(child_);
    }

    Element Render() override {
        on_visible_();
        return child_->Render();
    }

    bool OnEvent(Event event) override {
        return child_->OnEvent(event);
    }

    bool Focusable() const override {
        return child_->Focusable();
    }
private:
    Component child_;
    std::function<void()> on_visible_;
};

std::string GenerateRandomName() {
  static const std::vector<std::string> first_names = {
      "Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Heidi"};
  static const std::vector<std::string> last_names = {
      "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller"};
  static std::random_device rd;
  static std::mt19937 gen(rd());
  std::uniform_int_distribution<> dis_first(0, first_names.size() - 1);
  std::uniform_int_distribution<> dis_last(0, last_names.size() - 1);
  std::uniform_int_distribution<> dis_middle(100, 999);
  return first_names[dis_first(gen)] + " " + std::to_string(dis_middle(gen)) + " " + last_names[dis_last(gen)];
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
          Log("Cleaning up Firebase resources...");
          if (firestore_) {
              delete firestore_;
              firestore_ = nullptr;
          }
          delete app_; 
          app_ = nullptr;
      }
  }

  void StopListener() {
      if (registration_.is_valid()) {
          Log("Removing Firestore listener...");
          registration_.Remove();
          registration_ = firebase::firestore::ListenerRegistration();
      }
  }

  bool Initialize(const std::string& api_key, int initial_limit = 10) {
    Cleanup(); 
    current_api_key_ = api_key;
    
    // Default limit
    page_size_ = initial_limit;
    page_index_ = 0;

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

    // Disable persistence
    firebase::firestore::Settings settings;
    settings.set_persistence_enabled(false);
    firestore_->set_settings(settings);

    Log("Firebase initialized. Loading first page...");
    LoadFirstPage();
    return true;
  }

  // --- Pagination Logic ---

  void LoadFirstPage() {
      if (!firestore_ || is_loading_) return;
      is_loading_ = true;
      page_index_ = 0;
      Log("Loading First Page");
      
      StopListener();
      
      firebase::firestore::Query query = firestore_->Collection("addressbook")
                                            .OrderBy("name")
                                            .Limit(page_size_);
      ApplyListener(query);
  }

  void LoadNextPage() {
      if (!firestore_ || is_loading_ || !last_doc_.is_valid()) return;
      is_loading_ = true;
      page_index_++;
      Log("Loading Next Page: " + std::to_string(page_index_ + 1));

      StopListener();
      
      firebase::firestore::Query query = firestore_->Collection("addressbook")
                                            .OrderBy("name")
                                            .StartAfter(last_doc_)
                                            .Limit(page_size_);
      ApplyListener(query);
  }

  void LoadPrevPage() {
      if (!firestore_ || is_loading_ || !first_doc_.is_valid() || page_index_ <= 0) return;
      
      is_loading_ = true;
      page_index_--;
      Log("Loading Previous Page: " + std::to_string(page_index_ + 1));

      StopListener();

      firebase::firestore::Query query = firestore_->Collection("addressbook")
                                            .OrderBy("name")
                                            .EndBefore(first_doc_)
                                            .LimitToLast(page_size_);
      ApplyListener(query);
  }

  void ApplyListener(firebase::firestore::Query query) { // Pass by value
      registration_ = query.AddSnapshotListener(
          [this](const firebase::firestore::QuerySnapshot& snapshot,
                 firebase::firestore::Error error, const std::string& error_msg) {
            
            is_loading_ = false;

            if (error != firebase::firestore::Error::kErrorOk) {
              Log("Firestore Error: " + error_msg);
              SetError("Firestore Error: " + error_msg);
              on_update_();
              return;
            }

            std::vector<Contact> new_contacts;
            auto docs = snapshot.documents();
            
            if (!docs.empty()) {
                first_doc_ = docs.front();
                last_doc_ = docs.back();
            }

            for (const auto& doc : docs) {
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
              // If we got full page, assume there might be more
              has_next_ = (new_contacts.size() >= (size_t)page_size_);
              error_message_.clear();
            }
            Log("Loaded " + std::to_string(new_contacts.size()) + " items.");
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
    
    Log("Adding contact: " + name);
    firestore_->Collection("addressbook").Document(name).Set(data);
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
  
  bool IsConnected() const { return firestore_ != nullptr; }
  bool IsLoading() const { return is_loading_; }
  bool HasNext() const { return has_next_; }
  bool HasPrev() const { return page_index_ > 0; }
  int GetPageIndex() const { return page_index_; }

 private:
  void SetError(const std::string& msg) {
      Log(msg);
      std::lock_guard<std::mutex> lock(mutex_);
      error_message_ = msg;
  }

  firebase::App* app_ = nullptr;
  firebase::firestore::Firestore* firestore_ = nullptr;
  firebase::firestore::ListenerRegistration registration_; 
  
  // Paging state
  firebase::firestore::DocumentSnapshot first_doc_;
  firebase::firestore::DocumentSnapshot last_doc_;
  int page_size_ = 10;
  int page_index_ = 0;
  bool has_next_ = false;

  std::vector<Contact> contacts_;
  std::string error_message_;
  std::string current_api_key_;
  bool is_loading_ = false;
  std::mutex mutex_;
  std::function<void()> on_update_;
};

int main(int argc, char** argv) {
  try {
      // Generate log filename with timestamp: app.YYMMDD-HHMM.log
      auto now = std::chrono::system_clock::to_time_t(std::chrono::system_clock::now());
      std::tm tm = *std::localtime(&now);
      char buffer[64];
      std::strftime(buffer, sizeof(buffer), "app.%y%m%d-%H%M.log", &tm);
      GetLogFilename() = buffer; // Set the static filename

      std::ofstream(GetLogFilename(), std::ios::trunc);
      Log("Starting application... Log file: " + GetLogFilename());

      auto screen = ScreenInteractive::Fullscreen();
      auto on_update = [&screen]() { screen.Post(Event::Custom); };
      FirestoreService service(on_update);

  // Dynamic limit based on screen size (Updated to use fixed 10 or screen)
  auto calculate_limit = []() {
      auto size = Terminal::Size();
      // approximate overhead: header(2), add_row(3), footer(1), border(2) = 8
      int limit = size.dimy - 8; 
      // return (limit > 5) ? limit : 5; 
      return 10; // Forced to 10 as per previous request
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
        
        // Prev Page Indicator
        if (service.HasPrev()) {
            rows_container->Add(Renderer([]{ return text("▲ Previous Page") | center | color(Color::GrayDark); }));
        }

        for (size_t i = 0; i < contacts.size(); ++i) {
          const auto& contact = contacts[i];
          auto remove_btn = Button("[Remove]", [&service, contact] { service.RemoveContact(contact.id); }, ButtonOption::Ascii());
          auto row = Renderer(remove_btn, [contact, remove_btn] {
            auto element = MakeTableRow(
                text(contact.name),
                text(contact.email),
                remove_btn->Render()
            );
            if (remove_btn->Focused()) {
                return element | inverted;
            }
            return element;
          });
          rows_container->Add(row);
        }
        
        // Next Page Indicator
        if (service.HasNext()) {
            rows_container->Add(Renderer([]{ return text("▼ Next Page") | center | color(Color::GrayDark); }));
        } else if (contacts.empty() && service.IsConnected()) {
             auto empty_label = Renderer([&] {
                return hbox({ filler(), text("No Data") | color(Color::GrayDark), filler() });
            });
            rows_container->Add(empty_label);
        }
        
        if (service.IsLoading()) {
             auto loading_label = Renderer([&] {
                return hbox({ filler(), text("Loading...") | color(Color::Yellow) | bold, filler() });
            });
            rows_container->Add(loading_label);
        }
      };

      // Custom Event Handler for Paging
      auto list_handler = CatchEvent(rows_container, [&](Event event) {
          if (!service.IsConnected()) return false;
          
          bool handled = rows_container->OnEvent(event);
          
          // Detect paging triggers
          if (!handled) {
              if (event == Event::ArrowDown || (event.is_mouse() && event.mouse().button == Mouse::WheelDown) || event == Event::Character('j')) {
                  if (service.HasNext()) {
                      service.LoadNextPage();
                      return true;
                  }
              }
              if (event == Event::ArrowUp || (event.is_mouse() && event.mouse().button == Mouse::WheelUp) || event == Event::Character('k')) {
                  if (service.HasPrev()) {
                      service.LoadPrevPage();
                      return true;
                  }
              }
          }
          return handled;
      });

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
          return vbox({ separator(), MakeTableRow(name_input->Render(), email_input->Render(), add_btn->Render()), separator() });
      });

      auto exit_btn = Button("[Exit]", [&screen] { screen.ExitLoopClosure()(); }, ButtonOption::Ascii());
      auto activate_btn = Button("[Activate]", [&] { show_config = true; }, ButtonOption::Ascii());

      auto main_container = Container::Vertical({ list_handler, add_row, Container::Horizontal({ activate_btn, exit_btn }) });
      auto app_renderer = Renderer(main_container, [&] {
        auto contacts = service.GetContacts();
        auto error_msg = service.GetError();
        bool connected = service.IsConnected();
        
        Elements rows;
        if (connected) rows.push_back(text("Status: Connected (Page: " + std::to_string(service.GetPageIndex() + 1) + ")") | color(Color::Green) | bold);
        else rows.push_back(text("Status: Disconnected") | color(Color::Red) | bold);
        if (!error_msg.empty()) rows.push_back(text(error_msg) | color(Color::Red) | center);
        rows.push_back(separator());
        rows.push_back(vbox({ MakeTableRow(text("Name") | bold, text("Mail Address") | bold, text("Operation") | bold), separator() }));
        rows.push_back(list_handler->Render() | flex);
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
  } catch (const std::exception& e) {
      std::cerr << "FATAL ERROR: " << e.what() << std::endl;
      return 1;
  }
  return 0;
}
