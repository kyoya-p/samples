#include "dataaccess.hpp"
#include "utils.hpp"

#include <thread>
#include <chrono>
#include <ctime>
#include <algorithm>
#include <unordered_map>

#include "firebase/util.h"

FirestoreService::FirestoreService(std::function<void()> on_update) : on_update_(on_update) {}

FirestoreService::~FirestoreService() {
    Cleanup();
}

void FirestoreService::Cleanup() {
    StopAllListeners();
    if (app_) {
        if (firestore_) {
            firestore_ = nullptr;
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        delete app_; 
        app_ = nullptr;
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
    }
}

void FirestoreService::StopAllListeners() {
    std::lock_guard<std::mutex> lock(mutex_);
    pages_.clear();
}

#ifdef _WIN32
#include <windows.h>
#endif

bool FirestoreService::Initialize(const std::string& api_key, int page_size) {
    Cleanup(); 
    current_api_key_ = api_key;
    has_more_ = true;

#ifdef _WIN32
    SetEnvironmentVariableA("GRPC_VERBOSITY", "NONE");
    SetEnvironmentVariableA("GRPC_TRACE", "");
#else
    setenv("GRPC_VERBOSITY", "NONE", 1);
#endif

    firebase::AppOptions options;
    options.set_api_key(api_key.c_str());
    options.set_app_id("1:646759465365:web:fc72f377308486d6e8769c");
    options.set_project_id("riot26-70125");
    options.set_messaging_sender_id("646759465365");
    options.set_storage_bucket("riot26-70125.firebasestorage.app");

    app_ = firebase::App::Create(options); 
    firebase::SetLogLevel(firebase::kLogLevelAssert);
    
    if (!app_) {
        SetError("Failed to create Firebase App.");
        return false;
    }

    firestore_ = firebase::firestore::Firestore::GetInstance(app_);
    if (!firestore_) {
        SetError("Failed to get Firestore instance.");
        return false;
    }

    firebase::firestore::Settings settings;
    settings.set_persistence_enabled(true);
    firestore_->set_settings(settings);

    StartQuery();
    return true;
}

void FirestoreService::SetSortOrder(const std::string& field, bool descending) {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (sort_field_ == field && sort_descending_ == descending) return;
        sort_field_ = field;
        sort_descending_ = descending;
    }
    StartQuery();
}

void FirestoreService::SetFilter(const std::string& name_prefix, const std::string& email_prefix) {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (filter_name_ == name_prefix && filter_email_ == email_prefix) return;
        filter_name_ = name_prefix;
        filter_email_ = email_prefix;
    }
    StartQuery();
}

void FirestoreService::LoadMore(int page_size) {
    if (!firestore_ || is_loading_ || !has_more_) return;
    FetchNextPage();
}

void FirestoreService::StartQuery() {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        pages_.clear();
        has_more_ = true;
    }
    FetchNextPage();
}

void FirestoreService::FetchNextPage() {
    if (!firestore_) return;

    size_t page_index;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (is_loading_) return;
        is_loading_ = true;
        page_index = pages_.size();
    }

    firebase::firestore::Query query = firestore_->Collection("addressbook");

    if (sort_field_ == "name") {
        query = query.OrderBy("name", sort_descending_ ? firebase::firestore::Query::Direction::kDescending : firebase::firestore::Query::Direction::kAscending);
        if (!filter_name_.empty()) {
            std::string end = filter_name_;
            end += "\xEF\xA3\xBF";
            query = query.WhereGreaterThanOrEqualTo("name", firebase::firestore::FieldValue::String(filter_name_))
                         .WhereLessThanOrEqualTo("name", firebase::firestore::FieldValue::String(end));
        }
    } else if (sort_field_ == "email") {
        query = query.OrderBy("email", sort_descending_ ? firebase::firestore::Query::Direction::kDescending : firebase::firestore::Query::Direction::kAscending);
        if (!filter_email_.empty()) {
            std::string end = filter_email_;
            end += "\xEF\xA3\xBF";
            query = query.WhereGreaterThanOrEqualTo("email", firebase::firestore::FieldValue::String(filter_email_))
                         .WhereLessThanOrEqualTo("email", firebase::firestore::FieldValue::String(end));
        }
    } else {
        query = query.OrderBy("timestamp", sort_descending_ ? firebase::firestore::Query::Direction::kDescending : firebase::firestore::Query::Direction::kAscending);
    }

    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (page_index > 0) {
            auto& last_page = pages_.back();
            if (last_page->last_doc.is_valid()) {
                query = query.StartAfter(last_page->last_doc);
            }
        }
    }

    // Always use Limit(10) explicitly before Get()
    firebase::firestore::Query limited_query = query.Limit(10);
    
    std::string q_log = "Firestore Query: Page=";
    q_log += std::to_string(page_index);
    q_log += " Limit=10 OrderBy=";
    q_log += sort_field_;
    Log(q_log);

    limited_query.Get().OnCompletion([this, page_index](const firebase::Future<firebase::firestore::QuerySnapshot>& completed_future) {
        if (completed_future.error() != firebase::firestore::Error::kErrorOk) {
            std::string err = "Firestore Error (Page=" + std::to_string(page_index) + "): ";
            err += completed_future.error_message();
            SetError(err);
            is_loading_ = false;
            on_update_();
            return;
        }

        const firebase::firestore::QuerySnapshot* snapshot = completed_future.result();
        if (snapshot) {
            std::lock_guard<std::mutex> lock(mutex_);
            auto page = std::make_unique<Page>();
            page->snapshot = std::make_unique<firebase::firestore::QuerySnapshot>(*snapshot);
            auto docs = snapshot->documents();
            
            size_t actual_count = docs.size();
            if (actual_count > 10) actual_count = 10; 

            std::string res_log = "Firestore Query Result: Page=" + std::to_string(page_index);
            res_log += " Count=" + std::to_string(actual_count);
            res_log += " (Raw=" + std::to_string(docs.size()) + ")";
            if (actual_count > 0) {
                res_log += " First=" + docs[0].id();
            }
            Log(res_log);

            if (actual_count > 0) {
                page->last_doc = docs[actual_count - 1];
            }
            has_more_ = (docs.size() >= 10);
            pages_.push_back(std::move(page));
            error_message_.clear();
        }
        is_loading_ = false;
        on_update_();
    });
}

size_t FirestoreService::GetLoadedCount() {
    std::lock_guard<std::mutex> lock(mutex_);
    size_t total = 0;
    for (const auto& page : pages_) {
        if (page->snapshot) {
            size_t count = page->snapshot->documents().size();
            total += (count > 10) ? 10 : count;
        }
    }
    return total;
}

std::string FirestoreService::GetData(size_t index, const std::string& field) {
    std::lock_guard<std::mutex> lock(mutex_);
    size_t current_base = 0;
    for (const auto& page : pages_) {
        if (!page->snapshot) continue;
        auto docs = page->snapshot->documents();
        size_t count = docs.size();
        if (count > 10) count = 10;

        if (index < current_base + count) {
            auto doc = docs[index - current_base];
            auto data = doc.GetData();
            
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
            if (data.count(field)) return data[field].string_value();
            return "";
        }
        current_base += count;
    }
    return "";
}

std::string FirestoreService::GetId(size_t index) {
    std::lock_guard<std::mutex> lock(mutex_);
    size_t current_base = 0;
    for (const auto& page : pages_) {
        if (!page->snapshot) continue;
        auto docs = page->snapshot->documents();
        size_t count = docs.size();
        if (count > 10) count = 10;
        
        if (index < current_base + count) {
            return docs[index - current_base].id();
        }
        current_base += count;
    }
    return "";
}

void FirestoreService::AddContact(const std::string& name, const std::string& email) {
    if (!firestore_) return;
    std::unordered_map<std::string, firebase::firestore::FieldValue> data;
    data["name"] = firebase::firestore::FieldValue::String(name);
    data["email"] = firebase::firestore::FieldValue::String(email);
    data["timestamp"] = firebase::firestore::FieldValue::ServerTimestamp();
    firestore_->Collection("addressbook").Document(name).Set(data).OnCompletion([this](const firebase::Future<void>&){
        StartQuery(); 
    });
}

void FirestoreService::RemoveContact(const std::string& id) {
    if (!firestore_) return;
    firestore_->Collection("addressbook").Document(id).Delete().OnCompletion([this](const firebase::Future<void>&){
        StartQuery(); 
    });
}

std::string FirestoreService::GetError() { std::lock_guard<std::mutex> lock(mutex_); return error_message_; }
bool FirestoreService::IsConnected() const { return firestore_ != nullptr; }
bool FirestoreService::IsLoading() const { return is_loading_; }
bool FirestoreService::HasMore() const { return has_more_; }
void FirestoreService::SetError(const std::string& msg) { Log(msg); std::lock_guard<std::mutex> lock(mutex_); error_message_ = msg; }
