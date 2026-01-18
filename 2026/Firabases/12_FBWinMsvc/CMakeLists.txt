cmake_minimum_required(VERSION 3.14)
project(FirebaseTuiSample LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

# --- FTXUI Setup ---
FetchContent_Declare(ftxui
  GIT_REPOSITORY https://github.com/ArthurSonzogni/FTXUI
  GIT_TAG v5.0.0
)
FetchContent_MakeAvailable(ftxui)

# --- Firebase C++ SDK Setup (Download Binaries) ---
set(FIREBASE_SDK_VERSION "13.3.0")
message(STATUS "Configuring Firebase C++ SDK v${FIREBASE_SDK_VERSION}...")

FetchContent_Declare(firebase_cpp_sdk
  URL https://dl.google.com/firebase/sdk/cpp/firebase_cpp_sdk_${FIREBASE_SDK_VERSION}.zip
)
FetchContent_MakeAvailable(firebase_cpp_sdk)

# --- Application Setup ---
add_executable(FirebaseApp main.cpp)

# Link Libraries
target_link_libraries(FirebaseApp
  PRIVATE
    ftxui::screen
    ftxui::dom
    ftxui::component
    firebase_app
    firebase_auth
    firebase_firestore
)

# Windows specific system libraries for Firebase
if(WIN32)
  target_link_libraries(FirebaseApp
    PRIVATE
      ws2_32
      crypt32
      bcrypt
      dbghelp
      iphlpapi
  )
endif()

# MSVC specific options
if(MSVC)
  target_compile_options(FirebaseApp PRIVATE /W3 /EHsc /D_CRT_SECURE_NO_WARNINGS)
endif()