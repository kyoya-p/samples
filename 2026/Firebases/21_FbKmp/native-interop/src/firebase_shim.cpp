#include "firebase_shim.h"
#include "firebase/app.h"
#include "firebase/auth.h"
#include "firebase/firestore.h"
#include "firebase/util.h"

#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <thread>
#include <chrono>
#include <cstring>
#include <sstream>

// Helper to wait for future
template<typename T>
const T* WaitForFuture(const firebase::Future<T>& future) {
    while (future.status() == firebase::kFutureStatusPending) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    if (future.status() != firebase::kFutureStatusComplete) {
        return nullptr;
    }
    return future.result();
}

// Specialization for void
bool WaitForFutureVoid(const firebase::Future<void>& future) {
    while (future.status() == firebase::kFutureStatusPending) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    return future.status() == firebase::kFutureStatusComplete;
}

FBAppHandle fb_initialize_app(const char* project_id, const char* api_key, const char* app_id) {
    firebase::AppOptions options;
    options.set_project_id(project_id);
    options.set_api_key(api_key);
    options.set_app_id(app_id);
    
    // Note: On Desktop, Create usually requires JNIEnv (Android) or nil (Desktop).
    return firebase::App::Create(options);
}

FBAuthHandle fb_initialize_auth(FBAppHandle app) {
    return firebase::auth::Auth::GetAuth(static_cast<firebase::App*>(app));
}

FBFirestoreHandle fb_initialize_firestore(FBAppHandle app) {
    return firebase::firestore::Firestore::GetInstance(static_cast<firebase::App*>(app));
}

char* fb_sign_in(FBAuthHandle auth, const char* email, const char* password) {
    auto* f_auth = static_cast<firebase::auth::Auth*>(auth);
    auto future = f_auth->SignInWithEmailAndPassword(email, password);
    const auto* result = WaitForFuture(future);
    
    if (result && result->user.is_valid()) {
        std::string uid = result->user.uid();
        return strdup(uid.c_str());
    }
    return nullptr;
}

int fb_add_document(FBFirestoreHandle firestore, const char* collection, const char* doc_name, int value) {
    auto* db = static_cast<firebase::firestore::Firestore*>(firestore);
    
    std::unordered_map<std::string, firebase::firestore::FieldValue> data;
    data["name"] = firebase::firestore::FieldValue::String(doc_name);
    data["value"] = firebase::firestore::FieldValue::Integer(value);
    
    auto future = db->Collection(collection).Add(data);
    const auto* result = WaitForFuture(future);
    
    return result ? 0 : -1;
}

char* fb_query_sorted(FBFirestoreHandle firestore, const char* collection, const char* field) {
    auto* db = static_cast<firebase::firestore::Firestore*>(firestore);
    
    auto future = db->Collection(collection).OrderBy(field).Get();
    const auto* result = WaitForFuture(future); // QuerySnapshot
    
    if (!result) return nullptr;
    
    std::stringstream ss;
    ss << "[";
    bool first = true;
    for (const auto& doc : result->documents()) {
        if (!first) ss << ",";
        first = false;
        
        ss << "{";
        auto name = doc.Get("name").string_value();
        auto val = doc.Get("value").integer_value();
        ss << "\"name\": \"" << name << "\",";
        ss << "\"value\": " << val;
        ss << "}";
    }
    ss << "]";
    
    return strdup(ss.str().c_str());
}

void fb_free_string(char* str) {
    if (str) free(str);
}

