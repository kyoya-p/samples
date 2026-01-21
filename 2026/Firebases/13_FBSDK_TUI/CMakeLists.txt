cmake_minimum_required(VERSION 3.14) # FetchContent MakeAvailable needs 3.14+
project(FirebaseTuiSample LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
cmake_policy(SET CMP0135 NEW)

include(FetchContent)

# --- Firebase C++ SDK Setup ---
# set(FIREBASE_SDK_VERSION "13.3.0")
message(STATUS "Downloading Firebase C++ SDK version ${FIREBASE_SDK_VERSION}. This may take some time...")
FetchContent_Declare(
  firebase_sdk
  URL "https://dl.google.com/firebase/sdk/cpp/firebase_cpp_sdk_13.3.0.zip"
)
# FetchContent_MakeAvailable(firebase_sdk)
set(FIREBASE_SDK_DIR "${CMAKE_CURRENT_SOURCE_DIR}/build/firebase_cpp_sdk")
set(FIREBASE_INCLUDE_DIR "${FIREBASE_SDK_DIR}/include")

if(MSVC)
    # Windows / MSVC Setup
    # Assumes x64 build. Adjust if x86 is needed.
    # Note: VS2019 libs are generally compatible with VS2022.
    set(FIREBASE_LIB_DIR "${FIREBASE_SDK_DIR}/libs/windows/VS2019/MD/x64/Release")
    
    # If Debug build is preferred, you might want to switch this, 
    # but mixing Debug/Release CRTs can be tricky with prebuilt libs.
    # Firebase C++ SDK usually provides MD (MultiThreadedDLL) libs by default in newer versions, or MT.
    # We will try to link against the libs found in that dir.
    
    set(FIREBASE_LIBS
        "${FIREBASE_LIB_DIR}/firebase_firestore.lib"
        "${FIREBASE_LIB_DIR}/firebase_auth.lib"
        "${FIREBASE_LIB_DIR}/firebase_app.lib"
    )

    # Windows System Libraries required by Firebase/Curl/SSL
    set(SYSTEM_LIBS
        ws2_32
        crypt32
        advapi32
        user32
        gdi32
        shell32
        shlwapi
        bcrypt
        rpcrt4
        ole32
        oleaut32
    )
    
    add_compile_definitions(_CRT_SECURE_NO_WARNINGS)
else()
    # Linux Setup
    set(FIREBASE_LIB_DIR "${FIREBASE_SDK_DIR}/libs/linux/x86_64/cxx11")
    set(FIREBASE_LIBS
        "${FIREBASE_LIB_DIR}/libfirebase_firestore.a"
        "${FIREBASE_LIB_DIR}/libfirebase_auth.a"
        "${FIREBASE_LIB_DIR}/libfirebase_app.a"
    )
    set(SYSTEM_LIBS dl)
    
    # libsecret-1 via PkgConfig (Linux only)
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(SECRET REQUIRED libsecret-1)
    include_directories(${SECRET_INCLUDE_DIRS})
    list(APPEND SYSTEM_LIBS ${SECRET_LIBRARIES})
    
    # Firestore on Linux needs these definitions often
    add_compile_definitions(_GLIBCXX_USE_CXX11_ABI=1)
endif()

message(STATUS "Firebase SDK Dir: ${FIREBASE_SDK_DIR}")

# Required System Libraries
find_package(Threads REQUIRED)

# CURL & OpenSSL (On Windows, these might need to be provided or found differently)
# For this sample, on Windows, we rely on CMake finding them if installed, 
# or if Firebase SDK bundles them (it often does NOT bundle headers, but links static libs).
# However, Firebase C++ SDK for Windows usually links statically against dependencies or uses system ones.
# Actually, the prebuilt Windows libs for Firebase usually include dependencies or don't require external CURL/OpenSSL in the simple case?
# Wait, the Linux setup explicitly links CURL/OpenSSL.
# On Windows, the Firebase SDK docs say: "The Firebase C++ SDK for desktop includes the following libraries...".
# It usually statically links what it needs on Windows or uses Windows APIs.
# Let's try NOT linking CURL/OpenSSL explicitly on Windows first, as the .lib files might be self-contained or use WinHTTP.
if(NOT MSVC)
    find_package(CURL REQUIRED)
    find_package(OpenSSL REQUIRED)
    find_package(ZLIB REQUIRED)
endif()

# FTXUI Configuration
FetchContent_Declare(ftxui
  GIT_REPOSITORY https://github.com/ArthurSonzogni/FTXUI
  GIT_TAG v5.0.0
)
FetchContent_MakeAvailable(ftxui)

add_executable(FirebaseApp src/main.cpp src/dataaccess.cpp src/utils.cpp)

# Link Libraries
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
)
