#include "util/config.h"
#include "util/safe_queue.h"
#include "common/types.h"
#include "telemetry/collector.h"
#include "telemetry/fault_watcher.h"
#include "transport/http_sender.h"

#include <iostream>
#include <thread>
#include <atomic>
#include <csignal>
#include <chrono>

static std::atomic<bool> running{true};

static void on_signal(int) {
    running.store(false);
}

int main() {
    std::signal(SIGTERM, on_signal);
    std::signal(SIGINT,  on_signal);

    EdgeConfig config = load_from_env();

    std::cout << "[edge-client] starting on host: " << config.hostname << "\n";
    std::cout << "[edge-client] backend: " << config.backend_url << "\n";

    SafeQueue<TelemetrySnapshot> telemetry_queue;
    SafeQueue<FaultEvent>        fault_queue;

    HttpSender sender(config, telemetry_queue, fault_queue, running);

    // Retry registration up to 5 times before giving up
    bool registered = false;
    for (int attempt = 1; attempt <= 5 && running; ++attempt) {
        if (sender.register_device()) {
            registered = true;
            break;
        }
        std::cerr << "[edge-client] registration attempt " << attempt
                  << "/5 failed, retrying in 5s...\n";
        std::this_thread::sleep_for(std::chrono::seconds(5));
    }

    if (!registered) {
        std::cerr << "[edge-client] could not register with backend, exiting\n";
        return 1;
    }

    TelemetryCollector collector(config, telemetry_queue, running);
    FaultWatcher       watcher(config, fault_queue, running);

    std::thread collector_thread([&] { collector.run(); });
    std::thread watcher_thread([&]  { watcher.run(); });
    std::thread sender_thread([&]   { sender.run(); });

    std::cout << "[edge-client] all threads started\n";

    collector_thread.join();
    watcher_thread.join();
    sender_thread.join();

    std::cout << "[edge-client] shutdown complete\n";
    return 0;
}
