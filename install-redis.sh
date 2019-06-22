#!/usr/bin/env bash
set -ex
REDIS_VERSION=5.0.5
wget http://download.redis.io/releases/redis-$REDIS_VERSION.tar.gz
tar -xzvf redis-$REDIS_VERSION.tar.gz
cd redis-$REDIS_VERSION && make
