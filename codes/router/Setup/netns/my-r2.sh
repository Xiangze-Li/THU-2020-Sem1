#! /bin/bash

clear
make -C ../../Homework/router/r2/
ip netns exec R2 sh -c "echo 0 > /proc/sys/net/ipv4/conf/all/forwarding"
ip netns exec R2 ./../../Homework/router/r2/router
