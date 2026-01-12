package com.erp.oms.dto.viewObject.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "首页看板全量聚合数据对象 - 包含销售、库存、财务、运营四大核心维度")
public class GlobalDashboardVO {

    @Schema(description = "1. 核心指标卡片 (实时数据汇总)")
    private DashboardMetricsVO metrics;

    @Schema(description = "2. 销售趋势与构成 (折线图、饼图、排行)")
    private SalesAnalysisVO salesAnalysis;

    @Schema(description = "3. 库存与供应链分析 (周转率、积压预警、变动结构)")
    private StockSupplyChainVO stockAnalysis;

    @Schema(description = "4. 财务利润分析 (毛利额、利润率、亏本监控)")
    private ProfitAnalysisVO profitAnalysis;

    @Schema(description = "5. 运营时效监控 (发货效率、退货率)")
    private OperationalEfficiencyVO efficiencyAnalysis;

    @Schema(description = "数据最后更新时间")
    private LocalDateTime lastUpdateTime;
}