#! /bin/bash

make -C ../../Homework/router/r3/
ip netns exec R3 sh -c "echo 0 > /proc/sys/net/ipv4/conf/all/forwarding"
ip netns exec R3 ./../../Homework/router/r3/router
