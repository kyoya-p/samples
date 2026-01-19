cmake_minimum_required(VERSION 3.14) # FetchContent MakeAvailable needs 3.14+
project(FirebaseTuiSample LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

# --- Firebase C++ SDK Setup ---
set(FIREBASE_SDK_VERSION "13.3.0")
set(FIREBASE_SDK_URL "https://dl.google.com/firebase/sdk/cpp/firebase_cpp_sdk_${FIREBASE_SDK_VERSION}.zip")

FetchContent_Declare(
  firebase_sdk
  URL ${FIREBASE_SDK_URL}
  DOWNLOAD_EXTRACT_TIMESTAMP TRUE
)
FetchContent_MakeAvailable(firebase_sdk)

# Define paths based on FetchContent extraction
# Note: The zip extracts into the root of the source dir in recent versions or FetchContent behavior
set(FIREBASE_SDK_DIR "${firebase_sdk_SOURCE_DIR}")
set(FIREBASE_INCLUDE_DIR "${FIREBASE_SDK_DIR}/include")
set(FIREBASE_LIB_DIR "${FIREBASE_SDK_DIR}/libs/linux/x86_64/cxx11")

message(STATUS "Firebase SDK Dir: ${FIREBASE_SDK_DIR}")

# Required System Libraries
find_package(Threads REQUIRED)
find_package(CURL REQUIRED)
find_package(OpenSSL REQUIRED)
find_package(ZLIB REQUIRED)

# libsecret-1 via PkgConfig
find_package(PkgConfig REQUIRED)
pkg_check_modules(SECRET REQUIRED libsecret-1)

# FTXUI Configuration
FetchContent_Declare(ftxui
  GIT_REPOSITORY https://github.com/ArthurSonzogni/FTXUI
  GIT_TAG v5.0.0
)
FetchContent_MakeAvailable(ftxui)

add_executable(FirebaseApp main.cpp dataaccess.cpp utils.cpp)

# Link Firebase libraries manually
target_link_libraries(FirebaseApp
  PRIVATE
    ftxui::screen
    ftxui::dom
    ftxui::component
    "${FIREBASE_LIB_DIR}/libfirebase_firestore.a"
    "${FIREBASE_LIB_DIR}/libfirebase_auth.a"
    "${FIREBASE_LIB_DIR}/libfirebase_app.a"
    ${CURL_LIBRARIES}
    OpenSSL::SSL
    OpenSSL::Crypto
    ZLIB::ZLIB
    Threads::Threads
    ${SECRET_LIBRARIES}
    dl
)

target_include_directories(FirebaseApp
  PRIVATE
    ${FIREBASE_INCLUDE_DIR}
    ${SECRET_INCLUDE_DIRS}
)

# Firestore on Linux needs these definitions often
target_compile_definitions(FirebaseApp PRIVATE _GLIBCXX_USE_CXX11_ABI=1)