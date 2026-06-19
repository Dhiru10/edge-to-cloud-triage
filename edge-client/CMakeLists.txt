cmake_minimum_required(VERSION 3.22)
project(edge-client LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
FetchContent_Declare(
    json
    URL https://github.com/nlohmann/json/releases/download/v3.11.3/json.tar.xz
    URL_HASH SHA256=d6c65aca6b1ed68e7a182f4757257b107ae403032760ed6ef121c9d55e81757d
)
FetchContent_MakeAvailable(json)

find_package(CURL REQUIRED)

add_executable(edge-client
    src/main.cpp
    src/util/config.cpp
    src/telemetry/collector.cpp
    src/telemetry/fault_watcher.cpp
    src/transport/http_sender.cpp
)

target_include_directories(edge-client PRIVATE include)
target_link_libraries(edge-client PRIVATE CURL::libcurl nlohmann_json::nlohmann_json)
target_compile_options(edge-client PRIVATE -Wall -Wextra -O2)
