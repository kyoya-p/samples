#ifndef DATAACCESS_HPP
#define DATAACCESS_HPP

#include <string>
#include <vector>
#include <functional>
#include <memory>
#include <mutex>

#include "firebase/app.h"
#include "firebase/firestore.h"

class FirestoreService {
public:
    struct Page {
        std::unique_ptr<firebase::firestore::QuerySnapshot> snapshot;
        firebase::firestore::DocumentSnapshot last_doc;
        firebase::firestore::ListenerRegistration listener_registration;
    };

    FirestoreService(std::function<void()> on_update);
    ~FirestoreService();

    bool Initialize(const std::string& api_key, const std::string& collection_name, int page_size);
    void Cleanup();

    void AddContact(const std::string& name, const std::string& email);
    void RemoveContact(const std::string& id);
    void AddJobLog(const std::vector<std::string>& emails);

    size_t GetLoadedCount();
    std::string GetData(size_t index, const std::string& field);
    std::string GetId(size_t index);

    bool IsConnected() const;
    bool IsLoading() const;
    bool HasMore() const;
    std::string GetError();

    void LoadMore(int page_size);
    void SetSortOrder(const std::string& field, bool descending);
    void SetFilter(const std::string& name_prefix, const std::string& email_prefix);

private:
    void StartQuery();
    void FetchNextPage();
    void StopAllListeners();
    void SetError(const std::string& msg);

    firebase::App* app_ = nullptr;
    firebase::firestore::Firestore* firestore_ = nullptr;
    
    std::string collection_name_;
    int initial_page_size_ = 10;
    std::string sort_field_ = "timestamp";
    bool sort_descending_ = true;
    std::string filter_name_ = "";
    std::string filter_email_ = "";

    std::vector<std::unique_ptr<Page>> pages_;
    bool is_loading_ = false;
    bool has_more_ = true;
    std::string current_api_key_;
    std::string error_message_;

    std::function<void()> on_update_;
    mutable std::mutex mutex_;
};

#endif // DATAACCESS_HPP