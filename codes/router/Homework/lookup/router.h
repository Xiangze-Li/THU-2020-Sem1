#include <stdint.h>
#include <utility>
#include <map>

/*
  表示路由表的一项。
  约定 addr 和 nexthop 以 **网络字节序（大端序）** 存储。
  这意味着 1.2.3.4 在小端序的机器上被解释为整数 0x04030201 而不是 0x01020304。
  保证 addr 和 len 构成合法的网络前缀。
  当 nexthop 为零时这是一条直连路由。
  你可以在全局变量中把路由表以一定的数据结构格式保存下来。
*/
typedef struct
{
  uint32_t addr;     // 网络字节序，IPv4 地址
  uint32_t len;      // 主机字节序，前缀长度
  uint32_t if_index; // 主机字节序，出端口编号
  uint32_t nexthop;  // 网络字节序，下一跳的 IPv4 地址
                     // 为了实现 RIP 协议，需要在这里添加额外的字段
  uint32_t metric;   // 主机序
} RoutingTableEntry;

using RouterKey = std::pair<uint32_t, uint32_t>;

extern std::map<RouterKey, RoutingTableEntry> routerTable;

const uint32_t MASK_BE[33] =
    {0x00000000,
     0x00000080, 0x000000c0, 0x000000e0, 0x000000f0, 0x000000f8, 0x000000fc, 0x000000fe, 0x000000ff,
     0x000080ff, 0x0000c0ff, 0x0000e0ff, 0x0000f0ff, 0x0000f8ff, 0x0000fcff, 0x0000feff, 0x0000ffff,
     0x0080ffff, 0x00c0ffff, 0x00e0ffff, 0x00f0ffff, 0x00f8ffff, 0x00fcffff, 0x00feffff, 0x00ffffff,
     0x80ffffff, 0xc0ffffff, 0xe0ffffff, 0xf0ffffff, 0xf8ffffff, 0xfcffffff, 0xfeffffff, 0xffffffff};
