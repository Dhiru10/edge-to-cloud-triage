#include "transport/http_sender.h"

#include <nlohmann/json.hpp>
#include <iostream>
#include <sstream>
#include <thread>
#include <chrono>
#include <iomanip>
#include <cmath>

using json = nlohmann::json;

static size_t write_cb(char* ptr, size_t size, size_t nmemb, std::string* data) {
    data->append(ptr, size * nmemb);
    return size * nmemb;
}

HttpSender::HttpSender(const EdgeConfig& config,
                        SafeQueue<TelemetrySnapshot>& tq,
                        SafeQueue<FaultEvent>& fq,
                        std::atomic<bool>& running)
    : config_(config), telemetry_queue_(tq), fault_queue_(fq), running_(running) {
    curl_global_init(CURL_GLOBAL_DEFAULT);
    curl_ = curl_easy_init();
}

HttpSender::~HttpSender() {
    if (curl_) curl_easy_cleanup(curl_);
    curl_global_cleanup();
}

bool HttpSender::register_device() {
    json body;
    body["hostname"]     = config_.hostname;
    body["osInfo"]       = config_.os_info;
    body["agentVersion"] = config_.agent_version;

    std::string response = post("/api/devices/register", body.dump());
    if (response.empty()) {
        std::cerr << "[sender] registration failed: no response\n";
        return false;
    }

    try {
        auto parsed = json::parse(response);
        device_id_ = parsed.at("id").get<std::string>();
        std::cout << "[sender] registered as device " << device_id_ << "\n";
        return true;
    } catch (const std::exception& ex) {
        std::cerr << "[sender] registration parse error: " << ex.what() << "\n";
        return false;
    }
}

void HttpSender::run() {
    auto last_send = std::chrono::steady_clock::now();

    while (running_) {
        flush_faults();

        auto now = std::chrono::steady_clock::now();
        auto elapsed = std::chrono::duration_cast<std::chrono::seconds>(now - last_send).count();
        if (elapsed >= config_.send_interval_sec) {
            flush_telemetry();
            last_send = std::chrono::steady_clock::now();
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(500));
    }

    flush_faults();
    flush_telemetry();
}

void HttpSender::flush_telemetry() {
    auto snapshots = telemetry_queue_.drain(static_cast<size_t>(config_.batch_size));
    if (snapshots.empty()) return;

    json body;
    body["deviceId"]  = device_id_;
    body["snapshots"] = json::array();

    for (const auto& s : snapshots) {
        json snap;
        snap["capturedAt"]  = s.captured_at;
        snap["cpuPct"]      = std::round(s.cpu_pct * 100.0) / 100.0;
        snap["memUsedMb"]   = s.mem_used_mb;
        snap["memTotalMb"]  = s.mem_total_mb;
        snap["diskUsedGb"]  = std::round(s.disk_used_gb * 100.0) / 100.0;
        snap["diskTotalGb"] = std::round(s.disk_total_gb * 100.0) / 100.0;
        snap["loadAvg1m"]   = std::round(s.load_avg_1m * 1000.0) / 1000.0;
        body["snapshots"].push_back(snap);
    }

    std::string resp = post("/api/telemetry", body.dump());
    std::cout << "[sender] sent " << snapshots.size() << " telemetry snapshots\n";
}

void HttpSender::flush_faults() {
    FaultEvent event;
    while (fault_queue_.pop(event, std::chrono::milliseconds(0))) {
        json body;
        body["deviceId"]    = device_id_;
        body["occurredAt"]  = event.occurred_at;
        body["faultType"]   = event.fault_type;
        body["processName"] = event.process_name;
        body["exitCode"]    = event.exit_code;
        body["rawLog"]      = event.raw_log;

        std::string resp = post("/api/faults", body.dump());
        if (!resp.empty()) {
            std::cout << "[sender] fault event submitted: " << event.process_name << "\n";
        } else {
            std::cerr << "[sender] failed to submit fault event: " << event.process_name << "\n";
        }
    }
}

std::string HttpSender::post(const std::string& path, const std::string& body) {
    if (!curl_) return "";

    std::string url      = config_.backend_url + path;
    std::string response;
    long        http_code = 0;

    curl_easy_reset(curl_);

    struct curl_slist* headers = nullptr;
    headers = curl_slist_append(headers, "Content-Type: application/json");
    std::string auth_header = "X-Api-Key: " + config_.api_key;
    headers = curl_slist_append(headers, auth_header.c_str());

    curl_easy_setopt(curl_, CURLOPT_URL, url.c_str());
    curl_easy_setopt(curl_, CURLOPT_HTTPHEADER, headers);
    curl_easy_setopt(curl_, CURLOPT_POSTFIELDS, body.c_str());
    curl_easy_setopt(curl_, CURLOPT_WRITEFUNCTION, write_cb);
    curl_easy_setopt(curl_, CURLOPT_WRITEDATA, &response);
    curl_easy_setopt(curl_, CURLOPT_TIMEOUT, 10L);
    curl_easy_setopt(curl_, CURLOPT_CONNECTTIMEOUT, 5L);

    CURLcode res = curl_easy_perform(curl_);
    curl_easy_getinfo(curl_, CURLINFO_RESPONSE_CODE, &http_code);
    curl_slist_free_all(headers);

    if (res != CURLE_OK) {
        std::cerr << "[sender] curl error on " << path << ": "
                  << curl_easy_strerror(res) << "\n";
        return "";
    }
    if (http_code >= 400) {
        std::cerr << "[sender] HTTP " << http_code << " on " << path
                  << ": " << response << "\n";
        return "";
    }
    return response;
}
