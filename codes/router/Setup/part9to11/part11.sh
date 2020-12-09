#!/bin/bash
echo "Enable part11"
set -v

ip netns exec PC1 ip l set lo up
ip netns exec PC1 systemctl restart bird
ip netns exec PC1 birdc restart all
ip netns exec PC1 birdc -s bird1.ctl enable part11
