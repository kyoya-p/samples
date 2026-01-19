#include "service.h"
#include <iostream>
#include <string>
#include <vector>
#include <thread>
#include <chrono>

void PrintUsage() {
    std::cout << "Usage: FBTest [options]" << std::endl;
    std::cout << "Options:" << std::endl;
    std::cout << "  --api_key <KEY>  Set Firebase API Key" << std::endl;
    std::cout << "  --list           List all contacts" << std::endl;
    std::cout << "  --add <N> <E>    Add a contact (Name Email)" << std::endl;
    std::cout << "  --remove <ID>    Remove a contact by ID" << std::endl;
}

int main(int argc, char** argv) {
    std::string api_key;
    const char* env_key = std::getenv("API_KEY");
    if (!env_key) env_key = std::getenv("FB_API_KEY");
    if (env_key) api_key = env_key;

    std::string command;
    std::vector<std::string> args;

    for (int i = 1; i < argc; ++i) {
        std::string arg = argv[i];
        if (arg == "--api_key" && i + 1 < argc) {
            api_key = argv[++i];
        } else if (arg == "--list") {
            command = "list";
        } else if (arg == "--add" && i + 2 < argc) {
            command = "add";
            args.push_back(argv[++i]);
            args.push_back(argv[++i]);
        } else if (arg == "--remove" && i + 1 < argc) {
            command = "remove";
            args.push_back(argv[++i]);
        } else if (arg == "--help") {
            PrintUsage();
            return 0;
        }
    }

    if (api_key.empty()) {
        std::cerr << "Error: API Key not provided (use --api_key or API_KEY env var)" << std::endl;
        return 1;
    }

    if (command.empty()) {
        PrintUsage();
        return 1;
    }

    bool done = false;
    FirestoreService service([&done]() {
        // We might use this to signal data ready for 'list'
    });

    if (!service.Initialize(api_key, 100)) {
        std::cerr << "Failed to initialize: " << service.GetError() << std::endl;
        return 1;
    }

    // Wait for connection/initial load (Firebase is async)
    int retry = 0;
    while (!service.IsConnected() && retry < 50) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        retry++;
    }

    if (!service.IsConnected()) {
        std::cerr << "Timeout waiting for connection." << std::endl;
        return 1;
    }

    if (command == "list") {
        // Wait for contacts to be populated
        std::this_thread::sleep_for(std::chrono::seconds(2)); 
        auto contacts = service.GetContacts();
        std::cout << "Contacts (" << contacts.size() << "):" << std::endl;
        for (const auto& c : contacts) {
            std::cout << "ID: " << c.id << " | Name: " << c.name << " | Email: " << c.email << " | Time: " << c.timestamp << std::endl;
        }
    } else if (command == "add") {
        std::cout << "Adding: " << args[0] << " (" << args[1] << ")..." << std::endl;
        service.AddContact(args[0], args[1]);
        std::this_thread::sleep_for(std::chrono::seconds(1));
        std::cout << "Done." << std::endl;
    } else if (command == "remove") {
        std::cout << "Removing: " << args[0] << "..." << std::endl;
        service.RemoveContact(args[0]);
        std::this_thread::sleep_for(std::chrono::seconds(1));
        std::cout << "Done." << std::endl;
    }

    return 0;
}
