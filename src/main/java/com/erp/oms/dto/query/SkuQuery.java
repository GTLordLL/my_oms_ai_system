package com.erp.oms.dto.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SkuQuery extends PageQuery {
    /**
     * 商品 ID（查看某个商品下的 SKU）
     */
    private Long productId;

    /**
     * 关键字（SKU 编码 / 规格）
     */
    private String keyword;

    /**
     * 是否库存预警
     * true：只查低库存
     */
    private Boolean lowStock;
}
