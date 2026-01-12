package com.erp.oms.dto.pageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Sku分页查询请求DTO")
public class SkuQueryDTO extends PageQueryDTO {
    /**
     * 商品 ID（查看某个商品下的 SKU）
     */
    @Schema(description = "商品 ID（查看某个商品下的 SKU）")
    private Long productId;

    /**
     * 关键字（SKU 编码 / 规格）
     */
    @Schema(description = "关键字（SKU 编码 / 规格）")
    private String keyword;

    /**
     * 是否库存预警
     * true：只查低库存
     */
    @Schema(description = "是否库存预警")
    private Boolean lowStock;

    // 价格区间查询
    @Schema(description = "最低成本价")
    private BigDecimal minPrice;

    @Schema(description = "最高成本价")
    private BigDecimal maxPrice;
}
