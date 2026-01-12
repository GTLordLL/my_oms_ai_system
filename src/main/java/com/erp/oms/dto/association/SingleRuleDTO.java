package com.erp.oms.dto.association;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SingleRuleDTO {
    // 前项 (买了这些)：["SKU_001", "SKU_002"]
    private List<String> antecedents;

    // 后项 (推荐这些)：["SKU_099"]
    private List<String> consequents;

    private BigDecimal support;    // 支持度
    private BigDecimal confidence; // 置信度
    private BigDecimal lift;       // 提升度

    // 命中该规则的订单实际数量
    private Integer count;
}