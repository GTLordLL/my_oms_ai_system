package com.erp.oms.dto.association;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AssociationRuleVO {
    private Long id;

    // 前项商品详细信息 (便于前端渲染图片和名称)
    private List<SkuSimpleInfo> anteProducts;

    // 后项商品详细信息
    private List<SkuSimpleInfo> consProducts;

    // 核心指标
    private BigDecimal support;
    private BigDecimal confidence;
    private BigDecimal lift;

    // 业务建议说明
    // 例如："购买 [可乐] 的用户有 80% 的概率会购买 [薯片]，建议组合摆放"
    private String suggestion;

    // 风险/强度等级：Strong(Lift>2), Medium(Lift>1.2), Weak
    private String strengthLevel;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkuSimpleInfo {
        private String skuCode;
        private String productName;
        private String mainImage;
    }
}