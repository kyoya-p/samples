# Project History: C++ Mouse TUI Sample

## Date: 2026-01-17

### Goal
Create a Terminal User Interface (TUI) application in C++ that supports mouse interactions (clicking, hovering) using a modern library.

### Technology Stack
- **Language**: C++17
- **Library**: [FTXUI](https://github.com/ArthurSonzogni/FTXUI) (Functional Terminal User Interface)
- **Build System**: CMake

### Implementation Steps

1.  **Project Initialization**
    -   Set up the project directory structure.
    -   Verified C++ compiler (`g++`) and CMake versions.

2.  **Configuration (`CMakeLists.txt`)**
    -   Configured CMake to fetch `FTXUI` v5.0.0 automatically from GitHub.
    -   Set C++ standard to C++17.
    -   Linked necessary FTXUI modules (`screen`, `dom`, `component`).

3.  **Source Code (`main.cpp`)**
    -   Implemented a main loop using `ScreenInteractive::TerminalOutput()`.
    -   **Components Added**:
        -   `Button`: Updates an internal counter and its own label upon clicking.
        -   `Checkbox`: Toggles state on click.
        -   `Menu`: Selectable list of options.
    -   **Layout**: Used `vbox` (vertical box) and `hbox` (horizontal box) to arrange components with borders and separators.
    -   **Event Handling**: Added a `CatchEvent` wrapper to handle 'q' or 'Esc' keys for exiting the application.

4.  **Build Process**
    -   Created a `build` directory.
    -   Ran `cmake ..` to generate makefiles and download dependencies.
    -   Ran `make` to compile the executable `mouse_tui`.

### How to Run
```bash
./build/mouse_tui
```

### Features
- **Mouse Support**: All components (Button, Checkbox, Menu) respond to mouse clicks.
- **Reactive UI**: The interface updates immediately upon interaction.
- **Cross-Platform Potential**: FTXUI is designed to work on Linux, macOS, and Windows (via new Windows Terminal).
