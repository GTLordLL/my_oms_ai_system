package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder // 设计模式：建造者模式，方便链式赋值
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单详情表（商品快照）")
@TableName("oms_order_item")
public class OrderItem {
    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID（关联 oms_order.id） */
    @Schema(description = "关联订单主表ID")
    private Long orderId;

    /** SKU ID */
    @Schema(description = "关联商品SKU ID")
    private Long skuId;

    @Schema(description = "商家编码快照")
    private String skuCode;

    /** 购买数量 */
    @Schema(description = "销售数量")
    private Integer quantity;

    /** 单件售出价 */
    @Schema(description = "成交单价")
    private BigDecimal unitPrice;

    /** 下单时的成本快照 */
    @Schema(description = "下单时的成本快照（用于利润核算）")
    private BigDecimal unitCost;
}
