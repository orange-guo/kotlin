/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_EXTRA_OBJECTPAGE_HPP_
#define CUSTOM_ALLOC_CPP_EXTRA_OBJECTPAGE_HPP_

#include <atomic>
#include <cstdint>

#include "AtomicStack.hpp"
#include "ExtraObjectData.hpp"

namespace kotlin::alloc {

union ExtraObjectCell {
    mm::ExtraObjectData* Data() { return reinterpret_cast<mm::ExtraObjectData*>(this); }

    ExtraObjectCell* nextFree;
    uint8_t _[sizeof(mm::ExtraObjectData)]; //  unused, lets cell have size of ExtraObjectData
};

static_assert(sizeof(ExtraObjectCell) == sizeof(mm::ExtraObjectData), "Cell has wrong size");

class alignas(8) ExtraObjectPage {
public:
    static ExtraObjectPage* Create() noexcept;

    void Destroy() noexcept;

    // Tries to allocate in current page, returns null if no free block in page
    mm::ExtraObjectData* TryAllocate() noexcept;

    bool Sweep(AtomicStack<mm::ExtraObjectData>& finalizerQueue, size_t& finalizersScheduled) noexcept;

private:
    friend class AtomicStack<ExtraObjectPage>;

    ExtraObjectPage() noexcept;

    // Used for linking pages together in `pages` queue or in `unswept` queue.
    ExtraObjectPage* next_;
    ExtraObjectCell* nextFree_;
    ExtraObjectCell cells_[];
};

} // namespace kotlin::alloc

#endif
