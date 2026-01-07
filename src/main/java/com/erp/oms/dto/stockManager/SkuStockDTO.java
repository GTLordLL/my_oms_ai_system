package com.erp.oms.dto.stockManager;

import lombok.Data;

@Data
public class SkuStockDTO {
    private String skuCode;
    private String specs;
    private Integer stockQuantity;
    private Boolean lowStock; // 新增字段：是否库存不足
}