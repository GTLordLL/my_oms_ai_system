package com.erp.oms.dto.pageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单详情查询参数")
public class OrderItemQueryDTO extends PageQueryDTO {

    @Schema(description = "关联订单ID（精确查询）")
    private Long orderId;

    @Schema(description = "关联 SKU ID（精确查询）")
    private Long skuId;

    @Schema(description = "商家编码/规格快照（模糊查询）")
    private String keyword;

    @Schema(description = "成交价格区间-最小值")
    private BigDecimal minPrice;

    @Schema(description = "成交价格区间-最大值")
    private BigDecimal maxPrice;
}