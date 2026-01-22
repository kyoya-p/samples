#ifndef UTILS_HPP
#define UTILS_HPP

#include <string>
#include <stdexcept>

// Helper to access the log filename safely
std::string& GetLogFilename();

// Simple logger to file
void Log(const std::string& message);

// Helper to capture stack trace
std::string GetStackTrace();

// Setup crash handler (Windows)
void SetupCrashHandler();

// Macro to log and throw exception
#define THROW_LOG(msg) \
    do { \
        std::string full_msg = std::string("EXCEPTION THROWN at ") + __FILE__ + ":" + std::to_string(__LINE__) + "\n"; \
        full_msg += "Reason: " + std::string(msg) + "\n"; \
        full_msg += "[Stack Trace]\n" + GetStackTrace(); \
        Log(full_msg); \
        throw std::runtime_error(msg); \
    } while (0)

#endif // UTILS_HPP
