#pragma once

#include "common/types.h"
#include "util/safe_queue.h"
#include "util/config.h"
#include <atomic>
#include <string>
#include <curl/curl.h>

class HttpSender {
public:
    HttpSender(const EdgeConfig& config,
               SafeQueue<TelemetrySnapshot>& telemetry_queue,
               SafeQueue<FaultEvent>& fault_queue,
               std::atomic<bool>& running);

    ~HttpSender();

    bool register_device();
    void run();

    const std::string& device_id() const { return device_id_; }

private:
    std::string post(const std::string& path, const std::string& body);

    void flush_telemetry();
    void flush_faults();

    const EdgeConfig&              config_;
    SafeQueue<TelemetrySnapshot>&  telemetry_queue_;
    SafeQueue<FaultEvent>&         fault_queue_;
    std::atomic<bool>&             running_;

    std::string device_id_;
    CURL*       curl_ = nullptr;
};
