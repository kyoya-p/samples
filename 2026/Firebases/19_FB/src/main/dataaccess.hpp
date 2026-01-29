#ifndef DATAACCESS_HPP
#define DATAACCESS_HPP

#include <string>
#include <vector>
#include <functional>
#include <mutex>
#include <memory>

#include "firebase/app.h"
#include "firebase/firestore.h"

class FirestoreService {
 public:
  FirestoreService(std::function<void()> on_update);
  ~FirestoreService();

  void Cleanup();
  void StopAllListeners();
  bool Initialize(const std::string& api_key, int page_size);
  void LoadMore(int page_size);
  
  void SetSortOrder(const std::string& field, bool descending);
  void SetFilter(const std::string& name_prefix, const std::string& email_prefix);
  
  void StartQuery(); 
  void FetchNextPage(); // Fetch only the next 10 items
  void AddContact(const std::string& name, const std::string& email);
  void RemoveContact(const std::string& id);

  // Buffer-less direct access to SDK snapshots
  size_t GetLoadedCount();
  std::string GetData(size_t index, const std::string& field);
  std::string GetId(size_t index);
  
  std::string GetError();
  bool IsConnected() const;
  bool IsLoading() const;
  bool HasMore() const;

 private:
  struct Page {
      std::unique_ptr<firebase::firestore::QuerySnapshot> snapshot;
      firebase::firestore::DocumentSnapshot last_doc;
      firebase::firestore::ListenerRegistration listener_registration;
  };

  void SetError(const std::string& msg);
  
  firebase::App* app_ = nullptr;
  firebase::firestore::Firestore* firestore_ = nullptr;
  
  // Storage of snapshots per page (Limit 10 each)
  std::vector<std::unique_ptr<Page>> pages_;
  
  std::string error_message_;
  std::string current_api_key_;
  
  // Query state
  std::string sort_field_ = "timestamp";
  bool sort_descending_ = true;
  std::string filter_name_ = "";
  std::string filter_email_ = "";

  bool has_more_ = true;
  bool is_loading_ = false;
  int initial_page_size_ = 10;
  std::mutex mutex_;
  std::function<void()> on_update_;
};

#endif // DATAACCESS_HPP
