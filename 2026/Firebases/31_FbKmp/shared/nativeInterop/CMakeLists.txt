
cmake_minimum_required(VERSION 3.11)
project(ftxui_wrapper)

# FTXUIを取り込む
add_subdirectory(external/ftxui)

# ラッパーライブラリを作成
add_library(ftxui_wrapper STATIC src/ftxui_wrapper.cpp)
target_include_directories(ftxui_wrapper PUBLIC include)
target_link_libraries(ftxui_wrapper PRIVATE ftxui::screen ftxui::dom ftxui::component)
