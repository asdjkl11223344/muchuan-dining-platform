package com.muchuan.service.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 基于 Redis 自增序列的订单号生成器
 */
@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ORDER_NUMBER_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ORDER_NUMBER_KEY_PREFIX = "order:number:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public String nextNumber() {
        LocalDateTime now = LocalDateTime.now();
        String counterKey = ORDER_NUMBER_KEY_PREFIX + now.format(DATE_KEY_FORMATTER);
        Long sequence = stringRedisTemplate.opsForValue().increment(counterKey);
        if (sequence == null) {
            throw new IllegalStateException("生成订单号失败");
        }
        if (sequence == 1L) {
            stringRedisTemplate.expire(counterKey, Duration.ofDays(2));
        }
        return now.format(ORDER_NUMBER_FORMATTER) + String.format("%06d", sequence);
    }
}

