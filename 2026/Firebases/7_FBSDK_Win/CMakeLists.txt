cmake_minimum_required(VERSION 3.14)
project(FirebaseCppSample CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

# --- Firebase C++ SDK Setup ---
if(NOT DEFINED FIREBASE_CPP_SDK_DIR)
  message(FATAL_ERROR "Please define FIREBASE_CPP_SDK_DIR to the path of the Firebase C++ SDK.")
endif()

set(FIREBASE_SDK_DIR "${FIREBASE_CPP_SDK_DIR}")
set(FIREBASE_INCLUDE_DIR "${FIREBASE_SDK_DIR}/include")

if(MSVC)
    set(FIREBASE_LIB_DIR "${FIREBASE_SDK_DIR}/libs/windows/VS2019/MD/x64/Release")
    set(FIREBASE_LIBS
        "${FIREBASE_LIB_DIR}/firebase_firestore.lib"
        "${FIREBASE_LIB_DIR}/firebase_auth.lib"
        "${FIREBASE_LIB_DIR}/firebase_app.lib"
    )
    set(SYSTEM_LIBS
        ws2_32 crypt32 advapi32 user32 gdi32 shell32 shlwapi bcrypt rpcrt4 ole32 oleaut32
    )
    add_compile_definitions(_CRT_SECURE_NO_WARNINGS)
else()
    set(FIREBASE_LIB_DIR "${FIREBASE_SDK_DIR}/libs/linux/x86_64/cxx11")
    set(FIREBASE_LIBS
        "${FIREBASE_LIB_DIR}/libfirebase_firestore.a"
        "${FIREBASE_LIB_DIR}/libfirebase_auth.a"
        "${FIREBASE_LIB_DIR}/libfirebase_app.a"
    )
    set(SYSTEM_LIBS dl)
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(SECRET REQUIRED libsecret-1)
    include_directories(${SECRET_INCLUDE_DIRS})
    list(APPEND SYSTEM_LIBS ${SECRET_LIBRARIES})
    add_compile_definitions(_GLIBCXX_USE_CXX11_ABI=1)
    find_package(CURL REQUIRED)
    find_package(OpenSSL REQUIRED)
    find_package(ZLIB REQUIRED)
endif()

find_package(Threads REQUIRED)

# FTXUI Configuration
FetchContent_Declare(ftxui
  GIT_REPOSITORY https://github.com/ArthurSonzogni/FTXUI
  GIT_TAG v5.0.0
)
FetchContent_MakeAvailable(ftxui)

add_executable(FirebaseApp src/main.cpp src/utils.cpp)

if(MSVC)
    target_link_libraries(FirebaseApp
        PRIVATE
        ftxui::screen
        ftxui::dom
        ftxui::component
        ${FIREBASE_LIBS}
        ${SYSTEM_LIBS}
    )
else()
    target_link_libraries(FirebaseApp
        PRIVATE
        ftxui::screen
        ftxui::dom
        ftxui::component
        ${FIREBASE_LIBS}
        ${CURL_LIBRARIES}
        OpenSSL::SSL
        OpenSSL::Crypto
        ZLIB::ZLIB
        Threads::Threads
        ${SYSTEM_LIBS}
    )
endif()

target_include_directories(FirebaseApp
    PRIVATE
    ${FIREBASE_INCLUDE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/src
)
