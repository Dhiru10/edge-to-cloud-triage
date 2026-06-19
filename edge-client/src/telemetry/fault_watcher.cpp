#include "telemetry/fault_watcher.h"

#include <filesystem>
#include <fstream>
#include <sstream>
#include <thread>
#include <chrono>
#include <algorithm>
#include <iostream>

namespace fs = std::filesystem;

FaultWatcher::FaultWatcher(const EdgeConfig& config,
                             SafeQueue<FaultEvent>& queue,
                             std::atomic<bool>& running)
    : config_(config), queue_(queue), running_(running) {}

void FaultWatcher::run() {
    while (running_) {
        try {
            scan_proc();
        } catch (const std::exception& ex) {
            std::cerr << "[fault_watcher] error: " << ex.what() << "\n";
        }

        ++scan_count_;
        if (scan_count_ % 60 == 0) {
            reported_pids_.clear();
        }

        for (int i = 0; i < config_.fault_scan_interval_sec * 10 && running_; ++i) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
    }
}

void FaultWatcher::scan_proc() {
    std::error_code ec;
    for (const auto& entry : fs::directory_iterator("/proc", ec)) {
        if (ec) break;
        if (!entry.is_directory()) continue;

        const std::string name = entry.path().filename().string();
        if (!std::all_of(name.begin(), name.end(), ::isdigit)) continue;

        int pid = std::stoi(name);
        if (reported_pids_.count(pid)) continue;

        std::string proc_name;
        char state = '\0';
        if (!read_proc_status(pid, proc_name, state)) continue;

        if (state == 'Z') {
            reported_pids_.insert(pid);

            FaultEvent event;
            event.occurred_at  = iso_now();
            event.fault_type   = "PROCESS_EXIT";
            event.process_name = proc_name;
            event.exit_code    = -1;
            event.raw_log      = "Process " + proc_name + " (pid " +
                                  std::to_string(pid) + ") exited unexpectedly (zombie state).";

            std::cout << "[fault_watcher] detected zombie: " << proc_name
                      << " (pid " << pid << ")\n";
            queue_.push(std::move(event));
        }
    }
}

bool FaultWatcher::read_proc_status(int pid, std::string& name, char& state) {
    std::ifstream f("/proc/" + std::to_string(pid) + "/status");
    if (!f) return false;

    std::string key, value;
    bool got_name = false, got_state = false;

    while (f >> key) {
        if (key == "Name:") {
            f >> name;
            got_name = true;
        } else if (key == "State:") {
            f >> state;
            got_state = true;
        }
        if (got_name && got_state) break;
        f.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
    }
    return got_name && got_state;
}
