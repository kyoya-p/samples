cmake_minimum_required(VERSION 3.10)
project(FirebaseCppSample CXX)

# Firebase C++ SDKの場所を指定させる
if(NOT DEFINED FIREBASE_CPP_SDK_DIR)
  message(FATAL_ERROR "Please define FIREBASE_CPP_SDK_DIR to the path of the Firebase C++ SDK.")
endif()

# Firebase SDKを取り込む
add_subdirectory(${FIREBASE_CPP_SDK_DIR} bin/ EXCLUDE_FROM_ALL)

add_executable(FirebaseApp src/main.cpp)

# 必要なライブラリをリンク
# Windowsでは firebase_app, firebase_auth, firebase_firestore を使用
target_link_libraries(FirebaseApp
  firebase_auth
  firebase_firestore
  firebase_app
)

if(UNIX AND NOT APPLE)
  # Linux (WSL) では追加のシステムライブラリが必要
  find_package(CURL REQUIRED)
  find_package(ZLIB REQUIRED)
  find_package(OpenSSL REQUIRED)
  target_link_libraries(FirebaseApp 
    pthread 
    atomic 
    dl 
    ${CURL_LIBRARIES}
    ${ZLIB_LIBRARIES}
    ${OPENSSL_LIBRARIES}
  )
  # uuid が必要な場合がある
  find_library(UUID_LIB uuid)
  if(UUID_LIB)
    target_link_libraries(FirebaseApp ${UUID_LIB})
  endif()
endif()

# C++17標準を使用
set_property(TARGET FirebaseApp PROPERTY CXX_STANDARD 17)

if(MSVC)
  # 一般的なMSVC設定
  add_compile_options(/W4 /WX-)
endif()
