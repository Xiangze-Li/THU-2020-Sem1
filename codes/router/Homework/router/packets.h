#pragma once
#include <cstdint>
#include <cstdlib>

struct IpHeader
{
    uint8_t ihl : 4, version : 4;
    uint8_t tos;
    uint16_t totLen;
    uint16_t id;
    uint16_t fragOff;
    uint8_t ttl;
    uint8_t protocol;
    uint16_t check;
    uint32_t srcAddr;
    uint32_t dstAddr;
} __attribute__((aligned(4)));

struct UdpHeader
{
    uint16_t src;
    uint16_t dst;
    uint16_t len;
    uint16_t chksum;
} __attribute__((aligned(4)));

struct RawRip
{
    uint8_t command; // 1(request) or 2(reponse)
    uint8_t version; // 2
    uint16_t zero;
    struct Entry
    {
        uint16_t family; // 0(request) or 2(response)
        uint16_t tag;    // 0
        uint32_t addr;
        uint32_t mask;
        uint32_t nexthop;
        uint32_t metric; // [1, 16]
    } entries[];
} __attribute__((aligned(4)));
