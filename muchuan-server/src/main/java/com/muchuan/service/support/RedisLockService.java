package com.muchuan.service.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

/**
 * 基于 Redis 的轻量分布式锁
 */
@Service
public class RedisLockService {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>();

    static {
        UNLOCK_SCRIPT.setScriptText("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public boolean tryLock(String key, String value, Duration ttl) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(success);
    }

    public void unlock(String key, String value) {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), value);
    }
}

