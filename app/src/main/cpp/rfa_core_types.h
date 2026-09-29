#ifndef RAFAELIA_CORE_TYPES_H
#define RAFAELIA_CORE_TYPES_H

/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 * Research/evaluation use: see LICENSE_RESEARCH_COMMERCIAL.md.
 * Commercial use requires a separate written agreement with the rights holder.
 */

typedef signed char rfa_i8;
typedef unsigned char rfa_u8;
typedef signed short rfa_i16;
typedef unsigned short rfa_u16;
typedef signed int rfa_i32;
typedef unsigned int rfa_u32;
typedef signed long long rfa_i64;
typedef unsigned long long rfa_u64;

#if defined(__cplusplus)
static_assert(sizeof(rfa_i8) == 1, "requires 8-bit char");
static_assert(sizeof(rfa_i16) == 2, "requires 16-bit short");
static_assert(sizeof(rfa_i32) == 4, "requires 32-bit int");
static_assert(sizeof(rfa_i64) == 8, "requires 64-bit long long");
#else
_Static_assert(sizeof(rfa_i8) == 1, "requires 8-bit char");
_Static_assert(sizeof(rfa_i16) == 2, "requires 16-bit short");
_Static_assert(sizeof(rfa_i32) == 4, "requires 32-bit int");
_Static_assert(sizeof(rfa_i64) == 8, "requires 64-bit long long");
#endif

#endif
