#!/bin/bash

ip netns exec R1 sh -c "echo 1 > /proc/sys/net/ipv4/conf/all/forwarding"
ip netns exec R1 bird -c bird-r1-v2.conf -d -s bird-r1.ctl
