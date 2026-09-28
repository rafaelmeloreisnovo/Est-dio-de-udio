#ifndef RAFAELIA_CORE_TYPES_H
#define RAFAELIA_CORE_TYPES_H

typedef signed short rfa_i16;
typedef signed int rfa_i32;
typedef unsigned int rfa_u32;
typedef signed long long rfa_i64;
typedef unsigned long long rfa_u64;

#if defined(__cplusplus)
static_assert(sizeof(rfa_i16) == 2, "requires 16-bit short");
static_assert(sizeof(rfa_i32) == 4, "requires 32-bit int");
static_assert(sizeof(rfa_i64) == 8, "requires 64-bit long long");
#else
_Static_assert(sizeof(rfa_i16) == 2, "requires 16-bit short");
_Static_assert(sizeof(rfa_i32) == 4, "requires 32-bit int");
_Static_assert(sizeof(rfa_i64) == 8, "requires 64-bit long long");
#endif

#endif
