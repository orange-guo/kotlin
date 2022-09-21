/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_GCAPI_HPP_
#define CUSTOM_ALLOC_CPP_GCAPI_HPP_

#include <cstdint>
#include <inttypes.h>
#include <limits>
#include <stdlib.h>

#include "ExtraObjectData.hpp"

namespace kotlin::alloc {

bool TryResetMark(void* ptr) noexcept;
bool TryFinalize(mm::ExtraObjectData* extraObject, AtomicStack<mm::ExtraObjectData>& finalizerQueue, size_t& finalizersScheduled) noexcept;

void* SafeAlloc(uint64_t size) noexcept;

} // namespace kotlin::alloc

#endif
