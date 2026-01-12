package com.erp.oms.dto.viewObject.AiAnalysis;

import com.erp.oms.dto.association.AssociationRuleVO;
import com.erp.oms.dto.demandForecast.SKUForecastVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessInsightVO {
    // 1. 核心看板摘要 (来自 metrics)
    private SummaryDTO summary;

    // 2. 风险预警模块 (聚合 stock, profit, efficiency 中的异常)
    private List<RiskAlertDTO> risks;

    // 3. 增长与表现 (来自 sales, stock 的高周转)
    private GrowthPerformanceDTO performance;

    // 4. 运营效率 (来自 efficiency)
    private EfficiencyDTO efficiency;

    // 5. 深度 AI 洞察输入 (来自三个核心模块)
    private AIModuleData aiModules;
    // --- 内部传输对象 ---

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryDTO {
        private BigDecimal todaySales;
        private Integer todayOrders;
        private BigDecimal monthGrossProfit; // 月毛利
        private Double overallProfitMargin;  // 整体毛利率
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RiskAlertDTO {
        private String module;     // FORECAST, ASSOCIATION, RFM, STOCK
        private String level;    // CRITICAL (严重), WARNING (警告)
        private String description; // 例如: "5个SKU库存低于预警值", "今日产生3笔亏损订单"
    }

    @Data
    public static class GrowthPerformanceDTO {
        private List<String> topProducts;      // 热销前三名名称
        private String salesTrend;            // 这里的逻辑由后端判定：UP (增长), DOWN (下降), STABLE (平稳)
        private List<String> stagnantSkuCodes; // 滞销严重的SKU编号
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EfficiencyDTO {
        private Double avgDeliverHours;
        private Double returnRate;
        private Boolean isDeliveryDelayed;    // 后端判定是否超过阈值
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIModuleData {
        // [模块A] 销量预测特征：提取缺货风险最高的 Top 5 SKU
        private List<SKUForecastVO> highRiskForecasts;

        // [模块B] 关联规则特征：提取置信度最高且包含热销品的规则
        private List<AssociationRuleVO> keyAssociationRules;

        // [模块C] 用户价值特征：提取各层级的人数分布及异常变动
        // (例如：流失客户占比是否突然升高)
        private List<RfmLevelInsight> rfmInsights;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RfmLevelInsight {
        private String levelName;
        private Integer userCount;
        private Double percentage;
        private BigDecimal avgMonetary;
        private String trend; // UP, DOWN, STABLE (对比上个周期)
    }
}