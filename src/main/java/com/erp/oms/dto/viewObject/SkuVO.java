package com.erp.oms.dto.viewObject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "SKU显示对象")
public class SkuVO {

    @Schema(description = "SkuID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "SKU编码")
    private String skuCode;

    @Schema(description = "规格属性(如: 红色-XL)")
    private String specs;

    @Schema(description = "采购成本价")
    private BigDecimal costPrice;

    @Schema(description = "吊牌价")
    private BigDecimal originPrice;

    @Schema(description = "当前库存")
    private Integer stockQuantity;

    @Schema(description = "库存预警线")
    private Integer alertQuantity;

    @Schema(description = "库存状态: true-充足, false-预警")
    private Boolean isStockSafe;
}