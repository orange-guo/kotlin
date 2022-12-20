/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_
#define CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_

#include "AtomicStack.hpp"
#include "ConcurrentMarkAndSweep.hpp"
#include "ExtraObjectData.hpp"
#include "GCState.hpp"
#include "ScopedThread.hpp"

namespace kotlin::alloc {

class CustomFinalizerProcessor : Pinned {
public:
    explicit CustomFinalizerProcessor(std::function<void(int64_t)> epochDoneCallback) : epochDoneCallback_(std::move(epochDoneCallback)) {}
    void StopFinalizerThread() noexcept;
    void SetEpoch(int64_t epoch) noexcept;
    bool IsRunning() noexcept;
    void StartFinalizerThreadIfNone() noexcept;
    void WaitFinalizerThreadInitialized() noexcept;
    ~CustomFinalizerProcessor();

private:
    friend class gc::ConcurrentMarkAndSweep;
    ScopedThread finalizerThread_;
    AtomicStack<ExtraObjectCell> finalizerQueue_;
    std::condition_variable finalizerQueueCondVar_;
    std::mutex finalizerQueueMutex_;
    std::function<void(int64_t)> epochDoneCallback_;
    int64_t finalizerQueueEpoch_ = 0;
    bool shutdownFlag_ = false;
    bool newTasksAllowed_ = true;

    std::mutex initializedMutex_;
    std::condition_variable initializedCondVar_;
    bool initialized_ = false;

    std::mutex threadCreatingMutex_;
};

} // namespace kotlin::alloc

#endif // CUSTOM_ALLOC_CPP_CUSTOMFINALIZERPROCESSOR_HPP_
