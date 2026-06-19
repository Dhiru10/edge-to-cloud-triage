#include "util/config.h"

#include <cstdlib>
#include <unistd.h>
#include <fstream>
#include <sstream>
#include <array>

static std::string env_or(const char* key, const char* fallback) {
    const char* val = std::getenv(key);
    return val ? val : fallback;
}

static std::string env_or(const char* key, std::string fallback) {
    const char* val = std::getenv(key);
    return val ? val : std::move(fallback);
}

static int env_int(const char* key, int fallback) {
    const char* val = std::getenv(key);
    return val ? std::stoi(val) : fallback;
}

static std::string detect_hostname() {
    char buf[256];
    if (gethostname(buf, sizeof(buf)) == 0) return buf;
    return "unknown-host";
}

static std::string detect_os_info() {
    std::ifstream f("/etc/os-release");
    if (!f) return "Linux";
    std::string line;
    while (std::getline(f, line)) {
        if (line.rfind("PRETTY_NAME=", 0) == 0) {
            std::string val = line.substr(12);
            if (!val.empty() && val.front() == '"') val = val.substr(1);
            if (!val.empty() && val.back()  == '"') val.pop_back();
            return val;
        }
    }
    return "Linux";
}

EdgeConfig load_from_env() {
    EdgeConfig cfg;
    cfg.backend_url             = env_or("BACKEND_URL", "http://localhost:8080");
    cfg.api_key                 = env_or("API_KEY",     "dev-api-key-change-in-prod");
    cfg.hostname                = env_or("HOSTNAME_OVERRIDE", detect_hostname());
    cfg.os_info                 = detect_os_info();
    cfg.agent_version           = env_or("AGENT_VERSION", "1.0.0");
    cfg.collect_interval_sec    = env_int("COLLECT_INTERVAL_SECONDS",    15);
    cfg.fault_scan_interval_sec = env_int("FAULT_SCAN_INTERVAL_SECONDS", 10);
    cfg.send_interval_sec       = env_int("SEND_INTERVAL_SECONDS",       30);
    cfg.batch_size              = env_int("BATCH_SIZE",                  20);
    return cfg;
}
