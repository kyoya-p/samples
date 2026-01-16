#include <iostream>
#include <string>
#include <thread>
#include <chrono>
#include <cstdlib>
#include <vector>
#include <map>

#include "firebase/app.h"
#include "firebase/auth.h"
#include "firebase/firestore.h"
#include "firebase/util.h"

// 環境変数を取得するクロスプラットフォームヘルパー
std::string GetEnv(const char* name, const char* default_value = "") {
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
    return std::string(default_value);
}

// Futureの完了を待つヘルパー
template <typename T>
const T* WaitForFuture(const firebase::Future<T>& future, const char* operation_name) {
    std::cout << "Waiting for " << operation_name << "..." << std::endl;
    while (future.status() == firebase::kFutureStatusPending) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    if (future.status() != firebase::kFutureStatusComplete) {
        std::cerr << "ERROR: " << operation_name << " failed or invalid. Status: " << future.status() << std::endl;
        return nullptr;
    }

    if (future.error() != 0) {
        std::cerr << "ERROR: " << operation_name << " failed. Code: " << future.error() << ", Message: " << future.error_message() << std::endl;
        return nullptr;
    }

    return future.result();
}

// Void Future用
bool WaitForFutureVoid(const firebase::Future<void>& future, const char* operation_name) {
    std::cout << "Waiting for " << operation_name << "..." << std::endl;
    while (future.status() == firebase::kFutureStatusPending) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    if (future.status() != firebase::kFutureStatusComplete) {
        std::cerr << "ERROR: " << operation_name << " failed or invalid. Status: " << future.status() << std::endl;
        return false;
    }

    if (future.error() != 0) {
        std::cerr << "ERROR: " << operation_name << " failed. Code: " << future.error() << ", Message: " << future.error_message() << std::endl;
        return false;
    }

    return true;
}

int main(int argc, char* argv[]) {
    // 1. 設定の読み込み
    std::string api_key = GetEnv("FB_API_KEY");
    std::string project_id = GetEnv("FB_PROJECT_ID", "riot26-70125");
    std::string app_id = GetEnv("FB_APP_ID");

    if (api_key.empty()) {
        std::cerr << "Error: FB_API_KEY environment variable is not set." << std::endl;
        return 1;
    }

    // 2. Firebase App 初期化
    std::cout << "Initializing Firebase..." << std::endl;
    firebase::AppOptions options;
    options.set_api_key(api_key.c_str());
    options.set_project_id(project_id.c_str());
    options.set_app_id(app_id.c_str());

    firebase::App* app = firebase::App::Create(options);
    if (!app) {
        std::cerr << "Failed to create Firebase App." << std::endl;
        return 1;
    }
    std::cout << "Firebase initialized successfully!" << std::endl;

    // 3. Auth (匿名認証)
    std::cout << "Initializing Auth..." << std::endl;
    firebase::auth::Auth* auth = firebase::auth::Auth::GetAuth(app);
    
    std::cout << "Signing in anonymously..." << std::endl;
    auto sign_in_future = auth->SignInAnonymously();
    const auto* user_result = WaitForFuture(sign_in_future, "SignInAnonymously");
    
    if (user_result) {
        std::cout << "Auth SUCCESS: Signed in as UID: " << user_result->user.uid() << std::endl;
    } else {
        return 1;
    }

    // 4. Firestore
    std::cout << "Initializing Firestore..." << std::endl;
    firebase::firestore::Firestore* db = firebase::firestore::Firestore::GetInstance(app);

    std::cout << "--- START VERIFICATION ---" << std::endl;
    
    // データ作成
    std::map<std::string, firebase::firestore::FieldValue> data;
    auto now_ms = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
    std::string name_val = "Verify Item C++ " + std::to_string(now_ms);
    data["name"] = firebase::firestore::FieldValue::String(name_val);
    data["createdAt"] = firebase::firestore::FieldValue::Integer(now_ms); // Date型は少し複雑なので簡易的にTimestamp(Int)で

    // 書き込み
    std::cout << "1. Writing data: " << name_val << std::endl;
    firebase::firestore::CollectionReference collection = db->Collection("samples");
    auto add_future = collection.Add(data);
    const auto* doc_ref_result = WaitForFuture(add_future, "Firestore Add");

    if (!doc_ref_result) return 1;
    std::cout << "   -> Written ID: " << doc_ref_result->id() << std::endl;

    // 読み込み
    std::cout << "2. Reading data..." << std::endl;
    auto get_future = collection.Get();
    const auto* query_snapshot = WaitForFuture(get_future, "Firestore Get");

    if (!query_snapshot) return 1;
    std::cout << "   -> Fetched " << query_snapshot->documents().size() << " documents." << std::endl;

    bool found = false;
    for (const auto& doc : query_snapshot->documents()) {
        if (doc.Get("name").string_value() == name_val) {
             found = true;
             std::cout << "3. Verification SUCCESS: Data match found! [ID: " << doc.id() << "]" << std::endl;
             break;
        }
    }

    if (!found) {
        std::cerr << "Verification FAILED: The written data was not found." << std::endl;
        // return 1; // 失敗してもクリーンアップへ
    }

    std::cout << "--- VERIFICATION COMPLETE ---" << std::endl;

    // クリーンアップ (Appの削除等はOSに任せても良いが、明示的に行うなら)
    delete auth;
    delete db;
    delete app;

    return 0;
}
