package com.erp.oms.dto.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderQuery extends PageQuery{
    /**
     * 平台订单号（精确）
     */
    private String platformOrderSn;

    /**
     * 来源平台（TAOBAO / JD / PDD）
     */
    private String sourcePlatform;

    /**
     * 订单状态
     * 待发货 / 已发货 / 已完成 / 已退款
     */
    private String status;

    /**
     * 买家昵称（模糊）
     */
    private String buyerNick;

    /**
     * 订单关键字（平台订单号 / 买家昵称）
     */
    private String keyword;

    /**
     * 支付时间范围
     */
    private LocalDateTime payTimeStart;
    private LocalDateTime payTimeEnd;

    /**
     * 创建时间范围
     */
    private LocalDateTime createTimeStart;
    private LocalDateTime createTimeEnd;

    /**
     * 实付金额范围
     */
    private BigDecimal minPayAmount;
    private BigDecimal maxPayAmount;
}
