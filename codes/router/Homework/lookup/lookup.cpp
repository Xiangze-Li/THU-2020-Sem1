#include "router.h"
#include <stdint.h>
#include <stdlib.h>
#include <cstdio>
#include <algorithm>

std::map<RouterKey, RoutingTableEntry> routerTable;

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
    RouterKey key = {entry.addr, entry.len};
    if (insert)
        routerTable[key] = entry;
    else // !insert == delete
    {
        auto found = routerTable.find(key);
        if (found != routerTable.end())
            routerTable.erase(found);
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
    static auto genMaskedAddr = [](uint32_t addr, uint32_t maskLen) -> uint32_t {
        return addr & MASK_BE[maskLen];
    };

    // fprintf(stderr, "Target : 0x%08x\n", addr);
    // fprintf(stderr, "RoutingTable : \n");
    // for (const auto & e:routerTable){
    //     fprintf(stderr, "    0x%08x, %d : 0x%08x, %d\n", e.first.first, e.first.second, e.second.nexthop, e.second.if_index);
    // }

    for (int len = 32; len >= 0; len--)
    {
        uint32_t masked = genMaskedAddr(addr, len);
        auto found = routerTable.find(RouterKey(masked, len));
        if (found != routerTable.end())
        {
            *if_index = found->second.if_index;
            *nexthop = found->second.nexthop;
            return true;
        }
    }
    *nexthop = 0;
    *if_index = 0;
    return false;
}
