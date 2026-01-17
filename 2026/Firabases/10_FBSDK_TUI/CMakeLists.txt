cmake_minimum_required(VERSION 3.11)
project(FirebaseTuiSample LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Firebase C++ SDK paths
set(FIREBASE_SDK_DIR "${CMAKE_SOURCE_DIR}/build/sdk/firebase_cpp_sdk")
set(FIREBASE_INCLUDE_DIR "${FIREBASE_SDK_DIR}/include")
set(FIREBASE_LIB_DIR "${FIREBASE_SDK_DIR}/libs/linux/x86_64/cxx11")

# Required System Libraries
find_package(Threads REQUIRED)
find_package(CURL REQUIRED)
find_package(OpenSSL REQUIRED)
find_package(ZLIB REQUIRED)

# libsecret-1 via PkgConfig
find_package(PkgConfig REQUIRED)
pkg_check_modules(SECRET REQUIRED libsecret-1)

# FTXUI Configuration
include(FetchContent)
FetchContent_Declare(ftxui
  GIT_REPOSITORY https://github.com/ArthurSonzogni/FTXUI
  GIT_TAG v5.0.0
)
FetchContent_MakeAvailable(ftxui)

add_executable(FirebaseApp main.cpp)

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