#!/bin/bash

ip netns exec PC1 sh -c "echo 1 > /proc/sys/net/ipv4/conf/all/forwarding"
ip netns exec PC1 bird -c bird1-v2.conf -s bird1.ctl -d
