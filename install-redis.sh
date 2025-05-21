#!/usr/bin/env bash
set -ex
REDIS_VERSION="8.0.1"
wget https://download.redis.io/releases/redis-${REDIS_VERSION}.tar.gz
tar -xzvf redis-${REDIS_VERSION}.tar.gz
cd redis-${REDIS_VERSION} && make
