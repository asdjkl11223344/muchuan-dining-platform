package com.muchuan.listener;

import com.alibaba.fastjson.JSON;
import com.muchuan.event.OrderLifecycleEvent;
import com.muchuan.service.support.OrderTimelineService;
import com.muchuan.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单生命周期监听器
 */
@Component
@Slf4j
public class OrderLifecycleEventListener {

    @Autowired
    private OrderTimelineService orderTimelineService;
    @Autowired
    private WebSocketServer webSocketServer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(OrderLifecycleEvent event) {
        orderTimelineService.append(event.getOperationLog());

        if (event.getNotifyType() == null) {
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("type", event.getNotifyType());
        message.put("orderId", event.getOperationLog().getOrderId());
        message.put("content", event.getNotifyContent());

        String payload = JSON.toJSONString(message);
        log.info("推送订单生命周期通知：{}", payload);
        webSocketServer.sendToAllClient(payload);
    }
}

