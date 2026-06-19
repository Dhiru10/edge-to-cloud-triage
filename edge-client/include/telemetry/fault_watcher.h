#pragma once

#include "common/types.h"
#include "util/safe_queue.h"
#include "util/config.h"
#include <atomic>
#include <set>

class FaultWatcher {
public:
    FaultWatcher(const EdgeConfig& config,
                 SafeQueue<FaultEvent>& queue,
                 std::atomic<bool>& running);

    void run();

private:
    void scan_proc();
    bool read_proc_status(int pid, std::string& name, char& state);

    const EdgeConfig&      config_;
    SafeQueue<FaultEvent>& queue_;
    std::atomic<bool>&     running_;
    std::set<int>          reported_pids_;
    int                    scan_count_ = 0;
};
