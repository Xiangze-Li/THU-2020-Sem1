#!/bin/bash
set -v

ip netns exec PC2 ping 166.111.4.100
ip netns exec PC2 ping 101.6.4.100
