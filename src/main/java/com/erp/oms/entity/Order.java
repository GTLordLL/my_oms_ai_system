package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.oms.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "订单主表")
@TableName("oms_order")
public class Order {
    @Schema(description = "订单ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 平台订单号（如淘宝订单号） */
    @Schema(description = "平台订单号")
    private String platformOrderSn;

    /** 来源平台：TAOBAO / JD / PDD 等 */
    @Schema(description = "来源平台")
    private String sourcePlatform;

    /** 订单总额 */
    @Schema(description = "订单总额")
    private BigDecimal totalAmount;

    /** 实付金额 */
    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    /** 物流费成本 */
    @Schema(description = "运费")
    private BigDecimal postFee;

    /** 订单状态：待发货、已发货、已完成、已退款 售后中*/
    @Schema(description = "状态:0-待付款 1-待发货, 2-已发货, 3-已完成, 4-已关闭, 5-售后中")
    private OrderStatus status;

    /** 买家昵称 */
    @Schema(description = "买家昵称")
    private String buyerNick;

    @Schema(description = "收件人姓名")
    private String receiverName;

    @Schema(description = "收件人手机号")
    private String receiverMobile;

    @Schema(description = "详细地址")
    private String receiverAddress;

    @Schema(description = "快递单号")
    private String logisticsNo;

    @Schema(description = "发货时间")
    private LocalDateTime deliverTime;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "逻辑删除: 0-正常, 1-已删除", hidden = true)
    @TableLogic
    private Integer isDeleted;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
