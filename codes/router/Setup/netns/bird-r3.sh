#!/bin/bash

ip netns exec R3 sh -c "echo 1 > /proc/sys/net/ipv4/conf/all/forwarding"
ip netns exec R3 bird -c bird-r3-v2.conf -d -s bird-r3.ctl
