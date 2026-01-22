#include "dataaccess.hpp"
#include "utils.hpp"
#include <iostream>
#include <thread>
#include <chrono>

int main() {
    std::cout << "Starting FirestoreService Test..." << std::endl;
    
    SetupCrashHandler();
    
    bool updated = false;
    auto service = std::make_unique<FirestoreService>([&] {
        updated = true;
        std::cout << "Callback received!" << std::endl;
    });

    std::cout << "Initializing service (without API Key)..." << std::endl;
    bool result = service->Initialize("", 10);
    
    if (!result) {
        std::cout << "Expected failure with empty API Key. Error: " << service->GetError() << std::endl;
    }

    std::cout << "Testing AddOrUpdateContact call (should fail/log but not crash)..." << std::endl;
    service->AddOrUpdateContact("TestUser", "test@example.com");

    std::cout << "Cleanup..." << std::endl;
    service->Cleanup();

    std::cout << "Test completed successfully." << std::endl;
    return 0;
}
