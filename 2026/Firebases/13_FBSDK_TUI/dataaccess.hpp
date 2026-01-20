#ifndef DATAACCESS_HPP
#define DATAACCESS_HPP

#include <string>
#include <vector>
#include <functional>
#include <mutex>
#include <memory>

#include "firebase/app.h"
#include "firebase/firestore.h"

struct Contact {
  std::string id;
  std::string name;
  std::string email;
  std::string timestamp;
};

class FirestoreService {
 public:
  FirestoreService(std::function<void()> on_update);
  ~FirestoreService();

  void Cleanup();
  void StopAllListeners();
  bool Initialize(const std::string& api_key, int page_size);
  void LoadMore(int page_size);
  void StartListeningNextPage();
  void RebuildContacts();
  void AddContact(const std::string& name, const std::string& email);
  void RemoveContact(const std::string& id);

  std::vector<Contact> GetContacts();
  std::string GetError();
  bool IsConnected() const;
  bool IsLoading() const;
  bool HasMore() const;

 private:
  struct Page {
      firebase::firestore::ListenerRegistration registration;
      std::vector<Contact> contacts;
      firebase::firestore::DocumentSnapshot last_doc;
      bool has_more = true;
      bool has_pending_writes = false;
  };

  void SetError(const std::string& msg);
  
  firebase::App* app_ = nullptr;
  firebase::firestore::Firestore* firestore_ = nullptr;
  
  std::vector<std::unique_ptr<Page>> pages_;
  std::vector<Contact> contacts_;
  std::string error_message_;
  std::string current_api_key_;
  int page_size_ = 10;
  bool has_more_ = true;
  bool is_loading_ = false;
  std::mutex mutex_;
  std::function<void()> on_update_;
};

#endif // DATAACCESS_HPP
