#pragma once

#include <string>
#include <chrono>
#include <ctime>

struct TelemetrySnapshot {
    std::string captured_at;
    double      cpu_pct       = 0.0;
    int         mem_used_mb   = 0;
    int         mem_total_mb  = 0;
    double      disk_used_gb  = 0.0;
    double      disk_total_gb = 0.0;
    double      load_avg_1m   = 0.0;
};

struct FaultEvent {
    std::string occurred_at;
    std::string fault_type;
    std::string process_name;
    int         exit_code = -1;
    std::string raw_log;
};

inline std::string iso_now() {
    auto now = std::chrono::system_clock::now();
    std::time_t t = std::chrono::system_clock::to_time_t(now);
    char buf[32];
    std::strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%SZ", std::gmtime(&t));
    return buf;
}
