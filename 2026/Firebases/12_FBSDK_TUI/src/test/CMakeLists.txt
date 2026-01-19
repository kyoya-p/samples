add_executable(app_tests 
    app_test.cpp 
    ../main/dataaccess.cpp
    ../main/utils.cpp
    
    ../main/ui.cpp
)

target_link_libraries(app_tests
  PRIVATE
    ftxui::screen
    ftxui::dom
    ftxui::component
    GTest::gtest_main
    ${FIREBASE_LIBS}
    ${SYSTEM_LIBS}
)

target_include_directories(app_tests 
  PRIVATE 
    ../main
    ${FIREBASE_INCLUDE_DIR}
)

include(GoogleTest)
gtest_discover_tests(app_tests)
