package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder // 设计模式：建造者模式，方便链式赋值
@NoArgsConstructor
@AllArgsConstructor
@TableName("oms_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 平台订单号（如淘宝订单号） */
    private String platformOrderSn;

    /** 来源平台：TAOBAO / JD / PDD 等 */
    private String sourcePlatform;

    /** 订单总额 */
    private BigDecimal totalAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 物流费成本 */
    private BigDecimal postFee;

    /** 订单状态：待发货、已发货、已完成、已退款 */
    private String status;

    /** 买家昵称 */
    private String buyerNick;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
