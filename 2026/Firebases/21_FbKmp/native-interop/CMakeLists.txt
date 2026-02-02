cmake_minimum_required(VERSION 3.14)
project(FirebaseShim)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(FIREBASE_SDK_DIR "${CMAKE_CURRENT_SOURCE_DIR}/firebase_cpp_sdk")

if(UNIX AND NOT APPLE)
    set(FIREBASE_LIB_DIR "${FIREBASE_SDK_DIR}/libs/linux/x86_64/cxx11")
    set(FIREBASE_LIBS
        "${FIREBASE_LIB_DIR}/libfirebase_firestore.a"
        "${FIREBASE_LIB_DIR}/libfirebase_auth.a"
        "${FIREBASE_LIB_DIR}/libfirebase_app.a"
    )
    # Linux system deps
    find_package(CURL REQUIRED)
    find_package(OpenSSL REQUIRED)
    find_package(Threads REQUIRED)
    set(SYSTEM_LIBS
        ${CURL_LIBRARIES}
        OpenSSL::SSL
        OpenSSL::Crypto
        Threads::Threads
        dl
    )
elseif(WIN32)
    set(FIREBASE_LIB_DIR "${FIREBASE_SDK_DIR}/libs/windows/VS2019/MD/x64/Release")
     set(FIREBASE_LIBS
        "${FIREBASE_LIB_DIR}/firebase_firestore.lib"
        "${FIREBASE_LIB_DIR}/firebase_auth.lib"
        "${FIREBASE_LIB_DIR}/firebase_app.lib"
    )
    set(SYSTEM_LIBS
        ws2_32 crypt32 advapi32 user32 gdi32 shell32 shlwapi bcrypt rpcrt4 ole32 oleaut32
    )
endif()

include_directories(include)
include_directories(${FIREBASE_SDK_DIR}/include)

add_library(firebase_shim STATIC src/firebase_shim.cpp)
target_link_libraries(firebase_shim PRIVATE ${FIREBASE_LIBS} ${SYSTEM_LIBS})