#include <stdint.h>
#include <stdlib.h>

// 在 checksum.cpp 中定义
extern bool validateIPChecksum(uint8_t *packet, size_t len);

/**
 * @brief 进行转发时所需的 IP 头的更新：
 *        你需要先检查 IP 头校验和的正确性，如果不正确，直接返回 false ；
 *        如果正确，请更新 TTL 和 IP 头校验和，并返回 true 。
 *        你可以调用 checksum 题中的 validateIPChecksum 函数，
 *        编译的时候会链接进来。
 * @param packet 收到的 IP 包，既是输入也是输出，原地更改
 * @param len 即 packet 的长度，单位为字节
 * @return 校验和无误则返回 true ，有误则返回 false
 */
bool forward(uint8_t *packet, size_t len)
{
    if (!validateIPChecksum(packet, len))
        return false;

    uint16_t oldTTLProto = (uint16_t)(packet[8] << 8) + (uint16_t)(packet[9]);
    uint16_t oldChecksum = (uint16_t)(packet[10] << 8) + (uint16_t)(packet[11]);
    uint16_t newTTLProto = 0;
    uint32_t newChecksum = 0;

    packet[8] -= 1;
    newTTLProto = (uint16_t)(packet[8] << 8) + (uint16_t)(packet[9]);
    newChecksum = (~oldChecksum & 0xFFFFu) + (~oldTTLProto & 0xFFFFu) + newTTLProto;

    while (newChecksum > 0xFFFFu)
    {
        newChecksum = (newChecksum >> 16) + (newChecksum & 0xFFFFu);
    }
    newChecksum = ~newChecksum;

    packet[11] = newChecksum & 0xFFu;
    packet[10] = (newChecksum >> 8) & 0xFFu;

    return true;
}
