package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder // 设计模式：建造者模式，方便链式赋值
@NoArgsConstructor
@AllArgsConstructor
@TableName("oms_order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID（关联 oms_order.id） */
    private Long orderId;

    /** SKU ID */
    private Long skuId;

    /** 购买数量 */
    private Integer quantity;

    /** 单件售出价 */
    private BigDecimal unitPrice;

    /** 下单时的成本快照 */
    private BigDecimal unitCost;
}
