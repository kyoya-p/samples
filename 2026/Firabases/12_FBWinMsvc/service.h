#ifndef SERVICE_H
#define SERVICE_H

#define _CRT_SECURE_NO_WARNINGS
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
#include <ctime>

#include "firebase/app.h"
#include "firebase/firestore.h"
#include "firebase/util.h"

// Helper to access the log filename safely
inline std::string& GetLogFilename() {
    static std::string filename = "app.log";
    return filename;
}

// Simple logger to file
inline void Log(const std::string& message) {
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

struct Contact {
  std::string id;
  std::string name;
  std::string email;
  std::string timestamp;
};

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

  bool Initialize(const std::string& api_key, int initial_limit) {
    Cleanup(); 
    current_api_key_ = api_key;
    current_limit_ = initial_limit;
    has_more_ = true;

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

    firebase::firestore::Settings settings;
    settings.set_persistence_enabled(false);
    firestore_->set_settings(settings);

    Log("Firebase initialized. Initial Limit: " + std::to_string(current_limit_));
    StartListening(current_limit_);
    return true;
  }

  void UpdateLimit(int new_limit) {
      if (!firestore_ || new_limit <= 0) return;
      if (is_loading_) return; 

      Log("Updating Limit to: " + std::to_string(new_limit));
      is_loading_ = true; 
      current_limit_ = new_limit;
      StopListener();
      StartListening(new_limit);
  }

  void LoadMore() {
      if (!firestore_ || is_loading_ || !has_more_) return;
      int step = 10;
      UpdateLimit(current_limit_ + step);
  }

  void StartListening(int limit) {
      if (!firestore_) return;
      is_loading_ = true;
      
      firebase::firestore::Query query = firestore_->Collection("addressbook")
                                            .OrderBy("timestamp")
                                            .Limit(limit);

      registration_ = query.AddSnapshotListener(
          [this, limit](const firebase::firestore::QuerySnapshot& snapshot,
                 firebase::firestore::Error error, const std::string& error_msg) {
            
            is_loading_ = false;

            if (error != firebase::firestore::Error::kErrorOk) {
              Log("Firestore Error: " + error_msg);
              SetError("Firestore Error: " + error_msg);
              if (on_update_) on_update_();
              return;
            }

            std::vector<Contact> new_contacts;
            auto docs = snapshot.documents();
            
            for (const auto& doc : docs) {
              Contact c;
              c.id = doc.id();
              auto fields = doc.GetData();
              if (fields.count("name")) c.name = fields["name"].string_value();
              if (fields.count("email")) c.email = fields["email"].string_value();
              if (fields.count("timestamp") && fields["timestamp"].is_timestamp()) {
                  auto ts = fields["timestamp"].timestamp_value();
                  std::time_t t = ts.seconds();
                  std::tm* tm_ptr = std::localtime(&t);
                  char buf[32];
                  std::strftime(buf, sizeof(buf), "%m/%d %H:%M", tm_ptr);
                  c.timestamp = buf;
              } else {
                  c.timestamp = "N/A";
              }
              new_contacts.push_back(c);
            }

            {
              std::lock_guard<std::mutex> lock(mutex_);
              contacts_ = new_contacts;
              // If we got fewer than requested, no more data
              has_more_ = (new_contacts.size() >= (size_t)limit);
              error_message_.clear();
            }
            if (on_update_) on_update_();
          });
  }

  void AddContact(const std::string& name, const std::string& email) {
    if (!firestore_) return;
    std::unordered_map<std::string, firebase::firestore::FieldValue> data;
    data["name"] = firebase::firestore::FieldValue::String(name);
    data["email"] = firebase::firestore::FieldValue::String(email);
    data["timestamp"] = firebase::firestore::FieldValue::ServerTimestamp();
    firestore_->Collection("addressbook").Document(name).Set(data);
  }

  void RemoveContact(const std::string& id) {
    if (!firestore_) return;
    firestore_->Collection("addressbook").Document(id).Delete();
  }

  std::vector<Contact> GetContacts() { std::lock_guard<std::mutex> lock(mutex_); return contacts_; }
  std::string GetError() { std::lock_guard<std::mutex> lock(mutex_); return error_message_; }
  bool IsConnected() const { return firestore_ != nullptr; }
  bool IsLoading() const { return is_loading_; }
  bool HasMore() const { return has_more_; }
  int GetCurrentLimit() const { return current_limit_; }

 private:
  void SetError(const std::string& msg) { Log(msg); std::lock_guard<std::mutex> lock(mutex_); error_message_ = msg; }
  firebase::App* app_ = nullptr;
  firebase::firestore::Firestore* firestore_ = nullptr;
  firebase::firestore::ListenerRegistration registration_; 
  
  int current_limit_ = 10;
  bool has_more_ = true;
  std::vector<Contact> contacts_;
  std::string error_message_;
  std::string current_api_key_;
  bool is_loading_ = false;
  std::mutex mutex_;
  std::function<void()> on_update_;
};

#endif // SERVICE_H
