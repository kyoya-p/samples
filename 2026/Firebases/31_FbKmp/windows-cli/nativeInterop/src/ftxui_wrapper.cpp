#include "ftxui_wrapper.h"
#include <iostream>

extern "C" {
    void hello_ftxui() {
        std::cout << "Hello from FTXUI wrapper!" << std::endl;
    }
}
