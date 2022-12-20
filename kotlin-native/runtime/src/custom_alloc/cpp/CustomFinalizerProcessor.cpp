/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CustomFinalizerProcessor.hpp"

#include <mutex>
#include <thread>

#include "AtomicStack.hpp"
#include "Cleaner.h"
#include "CustomLogging.hpp"
#include "ExtraObjectData.hpp"
#include "FinalizerHooks.hpp"
#include "Runtime.h"
#include "WorkerBoundReference.h"

namespace kotlin::alloc {

void CustomFinalizerProcessor::StartFinalizerThreadIfNone() noexcept {
    CustomAllocDebug("CustomFinalizerProcessor::StartFinalizerThreadIfNone()");
    std::unique_lock guard(threadCreatingMutex_);
    if (finalizerThread_.joinable()) return;

    finalizerThread_ = ScopedThread(ScopedThread::attributes().name("Custom finalizer processor"), [this] {
        Kotlin_initRuntimeIfNeeded();
        {
            std::unique_lock guard(initializedMutex_);
            initialized_ = true;
        }
        initializedCondVar_.notify_all();
        int64_t finalizersEpoch = 0;
        CustomAllocDebug("CustomFinalizerProcessor: begin");
        while (true) {
            CustomAllocDebug("CustomFinalizerProcessor: loop");
            std::unique_lock lock(finalizerQueueMutex_);
            finalizerQueueCondVar_.wait(lock, [this, &finalizersEpoch] {
                return !finalizerQueue_.isEmpty() || finalizerQueueEpoch_ != finalizersEpoch || shutdownFlag_;
            });
            if (finalizerQueue_.isEmpty() && finalizerQueueEpoch_ == finalizersEpoch) {
                newTasksAllowed_ = false;
                RuntimeAssert(shutdownFlag_, "Nothing to do, but no shutdownFlag_ is set on wakeup");
                break;
            }
            finalizersEpoch = finalizerQueueEpoch_;
            lock.unlock();
            CustomAllocDebug("CustomFinalizerProcessor: unlock");
            if (!finalizerQueue_.isEmpty()) {
                ThreadStateGuard guard(ThreadState::kRunnable);
                mm::ExtraObjectData* extraObject;
                while ((extraObject = finalizerQueue_.Pop())) {
                    CustomAllocDebug("CustomFinalizerProcessor: finalizing %p", extraObject);
                    auto* baseObject = extraObject->GetBaseObject();
                    CustomAllocDebug("CustomFinalizerProcessor: baseObject %p", baseObject);
                    if (kotlin::HasFinalizers(baseObject)) {
                        auto* type = baseObject->type_info();
                        if (type == theCleanerImplTypeInfo) {
                            DisposeCleaner(baseObject);
                        } else if (type == theWorkerBoundReferenceTypeInfo) {
                            DisposeWorkerBoundReference(baseObject);
                        }
                    }
                    extraObject->Uninstall();
                    extraObject->setFlag(mm::ExtraObjectData::FLAGS_FINALIZED);
                }
            } else {
                CustomAllocDebug("CustomFinalizerProcessor: empty queue");
            }
            epochDoneCallback_(finalizersEpoch);
        }
        {
            std::unique_lock guard(initializedMutex_);
            initialized_ = false;
        }
        initializedCondVar_.notify_all();
        CustomAllocDebug("CustomFinalizerProcessor: done");
    });
}

void CustomFinalizerProcessor::StopFinalizerThread() noexcept {
    CustomAllocDebug("CustomFinalizerProcessor::StopFinalizerThread()");
    {
        std::unique_lock guard(finalizerQueueMutex_);
        if (!finalizerThread_.joinable()) return;
        shutdownFlag_ = true;
        finalizerQueueCondVar_.notify_all();
    }
    finalizerThread_.join();
    shutdownFlag_ = false;
    RuntimeAssert(finalizerQueue_.isEmpty(), "Finalizer queue should be empty when killing finalizer thread");
    std::unique_lock guard(finalizerQueueMutex_);
    newTasksAllowed_ = true;
    finalizerQueueCondVar_.notify_all();
}

void CustomFinalizerProcessor::SetEpoch(int64_t epoch) noexcept {
    CustomAllocDebug("CustomFinalizerProcessor::SetEpoch(%" PRIu64 ")", epoch);
    StartFinalizerThreadIfNone();
    finalizerQueueEpoch_ = epoch;
    finalizerQueueCondVar_.notify_all();
}

bool CustomFinalizerProcessor::IsRunning() noexcept {
    return finalizerThread_.joinable();
}

void CustomFinalizerProcessor::WaitFinalizerThreadInitialized() noexcept {
    CustomAllocDebug("CustomFinalizerProcessor::WaitFinalizerThreadInitialized()");
    std::unique_lock guard(initializedMutex_);
    initializedCondVar_.wait(guard, [this] { return initialized_; });
}

CustomFinalizerProcessor::~CustomFinalizerProcessor() {
    StopFinalizerThread();
}

} // namespace kotlin::alloc
