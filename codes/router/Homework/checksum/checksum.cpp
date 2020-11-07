#include <stdint.h>
#include <stdlib.h>

/**
 * @brief 进行 IP 头的校验和的验证
 * @param packet 完整的 IP 头和载荷
 * @param len 即 packet 的长度，单位是字节，保证包含完整的 IP 头
 * @return 校验和无误则返回 true ，有误则返回 false
 */
bool validateIPChecksum(uint8_t *packet, size_t len)
{
    int headLen = (packet[0] & 0x0F) * 4;
    int sum = 0;
    for (size_t i = 0; i < headLen; i++)
    {
        sum += packet[i];
        while (sum & ~0xFF)
        {
            sum = (sum >> 8) + (sum & 0xFF);
        }
    }
    return !(~(sum | 0xFFFFFF00));
}
