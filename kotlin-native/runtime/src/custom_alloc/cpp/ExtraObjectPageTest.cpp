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
using Page = typename kotlin::alloc::ExtraObjectPage;
using Queue = typename kotlin::alloc::AtomicStack<Data>;

Data* alloc(Page* page) {
    Data* ptr = page->TryAllocate();
    if (ptr) {
        memset(ptr, 0, sizeof(Data));
    }
    return ptr;
}

TEST(CustomAllocTest, ExtraObjectPageConsequtiveAlloc) {
    Page* page = Page::Create();
    Data* prev = alloc(page);
    Data* cur;
    while ((cur = alloc(page))) {
        EXPECT_EQ(prev + 1, cur);
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
