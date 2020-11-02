#include "router.h"
#include <stdint.h>
#include <stdlib.h>
#include <cstdio>
#include <vector>
#include <algorithm>

std::vector<RoutingTableEntry> routeTable;

/**
 * @brief 插入/删除一条路由表表项
 * @param insert 如果要插入则为 true ，要删除则为 false
 * @param entry 要插入/删除的表项
 *
 * 插入时如果已经存在一条 addr 和 len 都相同的表项，则替换掉原有的。
 * 删除时按照 addr 和 len **精确** 匹配。
 */
void update(bool insert, RoutingTableEntry entry)
{
    // TODO:
    if (insert)
    {
        bool found = false;
        for (RoutingTableEntry &e : routeTable)
        {
            if (e.addr == entry.addr && e.len == entry.len)
            {
                e = entry;
                found = true;
                break;
            }
        }
        if (!found)
        {
            routeTable.push_back(entry);
        }
    }
    else // !insert == delete
    {
        for (size_t i = 0; i < routeTable.size(); i++)
        {
            RoutingTableEntry const &e = routeTable[i];
            if (e.addr == entry.addr && e.len == entry.len)
            {
                routeTable.erase(routeTable.begin() + i);
                break;
            }
        }
    }
}

/**
 * @brief 进行一次路由表的查询，按照最长前缀匹配原则
 * @param addr 需要查询的目标地址，网络字节序
 * @param nexthop 如果查询到目标，把表项的 nexthop 写入
 * @param if_index 如果查询到目标，把表项的 if_index 写入
 * @return 查到则返回 true ，没查到则返回 false
 */
bool prefix_query(uint32_t addr, uint32_t *nexthop, uint32_t *if_index)
{
    // TODO:
    // fprintf(stderr, ">>>>\tQuerying 0x%08x\n", addr);

    *nexthop = 0;
    *if_index = 0;

    const RoutingTableEntry *foundEntry = nullptr;

    for (const RoutingTableEntry &e : routeTable)
    {
        if (e.len==0 && (!foundEntry || foundEntry->len <= 0)){
            foundEntry = &e;
            continue;
        }

        uint32_t mask = ((uint32_t)-1 >> (32 - e.len));

        // fprintf(stderr, "    \tRoute table entry 0x%08x %d; mask 0x%08x\n", e.addr, e.len, mask);
        // fprintf(stderr, "    \te.addr ^ addr == %08x\n", e.addr ^ addr);
        if (!(mask & (e.addr ^ addr)))
        {
            // fprintf(stderr, "    \tHit.\n");
            if (!foundEntry || foundEntry->len <= e.len)
            {
                foundEntry = &e;
                // fprintf(stderr, "    \tUpdate.\n");
            }
        }
    }

    if (!foundEntry)
        return false;
    else
    {
        *nexthop = foundEntry->nexthop;
        *if_index = foundEntry->if_index;
        return true;
    }
}
