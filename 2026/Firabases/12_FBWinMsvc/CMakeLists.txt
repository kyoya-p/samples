cmake_minimum_required(VERSION 3.14)
project(FirebaseTuiSample LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

# --- Firebase C++ SDK Setup (Download Binaries) ---
set(FIREBASE_SDK_VERSION "13.3.0")
message(STATUS "Configuring Firebase C++ SDK v${FIREBASE_SDK_VERSION}...")

FetchContent_Declare(firebase_cpp_sdk
  URL https://dl.google.com/firebase/sdk/cpp/firebase_cpp_sdk_${FIREBASE_SDK_VERSION}.zip
  DOWNLOAD_DIR ${CMAKE_SOURCE_DIR}/.cache/downloads
)
FetchContent_MakeAvailable(firebase_cpp_sdk)

# --- Application Setup ---
add_executable(FBTest main.cpp)

# Link Libraries
set(LIBS
    firebase_app
    firebase_auth
    firebase_firestore
    ws2_32
    crypt32
    bcrypt
    dbghelp
    iphlpapi
)

target_link_libraries(FBTest
  PRIVATE
    ${LIBS}
)

# MSVC specific options
if(MSVC)
  target_compile_options(FBTest PRIVATE /W3 /EHsc /D_CRT_SECURE_NO_WARNINGS)
endif()