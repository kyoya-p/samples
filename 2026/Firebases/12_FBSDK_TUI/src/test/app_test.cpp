#include <gtest/gtest.h>
#include "ftxui/component/component.hpp"
#include "ftxui/component/event.hpp"
#include "ftxui/screen/screen.hpp"
#include "dataaccess.hpp"
#include "utils.hpp"
#include "ui.hpp"
#include <fstream>
#include <cstdio>

using namespace ftxui;

TEST(HelpersTest, GenerateRandomName) {
    std::string name = GenerateRandomName();
    EXPECT_FALSE(name.empty());
    EXPECT_TRUE(name.find(" ") != std::string::npos); // First Last
}

TEST(HelpersTest, GenerateRandomEmail) {
    std::string name = "John Doe";
    std::string email = GenerateRandomEmail(name);
    EXPECT_EQ(email, "john.doe@example.com");
    
    std::string name2 = "Alice";
    std::string email2 = GenerateRandomEmail(name2);
    EXPECT_EQ(email2, "alice@example.com");
}


// --- Utils Test ---
TEST(UtilsTest, LogCreation) {
    // Ensure log file can be written to
    std::string& logFile = GetLogFilename();
    std::remove(logFile.c_str()); // Clean up before test

    Log("Test Log Message");

    std::ifstream ifs(logFile);
    EXPECT_TRUE(ifs.is_open());
    
    std::string line;
    std::getline(ifs, line);
    EXPECT_TRUE(line.find("Test Log Message") != std::string::npos);
    
    ifs.close();
}


// --- Logic Class Test ---
TEST(LogicTest, FirestoreServiceInit) {
    const char* env_key = std::getenv("FB_API_KEY");
    if (!env_key) env_key = std::getenv("API_KEY");

    if (!env_key) {
        GTEST_SKIP() << "Skipping Firestore test: API_KEY not set.";
    }

    FirestoreService service([]{});
    // With a valid key, it should try to initialize. 
    // Note: This might still fail if the key is invalid or network issues, 
    // but we can check initial state.
    bool init_result = service.Initialize(env_key, 10);
    
    // We expect true if key format is correct and network is reachable.
    // Initialize calls StartListeningNextPage which sets is_loading_ = true immediately.
    EXPECT_TRUE(service.IsLoading()); 
    
    // Allow some time for async thread (optional, mostly to ensure no crash)
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    
    // Check basic state getters
    // Note: contacts might still be empty if load hasn't finished or failed
    EXPECT_TRUE(service.GetContacts().empty()); 
}


#include "ui.hpp"

// ... existing tests ...

// --- TUI Integration Test ---
TEST(TUIIntegrationTest, AppFullRendering) {
    FirestoreService service([]{});
    // Use snapshot mode for consistent testing
    auto root = CreateAppUI(service, []{}, true); 
    
    auto screen = Screen::Create(Dimension::Fixed(80), Dimension::Fixed(20));
    Render(screen, root->Render());

    std::string output = screen.ToString();
    
    // Verify key UI elements are present
    EXPECT_TRUE(output.find("Status: Disconnected") != std::string::npos);
    EXPECT_TRUE(output.find("Name") != std::string::npos);
    EXPECT_TRUE(output.find("Mail") != std::string::npos);
    EXPECT_TRUE(output.find("[Add]") != std::string::npos);
    EXPECT_TRUE(output.find("[Activate]") != std::string::npos);
    EXPECT_TRUE(output.find("[Close]") != std::string::npos);
}

TEST(TUIIntegrationTest, AppEventHandling) {
    bool exited = false;
    FirestoreService service([]{});
    auto root = CreateAppUI(service, [&] { exited = true; }, false);

    // Simulate 'q' key to exit
    root->OnEvent(Event::Character('q'));
    EXPECT_TRUE(exited);
}