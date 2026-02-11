
#ifndef FTXUI_WRAPPER_H
#define FTXUI_WRAPPER_H

#include <stdio.h>

#ifdef __cplusplus
extern "C" {
#endif

static inline void hello_ftxui() {
    printf("Hello from FTXUI Wrapper (C++ inline)!\n");
}

#ifdef __cplusplus
}
#endif

#endif // FTXUI_WRAPPER_H
