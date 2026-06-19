#pragma once

#include "common/types.h"
#include "util/safe_queue.h"
#include "util/config.h"
#include <atomic>

class TelemetryCollector {
public:
    TelemetryCollector(const EdgeConfig& config,
                       SafeQueue<TelemetrySnapshot>& queue,
                       std::atomic<bool>& running);

    void run();

private:
    TelemetrySnapshot collect();

    double      read_cpu_pct();
    void        read_memory(int& used_mb, int& total_mb);
    void        read_disk(double& used_gb, double& total_gb);
    double      read_load_avg();

    const EdgeConfig&              config_;
    SafeQueue<TelemetrySnapshot>&  queue_;
    std::atomic<bool>&             running_;
};
