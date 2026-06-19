#pragma once

#include <string>

struct EdgeConfig {
    std::string backend_url;
    std::string api_key;
    std::string hostname;
    std::string os_info;
    std::string agent_version   = "1.0.0";
    int collect_interval_sec    = 15;
    int fault_scan_interval_sec = 10;
    int send_interval_sec       = 30;
    int batch_size              = 20;
};

EdgeConfig load_from_env();
