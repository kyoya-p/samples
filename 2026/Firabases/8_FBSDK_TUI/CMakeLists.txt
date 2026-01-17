cmake_minimum_required(VERSION 3.11)
project(FirebaseCppSample CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# パスをプロジェクト構成に合わせて固定
set(FIREBASE_CPP_SDK_DIR "${CMAKE_SOURCE_DIR}/build/sdk/firebase_cpp_sdk")

# --- FTXUI Setup ---
include(FetchContent)
FetchContent_Declare(ftxui
  GIT_REPOSITORY https://github.com/ArthurSonzogni/FTXUI
  GIT_TAG v5.0.0
)
FetchContent_GetProperties(ftxui)
if(NOT ftxui_POPULATED)
  FetchContent_Populate(ftxui)
  add_subdirectory(${ftxui_SOURCE_DIR} ${ftxui_BINARY_DIR} EXCLUDE_FROM_ALL)
endif()

add_executable(FirebaseApp src/main.cpp)

# --- Firebase SDK Setup (Manual) ---
set(FIREBASE_LIBS_DIR "${FIREBASE_CPP_SDK_DIR}/libs/linux/x86_64/cxx11")

target_include_directories(FirebaseApp PRIVATE 
  ${ftxui_SOURCE_DIR}/include
  ${FIREBASE_CPP_SDK_DIR}/include
)

# FTXUI targets: screen, dom, component
# Firebase libs: absolute paths
target_link_libraries(FirebaseApp
  PRIVATE
  screen
  dom
  component
  "${FIREBASE_LIBS_DIR}/libfirebase_auth.a"
  "${FIREBASE_LIBS_DIR}/libfirebase_firestore.a"
  "${FIREBASE_LIBS_DIR}/libfirebase_app.a"
)

if(UNIX AND NOT APPLE)
  find_package(PkgConfig REQUIRED)
  pkg_check_modules(SECRET libsecret-1 REQUIRED)
  
  find_package(CURL REQUIRED)
  find_package(ZLIB REQUIRED)
  find_package(OpenSSL REQUIRED)
  
  target_link_libraries(FirebaseApp
    PRIVATE
    pthread 
    atomic 
    dl 
    ${CURL_LIBRARIES}
    ${ZLIB_LIBRARIES}
    ${OPENSSL_LIBRARIES}
    ${SECRET_LIBRARIES}
  )
  
  target_include_directories(FirebaseApp PRIVATE ${SECRET_INCLUDE_DIRS})
  target_link_directories(FirebaseApp PRIVATE ${SECRET_LIBRARY_DIRS})

  find_library(UUID_LIB uuid)
  if(UUID_LIB)
    target_link_libraries(FirebaseApp PRIVATE ${UUID_LIB})
  endif()
endif()

if(MSVC)
  add_compile_options(/W4 /WX-)
  add_compile_options(/utf-8)
endif()