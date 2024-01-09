cmake_minimum_required(VERSION 3.15)

project(MyDeviceSDKCppProject)

# Minimum C++ standard
set(CMAKE_CXX_STANDARD 17)

# In a Windows install, set these compiler flags:
if(MSVC)
   set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /Zc:preprocessor /bigobj")
endif()

# Include the FetchContent module so you can download the C++ SDK
Include(FetchContent)

# Declare the version of the C++ SDK you want to download
FetchContent_Declare(
  cpprealm
  GIT_REPOSITORY https://github.com/realm/realm-cpp.git
  GIT_TAG        v1.0.0
)

# The MakeAvailable command ensures the named dependencies have been populated
FetchContent_MakeAvailable(cpprealm)

# Create an executable target called myApp with the source file main.cpp
add_executable(myApp main.cpp)

target_link_libraries(myApp PRIVATE cpprealm)

