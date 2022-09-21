/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCApi.hpp"

#include <limits>

#include "ConcurrentMarkAndSweep.hpp"
#include "CustomLogging.hpp"
#include "FinalizerHooks.hpp"
#include "ObjectFactory.hpp"

namespace kotlin::alloc {

bool TryResetMark(void* ptr) noexcept {
    using Node = typename kotlin::mm::ObjectFactory<kotlin::gc::ConcurrentMarkAndSweep>::Storage::Node;
    using NodeRef = typename kotlin::mm::ObjectFactory<kotlin::gc::ConcurrentMarkAndSweep>::NodeRef;
    Node& node = Node::FromData(ptr);
    NodeRef ref = NodeRef(node);
    auto& objectData = ref.ObjectData();
    bool reset = objectData.tryResetMark();
    CustomAllocDebug("TryResetMark(%p) = %d", ptr, reset);
    return reset;
}

using ObjectFactory = mm::ObjectFactory<gc::ConcurrentMarkAndSweep>;
using ExtraObjectsFactory = mm::ExtraObjectDataFactory;

static void KeepAlive(ObjHeader* baseObject) noexcept {
    auto& objectData = ObjectFactory::NodeRef::From(baseObject).ObjectData();
    objectData.tryMark();
}

bool TryFinalize(mm::ExtraObjectData* extraObject, AtomicStack<mm::ExtraObjectData>& finalizerQueue, size_t& finalizersScheduled) noexcept {
    if (extraObject->getFlag(mm::ExtraObjectData::FLAGS_FINALIZED)) {
        CustomAllocDebug("TryFinalize(%p): already finalized", extraObject);
        return true;
    }
    auto* baseObject = extraObject->GetBaseObject();
    if (!baseObject->heap()) {
        CustomAllocDebug("TryFinalize(%p): not a heap object", extraObject);
        return false;
    }
    if (extraObject->getFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE)) {
        CustomAllocDebug("TryFinalize(%p): already in finalizer queue, keep base object (%p) alive", extraObject, baseObject);
        KeepAlive(baseObject);
        return false;
    }
    extraObject->ClearWeakReferenceCounter();
    if (extraObject->HasAssociatedObject()) {
        extraObject->DetachAssociatedObject();
        extraObject->setFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE);
        finalizerQueue.Push(extraObject);
        finalizersScheduled++;
        KeepAlive(baseObject);
        CustomAllocDebug("TryFinalize(%p): add to finalizerQueue", extraObject);
        return false;
    } else {
        if (HasFinalizers(baseObject)) {
            extraObject->setFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE);
            finalizerQueue.Push(extraObject);
            finalizersScheduled++;
            KeepAlive(baseObject);
            CustomAllocDebug("TryFinalize(%p): addings to finalizerQueue, keep base object (%p) alive", extraObject, baseObject);
            return false;
        }
        extraObject->Uninstall();
        CustomAllocDebug("TryFinalize(%p): uninstalled extraObject", extraObject);
        return true;
    }
}

void* SafeAlloc(uint64_t size) noexcept {
    void* memory;
    if (size > std::numeric_limits<size_t>::max() || !(memory = std_support::malloc(size))) {
        konan::consoleErrorf("Out of memory trying to allocate %" PRIu64 "bytes. Aborting.\n", size);
        konan::abort();
    }
    return memory;
}

} // namespace kotlin::alloc
