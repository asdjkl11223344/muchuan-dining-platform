package com.muchuan.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单操作时间线
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderOperationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;

    private String orderNumber;

    private String action;

    private Integer beforeStatus;

    private String beforeStatusDesc;

    private Integer afterStatus;

    private String afterStatusDesc;

    private String operatorType;

    private Long operatorId;

    private String operatorLabel;

    private String remark;

    private LocalDateTime operateTime;
}

