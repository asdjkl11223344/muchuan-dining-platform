package com.muchuan.event;

import com.muchuan.vo.OrderOperationLogVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单生命周期事件
 */
@Getter
@AllArgsConstructor
public class OrderLifecycleEvent {

    private final OrderOperationLogVO operationLog;

    private final Integer notifyType;

    private final String notifyContent;
}

