package com.muchuan.service.support;

import com.alibaba.fastjson.JSON;
import com.muchuan.vo.OrderOperationLogVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单时间线存储服务
 */
@Service
public class OrderTimelineService {

    private static final String ORDER_TIMELINE_KEY_PREFIX = "order:timeline:";

    @Value("${muchuan.order.timeline.ttl-days:30}")
    private long timelineTtlDays;

    @Value("${muchuan.order.timeline.max-size:100}")
    private long timelineMaxSize;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void append(OrderOperationLogVO operationLogVO) {
        String key = buildKey(operationLogVO.getOrderId());
        stringRedisTemplate.opsForList().rightPush(key, JSON.toJSONString(operationLogVO));

        Long size = stringRedisTemplate.opsForList().size(key);
        if (size != null && size > timelineMaxSize) {
            long start = size - timelineMaxSize;
            stringRedisTemplate.opsForList().trim(key, start, size - 1);
        }

        stringRedisTemplate.expire(key, Duration.ofDays(timelineTtlDays));
    }

    public List<OrderOperationLogVO> listByOrderId(Long orderId) {
        List<String> payloadList = stringRedisTemplate.opsForList().range(buildKey(orderId), 0, -1);
        if (CollectionUtils.isEmpty(payloadList)) {
            return Collections.emptyList();
        }
        return payloadList.stream()
                .map(payload -> JSON.parseObject(payload, OrderOperationLogVO.class))
                .collect(Collectors.toList());
    }

    private String buildKey(Long orderId) {
        return ORDER_TIMELINE_KEY_PREFIX + orderId;
    }
}

