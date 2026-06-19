#pragma once

#include <queue>
#include <mutex>
#include <condition_variable>
#include <vector>
#include <chrono>

template<typename T>
class SafeQueue {
public:
    void push(T item) {
        std::lock_guard<std::mutex> lock(mutex_);
        queue_.push(std::move(item));
        cv_.notify_one();
    }

    bool pop(T& out, std::chrono::milliseconds timeout) {
        std::unique_lock<std::mutex> lock(mutex_);
        if (!cv_.wait_for(lock, timeout, [this] { return !queue_.empty(); })) {
            return false;
        }
        out = std::move(queue_.front());
        queue_.pop();
        return true;
    }

    std::vector<T> drain(size_t max_count) {
        std::lock_guard<std::mutex> lock(mutex_);
        std::vector<T> items;
        while (!queue_.empty() && items.size() < max_count) {
            items.push_back(std::move(queue_.front()));
            queue_.pop();
        }
        return items;
    }

    bool empty() const {
        std::lock_guard<std::mutex> lock(mutex_);
        return queue_.empty();
    }

    size_t size() const {
        std::lock_guard<std::mutex> lock(mutex_);
        return queue_.size();
    }

private:
    mutable std::mutex      mutex_;
    std::queue<T>           queue_;
    std::condition_variable cv_;
};
