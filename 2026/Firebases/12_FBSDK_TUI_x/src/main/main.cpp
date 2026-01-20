#include <algorithm>
#include <chrono>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <memory>
#include <random>
#include <string>
#include <vector>
#include <thread>
#include <mutex>
#include <atomic>
#include <unordered_map>
#include <exception>
#include <ctime>

#ifdef _WIN32
#include <process.h>
#include <windows.h>
#else
#include <unistd.h>
#endif

#include "ftxui/component/captured_mouse.hpp"
#include "ftxui/component/component.hpp"
#include "ftxui/component/component_base.hpp"
#include "ftxui/component/component_options.hpp"
#include "ftxui/component/screen_interactive.hpp"
#include "ftxui/dom/elements.hpp"
#include "ftxui/util/ref.hpp"
#include "ftxui/screen/terminal.hpp"

#include "dataaccess.hpp"
#include "utils.hpp"
#include "ui.hpp"

#ifdef RGB
#undef RGB
#endif

using namespace ftxui;

int main(int argc, char** argv) {
  try {
      std::ofstream(GetLogFilename(), std::ios::trunc);
      if (argc > 1 && std::string(argv[1]) == "--check") {
          std::cout << "Verification: OK" << std::endl;
          return 0;
      }
      bool snapshot_mode = (argc > 1 && std::string(argv[1]) == "--snapshot");
      
      if (snapshot_mode) {
          FirestoreService service([]{});
          const char* k = std::getenv("FB_API_KEY");
          if (!k) k = std::getenv("API_KEY");
          if (k) service.Initialize(k, 20);
          for(int i=0; i<20; ++i) { if(!service.GetContacts().empty()) break; std::this_thread::sleep_for(std::chrono::milliseconds(200)); }
          auto root = CreateAppUI(service, []{}, true);
          auto screen = Screen::Create(Dimension::Fixed(100), Dimension::Fixed(30));
          Render(screen, root->Render());
          std::cout << screen.ToString() << std::endl;
          return 0;
      }
      auto screen = ScreenInteractive::Fullscreen();
      std::atomic<bool> started{false};
      FirestoreService service([&screen, &started] { if (started) screen.Post(Event::Custom); });
      int nAddr = Terminal::Size().dimy + 5;
      const char* key = std::getenv("FB_API_KEY");
      if (!key) key = std::getenv("API_KEY");
      if (key) service.Initialize(key, nAddr);
      auto root = CreateAppUI(service, screen.ExitLoopClosure(), false);
      started = true;
      screen.Loop(root);
  } catch (...) { return 1; }
  return 0;
}