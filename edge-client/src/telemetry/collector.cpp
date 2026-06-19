#include "telemetry/collector.h"

#include <fstream>
#include <sstream>
#include <thread>
#include <chrono>
#include <sys/statvfs.h>
#include <iostream>

TelemetryCollector::TelemetryCollector(const EdgeConfig& config,
                                        SafeQueue<TelemetrySnapshot>& queue,
                                        std::atomic<bool>& running)
    : config_(config), queue_(queue), running_(running) {}

void TelemetryCollector::run() {
    while (running_) {
        try {
            queue_.push(collect());
        } catch (const std::exception& ex) {
            std::cerr << "[collector] error: " << ex.what() << "\n";
        }

        for (int i = 0; i < config_.collect_interval_sec * 10 && running_; ++i) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
    }
}

TelemetrySnapshot TelemetryCollector::collect() {
    TelemetrySnapshot snap;
    snap.captured_at  = iso_now();
    snap.cpu_pct      = read_cpu_pct();
    snap.load_avg_1m  = read_load_avg();
    read_memory(snap.mem_used_mb, snap.mem_total_mb);
    read_disk(snap.disk_used_gb, snap.disk_total_gb);
    return snap;
}

double TelemetryCollector::read_cpu_pct() {
    auto read_stat = []() -> std::array<long long, 7> {
        std::ifstream f("/proc/stat");
        std::string label;
        std::array<long long, 7> fields{};
        if (f >> label) {
            for (auto& v : fields) f >> v;
        }
        return fields;  // user nice system idle iowait irq softirq
    };

    auto s1 = read_stat();
    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    auto s2 = read_stat();

    long long idle1  = s1[3] + s1[4];
    long long total1 = s1[0]+s1[1]+s1[2]+s1[3]+s1[4]+s1[5]+s1[6];
    long long idle2  = s2[3] + s2[4];
    long long total2 = s2[0]+s2[1]+s2[2]+s2[3]+s2[4]+s2[5]+s2[6];

    long long d_total = total2 - total1;
    long long d_idle  = idle2  - idle1;
    if (d_total <= 0) return 0.0;

    return 100.0 * static_cast<double>(d_total - d_idle) / static_cast<double>(d_total);
}

void TelemetryCollector::read_memory(int& used_mb, int& total_mb) {
    std::ifstream f("/proc/meminfo");
    std::string key;
    long long value;
    std::string unit;

    long long mem_total = 0, mem_available = 0;
    int found = 0;

    while (f >> key >> value >> unit && found < 2) {
        if (key == "MemTotal:")     { mem_total     = value; ++found; }
        if (key == "MemAvailable:") { mem_available = value; ++found; }
    }

    total_mb = static_cast<int>(mem_total / 1024);
    used_mb  = static_cast<int>((mem_total - mem_available) / 1024);
}

void TelemetryCollector::read_disk(double& used_gb, double& total_gb) {
    struct statvfs st{};
    if (statvfs("/", &st) != 0) {
        used_gb = total_gb = 0.0;
        return;
    }
    const double gb = 1024.0 * 1024.0 * 1024.0;
    total_gb = static_cast<double>(st.f_blocks) * st.f_frsize / gb;
    double avail_gb = static_cast<double>(st.f_bavail) * st.f_frsize / gb;
    used_gb = total_gb - avail_gb;
}

double TelemetryCollector::read_load_avg() {
    std::ifstream f("/proc/loadavg");
    double load1 = 0.0;
    f >> load1;
    return load1;
}
