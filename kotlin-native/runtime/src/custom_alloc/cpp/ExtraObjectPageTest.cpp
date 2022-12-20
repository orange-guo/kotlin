/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstddef>
#include <cstdint>

#include "AtomicStack.hpp"
#include "CustomAllocConstants.hpp"
#include "ExtraObjectData.hpp"
#include "ExtraObjectPage.hpp"
#include "gtest/gtest.h"
#include "TypeInfo.h"

namespace {

using Data = typename kotlin::mm::ExtraObjectData;
using Cell = typename kotlin::alloc::ExtraObjectCell;
using Page = typename kotlin::alloc::ExtraObjectPage;
using Queue = typename kotlin::alloc::AtomicStack<Cell>;

Data* alloc(Page* page) {
    Data* ptr = page->TryAllocate();
    if (ptr) {
        memset(ptr, 0, sizeof(Data));
    }
    return ptr;
}

TEST(CustomAllocTest, ExtraObjectPageConsequtiveAlloc) {
    Page* page = Page::Create();
    uint8_t* prev = reinterpret_cast<uint8_t*>(alloc(page));
    uint8_t* cur;
    while ((cur = reinterpret_cast<uint8_t*>(alloc(page)))) {
        EXPECT_EQ(prev + sizeof(Cell), cur);
        prev = cur;
    }
    free(page);
}

TEST(CustomAllocTest, ExtraObjectPageSweepEmptyPage) {
    Page* page = Page::Create();
    Queue finalizerQueue;
    size_t scheduled = 0;
    EXPECT_FALSE(page->Sweep(finalizerQueue, scheduled));
    EXPECT_EQ(scheduled, size_t(0));
    free(page);
}

TEST(CustomAllocTest, ExtraObjectPageSweepFullFinalizedPage) {
    Page* page = Page::Create();
    int count = 0;
    Data* ptr;
    while ((ptr = alloc(page))) {
        ptr->setFlag(Data::FLAGS_FINALIZED);
        ++count;
    }
    EXPECT_EQ(count, EXTRA_OBJECT_COUNT);
    Queue finalizerQueue;
    size_t scheduled = 0;
    EXPECT_FALSE(page->Sweep(finalizerQueue, scheduled));
    EXPECT_EQ(scheduled, size_t(0));
    free(page);
}

} // namespace
