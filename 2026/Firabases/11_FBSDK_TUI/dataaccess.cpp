#include "dataaccess.hpp"
#include "utils.hpp"

#include <thread>
#include <chrono>
#include <ctime>
#include <algorithm>

#include "firebase/util.h"

FirestoreService::FirestoreService(std::function<void()> on_update) : on_update_(on_update) {}

FirestoreService::~FirestoreService() {
    Cleanup();
}

void FirestoreService::Cleanup() {
    StopAllListeners();
    if (app_) {
        Log("Cleaning up Firebase resources...");
        if (firestore_) {
            Log("Deleting Firestore instance...");
            delete firestore_;
            firestore_ = nullptr;
        }
        // Give some time for gRPC to shutdown safely as per Phase 7
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
        Log("Deleting Firebase App...");
        delete app_; 
        app_ = nullptr;
    }
}

void FirestoreService::StopAllListeners() {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!pages_.empty()) {
        Log("Removing all Firestore listeners (" + std::to_string(pages_.size()) + " pages)...");
        for (auto& page : pages_) {
            if (page->registration.is_valid()) {
                page->registration.Remove();
            }
        }
        pages_.clear();
    }
    contacts_.clear();
}

bool FirestoreService::Initialize(const std::string& api_key, int page_size) {
    Cleanup(); 
    current_api_key_ = api_key;
    page_size_ = page_size;
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

    Log("Firebase initialized with Paging Listener. Page Size: " + std::to_string(page_size_));
    StartListeningNextPage();
    return true;
}

void FirestoreService::LoadMore(int page_size) {
    if (!firestore_ || is_loading_ || !has_more_) return;
    page_size_ = page_size;
    StartListeningNextPage();
}

void FirestoreService::StartListeningNextPage() {
    if (!firestore_) return;
    
    size_t page_index;
    std::string cursor_id = "None";
    firebase::firestore::Query query = firestore_->Collection("addressbook").OrderBy("timestamp");

    {
        std::lock_guard<std::mutex> lock(mutex_);
        page_index = pages_.size();
        if (page_index > 0) {
            auto& last_page = pages_.back();
            if (!last_page->last_doc.is_valid()) {
                Log("Cannot load next page: Last document of previous page is invalid.");
                return;
            }
            if (last_page->has_pending_writes) {
                Log("Waiting for pending writes on Page " + std::to_string(page_index-1) + " (DocID: " + last_page->last_doc.id() + ") before loading more...");
                return;
            }
            query = query.StartAfter(last_page->last_doc);
            cursor_id = last_page->last_doc.id();
        }
    }

    int limit = page_size_;
    query = query.Limit(limit);
    
    Log("Starting listener for Page " + std::to_string(page_index) + " (Limit: " + std::to_string(limit) + ", Cursor: " + cursor_id + ")");

    is_loading_ = true;
    
    auto page = std::make_unique<Page>();
    Page* page_ptr = page.get();
    {
        std::lock_guard<std::mutex> lock(mutex_);
        pages_.push_back(std::move(page));
    }

    page_ptr->registration = query.AddSnapshotListener(
        [this, page_index, page_ptr](const firebase::firestore::QuerySnapshot& snapshot,
                                    firebase::firestore::Error error, const std::string& error_msg) {
        
        if (error != firebase::firestore::Error::kErrorOk) {
            Log("Firestore Page " + std::to_string(page_index) + " Error: " + error_msg);
            SetError("Firestore Error: " + error_msg);
            is_loading_ = false;
            on_update_();
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
            page_ptr->contacts = new_contacts;
            page_ptr->has_more = (new_contacts.size() >= (size_t)page_size_);
            page_ptr->has_pending_writes = snapshot.metadata().has_pending_writes();
            if (!new_contacts.empty()) {
                page_ptr->last_doc = snapshot.documents().back();
            }
            
            // Update global has_more based on the last page
            if (page_index == pages_.size() - 1) {
                has_more_ = page_ptr->has_more;
            }
            
            RebuildContacts();
            error_message_.clear();
        }
        is_loading_ = false;
        on_update_();
        });
}

void FirestoreService::RebuildContacts() {
    // Assumes mutex is already locked
    contacts_.clear();
    for (const auto& page : pages_) {
        contacts_.insert(contacts_.end(), page->contacts.begin(), page->contacts.end());
    }
}

void FirestoreService::AddContact(const std::string& name, const std::string& email) {
    if (!firestore_) return;
    std::unordered_map<std::string, firebase::firestore::FieldValue> data;
    data["name"] = firebase::firestore::FieldValue::String(name);
    data["email"] = firebase::firestore::FieldValue::String(email);
    data["timestamp"] = firebase::firestore::FieldValue::ServerTimestamp();
    firestore_->Collection("addressbook").Document(name).Set(data);
}

void FirestoreService::RemoveContact(const std::string& id) {
    if (!firestore_) return;
    firestore_->Collection("addressbook").Document(id).Delete();
}

std::vector<Contact> FirestoreService::GetContacts() { 
    std::lock_guard<std::mutex> lock(mutex_); 
    return contacts_; 
}

std::string FirestoreService::GetError() { 
    std::lock_guard<std::mutex> lock(mutex_); 
    return error_message_; 
}

bool FirestoreService::IsConnected() const { 
    return firestore_ != nullptr; 
}

bool FirestoreService::IsLoading() const { 
    return is_loading_; 
}

bool FirestoreService::HasMore() const { 
    return has_more_; 
}

void FirestoreService::SetError(const std::string& msg) { 
    Log(msg); 
    std::lock_guard<std::mutex> lock(mutex_); 
    error_message_ = msg; 
}
