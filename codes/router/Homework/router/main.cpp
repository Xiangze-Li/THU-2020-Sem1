#include "rip.h"
#include "router.h"
#include "router_hal.h"
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <algorithm>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#include <netinet/udp.h>

#define DEBUG_OUTPUT 0

const in_addr_t RIP_MULTICAST_ADDR = 0x090000e0u;
const macaddr_t RIP_MULTICAST_MAC = {0x01, 0x00, 0x5e, 0x00, 0x00, 0x09};

#if DEBUG_OUTPUT
void printRouterTable()
{
    auto size = routerTable.size();
    fprintf(stderr, "Routing Table: size=%ld\n", size);
    for (const auto &e : routerTable)
    {
        const auto &ee = e.second;
        fprintf(stderr, "    Addr: 0x%08x, MaskLen: %02d, Nexthop: 0x%08x, Interface: %d, Metric: %d\n",
                ee.addr, ee.len, ee.nexthop, ee.if_index, ee.metric);
    }
}
void printPacket(uint32_t len, uint8_t *packet)
{
    // fprintf(stderr, " 1 0 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1\n");
    for (int i = 0; i < len; i++)
    {
        fprintf(stderr, "%02x ", packet[i]);
        if (!((i + 1) % 16))
            fprintf(stderr, "\n");
    }
    fprintf(stderr, "\n");
}
#endif

uint8_t packet[2048];
uint8_t output[2048];

// for online experiment, don't change
#ifdef ROUTER_R1
// 0: 192.168.1.1
// 1: 192.168.3.1
// 2: 192.168.6.1
// 3: 192.168.7.1
const in_addr_t addrs[N_IFACE_ON_BOARD] = {0x0101a8c0, 0x0103a8c0, 0x0106a8c0,
                                           0x0107a8c0};
#elif defined(ROUTER_R2)
// 0: 192.168.3.2
// 1: 192.168.4.1
// 2: 192.168.8.1
// 3: 192.168.9.1
const in_addr_t addrs[N_IFACE_ON_BOARD] = {0x0203a8c0, 0x0104a8c0, 0x0108a8c0,
                                           0x0109a8c0};
#elif defined(ROUTER_R3)
// 0: 192.168.4.2
// 1: 192.168.5.2
// 2: 192.168.10.1
// 3: 192.168.11.1
const in_addr_t addrs[N_IFACE_ON_BOARD] = {0x0204a8c0, 0x0205a8c0, 0x010aa8c0,
                                           0x010ba8c0};
#else

// 自己调试用，你可以按需进行修改，注意字节序
// 0: 10.0.0.1
// 1: 10.0.1.1
// 2: 10.0.2.1
// 3: 10.0.3.1
in_addr_t addrs[N_IFACE_ON_BOARD] = {0x0100000a, 0x0101000a, 0x0102000a,
                                     0x0103000a};
#endif

extern bool validateIPChecksum(uint8_t *packet, size_t len);
extern void update(bool insert, RoutingTableEntry entry);
extern bool prefix_query(uint32_t addr, uint32_t *nexthop, uint32_t *if_index);
extern bool forward(uint8_t *packet, size_t len);
extern bool disassemble(const uint8_t *packet, uint32_t len, RipPacket *output);
extern uint32_t assemble(const RipPacket *rip, uint8_t *buffer);

uint16_t calChecksum(uint16_t *packet, size_t numBytes);
void calIpChksum(iphdr *ipHdr);
void calIcmpChksum(icmp *icmpMsg, size_t headerLen);

void sendRipResp(const macaddr_t &dstMac, uint32_t interface = -1u, uint8_t *packet = packet);
void sendIcmp(uint8_t icmpType, uint8_t icmpCode, uint32_t interface, in_addr_t srcAddr, const macaddr_t &srcMac);

int main(int argc, char *argv[])
{
    // 0a.
    int res = HAL_Init(1, addrs);
    if (res < 0)
    {
        return res;
    }

    // 0b. Add direct routes
    // For example:
    // 10.0.0.0/24 if 0
    // 10.0.1.0/24 if 1
    // 10.0.2.0/24 if 2
    // 10.0.3.0/24 if 3
    for (uint32_t i = 0; i < N_IFACE_ON_BOARD; i++)
    {
        RoutingTableEntry entry = {
            .addr = addrs[i] & 0x00FFFFFF, // network byte order
            .len = 24,                     // host byte order
            .if_index = i,                 // host byte order
            .nexthop = 0,                  // network byte order, means direct
            .metric = 1,
        };
        update(true, entry);
    }

    uint64_t last_time = 0;
    while (1)
    {
        uint64_t time = HAL_GetTicks();
        // the RFC says 30s interval,
        // but for faster convergence, use 5s here
        if (time > last_time + 5 * 1000)
        {
            // ref. RFC 2453 Section 3.8
            // HINT: print complete routing table to stdout/stderr for debugging
            // - TODO: send complete routing table to every interface

#if DEBUG_OUTPUT
            // printf("5s Timer\n");
            fprintf(stderr, "=========================================================\n");
            fprintf(stderr, "5s Timer\n");
            printRouterTable();
#endif

            sendRipResp(RIP_MULTICAST_MAC);

            last_time = time;
        }

        int mask = (1 << N_IFACE_ON_BOARD) - 1;
        macaddr_t src_mac;
        macaddr_t dst_mac;
        int if_index;
        res = HAL_ReceiveIPPacket(mask, packet, sizeof(packet), src_mac, dst_mac,
                                  1000, &if_index);
        if (res == HAL_ERR_EOF)
        {
            break;
        }
        else if (res < 0)
        {
            return res;
        }
        else if (res == 0)
        {
            // Timeout
            continue;
        }
        else if (res > sizeof(packet))
        {
            // packet is truncated, ignore it
            continue;
        }

#if DEBUG_OUTPUT
        fprintf(stderr, "=========================================================\n");
        fprintf(stderr, "Packet received\n");
#endif

        // 1. validate
        if (!validateIPChecksum(packet, res))
        {
#if DEBUG_OUTPUT
            // printf("Invalid IP Checksum\n");
            fprintf(stderr, "-   Invalid IP Checksum\n");
#endif
            // drop if ip checksum invalid
            continue;
        }

        iphdr *ipHdr = (iphdr *)packet;
        in_addr_t src_addr = ipHdr->saddr;
        in_addr_t dst_addr = ipHdr->daddr;
// - TODO: extract src_addr and dst_addr from packet (big endian)
#if DEBUG_OUTPUT
        fprintf(stderr, "-   src_addr: 0x%08x, dst_addr: 0x%08x\n", src_addr, dst_addr);
#endif

        // 2. check whether dst is me
        // - TODO: handle rip multicast address(224.0.0.9)
        bool dst_is_me = dst_addr == RIP_MULTICAST_ADDR;
        for (int i = 0; !dst_is_me && i < N_IFACE_ON_BOARD; i++)
        {
            if (memcmp(&dst_addr, &addrs[i], sizeof(in_addr_t)) == 0)
            {
                dst_is_me = true;
                break;
            }
        }
#if DEBUG_OUTPUT
        fprintf(stderr, "-   dst_is_me: %d\n", dst_is_me);
#endif

        if (dst_is_me)
        {
            // 3a.1
            RipPacket rip;
            // check and validate
            if (disassemble(packet, res, &rip))
            {
#if DEBUG_OUTPUT
                fprintf(stderr, "-   is RIP packet\n");
#endif

                if (rip.command == 1)
                {
#if DEBUG_OUTPUT
                    fprintf(stderr, "-   is RIP request\n");
#endif

                    // 3a.3 request, ref. RFC 2453 Section 3.9.1
                    // only need to respond to whole table requests in the lab
                    // - TODO: fill resp
                    // implement split horizon with poisoned reverse
                    // ref. RFC 2453 Section 3.4.3
                    if (rip.numEntries == 1 && rip.entries[0].metric == htonl(16u))
                    {
#if DEBUG_OUTPUT
                        fprintf(stderr, "-   sending RIP response\n");
#endif
                        sendRipResp(src_mac, if_index, output);
                    }
#if DEBUG_OUTPUT
                    else
                        fprintf(stderr, "-   not sending RIP resp\n");
#endif
                }
                else
                {
#if DEBUG_OUTPUT
                    fprintf(stderr, "-   is RIP resp\n");
#endif
                    static auto popcount = [](uint32_t mask) -> uint32_t {
                        uint32_t cntr = 0;
                        while (mask)
                        {
                            cntr += mask & 0x1u;
                            mask >>= 1;
                        }
                        return cntr;
                    };
                    // 3a.2 response, ref. RFC 2453 Section 3.9.2
                    // - TODO: update routing table
                    // update metric, if_index, nexthop
                    // HINT: handle nexthop = 0 case
                    // optional: triggered updates ref. RFC 2453 Section 3.10.1
                    bool updated = false;
                    for (size_t i = 0; i < rip.numEntries; i++)
                    {

                        const auto &e = rip.entries[i];
                        uint32_t metric = std::min(ntohl(e.metric) + 1, 16u);
                        uint32_t len = popcount(e.mask);
                        uint32_t nexthop = e.nexthop;
                        if (nexthop == 0)
                            nexthop = src_addr;
                        RouterKey key = RouterKey(e.addr, len);

#if DEBUG_OUTPUT
                        fprintf(stderr, "-   HANDLING: Addr: 0x%08x, MaskLen: %02d, Nexthop: 0x%08x, Interface: %d, Metric: %d ",
                                e.addr, len, nexthop, if_index, metric);
#endif
                        auto found = routerTable.find(key);
                        if (found != routerTable.end())
                        {
#if DEBUG_OUTPUT
                            fprintf(stderr, "> FOUND ");
#endif
                            auto &entry = found->second;
                            if ((src_addr == entry.nexthop && metric != entry.metric) || metric < entry.metric)
                            {
                                if (metric == 16u)
                                {
#if DEBUG_OUTPUT
                                    fprintf(stderr, "> ERASED\n");
#endif
                                    routerTable.erase(found);
                                }
                                else
                                {
#if DEBUG_OUTPUT
                                    fprintf(stderr, "> UPDATED\n");
#endif
                                    entry.nexthop = nexthop;
                                    entry.metric = metric;
                                }
                                updated = true;
                            }
#if DEBUG_OUTPUT
                            else
                                fprintf(stderr, "> DROPED\n");
#endif
                        }
                        else
                        {
#if DEBUG_OUTPUT
                            fprintf(stderr, "> NOT FOUND\n");
#endif
                            if (metric != 16u)
                            {
#if DEBUG_OUTPUT
                                fprintf(stderr, "> INSERTED\n");
#endif
                                routerTable[key] = {
                                    .addr = e.addr,
                                    .len = len,
                                    .if_index = if_index,
                                    .nexthop = nexthop,
                                    .metric = metric,
                                };
                                updated = true;
                            }
#if DEBUG_OUTPUT
                            else
                                fprintf(stderr, "> DROPED\n");
#endif
                        }
                    }
#if DEBUG_OUTPUT
                    if (updated)
                    {
                        fprintf(stderr, "-   Router Table Updated\n");
                        printRouterTable();
                    }
                    else
                    {
                        fprintf(stderr, "-   Router Table NOT Updated\n");
                    }
#endif
                }
            }
            else
            {
#if DEBUG_OUTPUT
                fprintf(stderr, "-   not RIP packet\n");
#endif

                // not a rip packet
                // handle icmp echo request packet
                // - TODO: how to determine?
                if (ipHdr->protocol == IPPROTO_ICMP)
                {
#if DEBUG_OUTPUT
                    fprintf(stderr, "-   is ICMP packet");
#endif

                    icmp *icmpMsg = (icmp *)(packet + 20);
                    if (icmpMsg->icmp_code == ICMP_ECHO)
                    {
                        iphdr *ipHdrR = (iphdr *)output;
                        memcpy(ipHdrR, ipHdr, sizeof(iphdr));
                        icmp *icmpMsgR = (icmp *)(output + 20);
                        memcpy(icmpMsgR, icmpMsg, sizeof(icmp));

                        std::swap(ipHdrR->saddr, ipHdrR->daddr);
                        icmpMsgR->icmp_type = ICMP_ECHOREPLY;
                        ipHdrR->ttl = 64;
                        calIpChksum(ipHdrR);
                        auto totLen = ntohl(ipHdrR->tot_len);
                        calIcmpChksum(icmpMsgR, totLen - 20);
                        HAL_SendIPPacket(if_index, output, totLen, src_mac);
#if DEBUG_OUTPUT
                        fprintf(stderr, "-   sending ICMP echo reply");
#endif
                    }
                }
            }
        }
        else
        {
            // 3b.1 dst is not me
            // check ttl
            uint8_t ttl = packet[8];
            if (ttl <= 1u)
            {
                // send icmp time to live exceeded to src addr
                sendIcmp(ICMP_TIME_EXCEEDED, 0, if_index, src_addr, src_mac);
#if DEBUG_OUTPUT
                fprintf(stderr, "-   sending ICMP time exceeded\n");
#endif
            }
            else
            {
                // forward
                // beware of endianness
                uint32_t nexthop, dest_if;
                if (prefix_query(dst_addr, &nexthop, &dest_if))
                {
                    // found
                    macaddr_t dest_mac;
                    // direct routing
                    if (nexthop == 0)
                    {
                        nexthop = dst_addr;
                    }
                    if (HAL_ArpGetMacAddress(dest_if, nexthop, dest_mac) == 0)
                    {
                        // found
                        memcpy(output, packet, res);
                        // update ttl and checksum
                        forward(output, res);
                        HAL_SendIPPacket(dest_if, output, res, dest_mac);
#if DEBUG_OUTPUT
                        fprintf(stderr, "-   forwarding\n");
#endif
                    }
                    else
                    {
                        // not found
                        // you can drop it

                        // printf("ARP not found for nexthop %x\n", nexthop);
                        fprintf(stderr, "-   ARP not found for nexthop %x\n", nexthop);
                    }
                }
                else
                {
                    // not found
                    // send ICMP Destination Network Unreachable
#if DEBUG_OUTPUT
                    // printf("IP not found in routing table for src %x dst %x\n", src_addr, dst_addr);
                    fprintf(stderr, "-   IP not found in routing table for src %x dst %x\n", src_addr, dst_addr);
#endif

                    // send icmp destination net unreachable to src addr
                    sendIcmp(ICMP_DEST_UNREACH, ICMP_NET_UNREACH, if_index, src_addr, src_mac);
                }
            }
        }
    }
    return 0;
}

uint16_t calChecksum(uint16_t *packet, size_t numBytes)
{
    uint32_t sum = 0;
    numBytes >>= 1;
    for (size_t i = 0; i < numBytes; i++)
    {
        sum += packet[i];
    }
    while (sum & ~0xFFFFu)
    {
        sum = (sum & 0xFFFFu) + (sum >> 16);
    }
    return ~sum;
}

void calIpChksum(iphdr *ipHdr)
{
    ipHdr->check = 0;
    ipHdr->check = calChecksum((uint16_t *)(ipHdr), 4 * ipHdr->ihl);
}

void calIcmpChksum(icmp *icmpMsg, size_t headerLen)
{
    icmpMsg->icmp_cksum = 0;
    icmpMsg->icmp_cksum = calChecksum((uint16_t *)(icmpMsg), headerLen);
}

void sendRipResp(const macaddr_t &dstMac, uint32_t interface, uint8_t *packet)
{
    iphdr *ipHdr = (iphdr *)packet;
    *ipHdr = iphdr{
        .ihl = 5,
        .version = 4,
        .tos = 0,
        .tot_len = 0, //
        .id = 0,
        .frag_off = 0,
        .ttl = 1,
        .protocol = IPPROTO_UDP,
        .check = 0, //
        .saddr = 0, //
        .daddr = RIP_MULTICAST_ADDR,
    };

    udphdr *udpHdr = (udphdr *)(packet + 20);
    *udpHdr = udphdr{
        htons(520),
        htons(520),
        0, //
        0,
    };

    RipPacket rip;
    rip.command = 2;

    static auto send = [&rip, &ipHdr, &udpHdr, &packet, &dstMac](const int &if_index, const int &cntr) {
        rip.numEntries = cntr;
        uint16_t totLen = (uint16_t)assemble(&rip, packet + 28) + 20 + 8;
        ipHdr->tot_len = htons(totLen);
        calIpChksum(ipHdr);
        udpHdr->len = htons((totLen - 20));
        // if (if_index == 1)
        //     printPacket(totLen, packet);
        HAL_SendIPPacket(if_index, packet, totLen, dstMac);
    };

    for (size_t i = 0; i < N_IFACE_ON_BOARD; i++)
    {
        if (interface != -1u && interface != i)
            continue;

        ipHdr->saddr = addrs[i];
        size_t cntr = 0;
        for (const auto &e : routerTable)
        {
            auto &r = e.second;
            rip.entries[cntr] = {
                .addr = r.addr,
                .mask = MASK_BE[r.len],
                .nexthop = r.nexthop,
                .metric = htonl(r.if_index == i ? 16u : r.metric)};
            cntr++;
            if (cntr >= RIP_MAX_ENTRY)
            {
                send(i, cntr);
                cntr = 0;
            }
        }
        if (cntr)
            send(i, cntr);
    }
}

void sendIcmp(uint8_t icmpType, uint8_t icmpCode, uint32_t interface, in_addr_t srcAddr, const macaddr_t &srcMac)
{
    memcpy(output + 28, packet, 28 * sizeof(uint8_t));
    uint16_t totLen = 56; // 20 (ip) + 8 (icmp) + original payload (20 + 8)
    iphdr *ipHdr = (iphdr *)output;
    *ipHdr = iphdr{
        .ihl = 5,
        .version = 4,
        .tos = 0,
        .tot_len = htons(totLen),
        .id = 0,
        .frag_off = 0,
        .ttl = 64,
        .protocol = IPPROTO_ICMP,
        .check = 0,
        .saddr = addrs[interface],
        .daddr = srcAddr,
    };
    calIpChksum(ipHdr);
    icmphdr *icmpHdr = (icmphdr *)(output + 20);
    icmpHdr->type = icmpType;
    icmpHdr->code = icmpCode;
    icmpHdr->un.gateway = 0;
    calIcmpChksum((icmp *)icmpHdr, totLen - 20);
    HAL_SendIPPacket(interface, output, totLen, srcMac);
}
