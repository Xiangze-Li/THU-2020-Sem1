#! /bin/bash

make -C ../../Homework/router/r1/
ip netns exec R1 sh -c "echo 0 > /proc/sys/net/ipv4/conf/all/forwarding"
ip netns exec R1 ./../../Homework/router/r1/router
