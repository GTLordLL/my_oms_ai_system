package com.erp.oms.dto.viewObject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "订单详情显示对象")
public class OrderItemVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联订单ID")
    private Long orderId;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "商家编码快照")
    private String skuCode;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "成交单价")
    private BigDecimal unitPrice;

    @Schema(description = "下单时的成本快照")
    private BigDecimal unitCost;

    @Schema(description = "成交总额（单价 * 数量）")
    private BigDecimal totalPrice;

    @Schema(description = "预估利润（(单价 - 成本) * 数量）")
    private BigDecimal estimatedProfit;
}