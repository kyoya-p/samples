#include "utils.hpp"

#include <chrono>
#include <ctime>
#include <fstream>
#include <iostream>
#include <unistd.h>
#include <execinfo.h>

std::string& GetLogFilename() {
    static std::string filename = "app.log";
    return filename;
}

void Log(const std::string& message) {
    std::ofstream log_file(GetLogFilename(), std::ios_base::app);
    if (log_file.is_open()) {
        auto now = std::chrono::system_clock::to_time_t(std::chrono::system_clock::now());
        std::tm tm = *std::localtime(&now);
        char buffer[32];
        std::strftime(buffer, sizeof(buffer), "%Y-%m-%d %H:%M:%S", &tm);
        log_file << "[" << buffer << "] [" << getpid() << "] " << message << std::endl;
        log_file.flush();
    }
}

std::string GetStackTrace() {
    void* array[20];
    size_t size;
    char** strings;
    std::string trace;

    size = backtrace(array, 20);
    strings = backtrace_symbols(array, size);

    if (strings) {
        for (size_t i = 0; i < size; i++) {
            trace += strings[i];
            trace += "\n";
        }
        free(strings);
    }
    return trace;
}

