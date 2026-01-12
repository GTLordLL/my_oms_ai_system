package com.erp.oms.dto.viewObject.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "财务利润分析VO")
public class ProfitAnalysisVO {

    @Schema(description = "今日预计毛利额")
    private BigDecimal todayGrossProfit;

    @Schema(description = "本月累计毛利额")
    private BigDecimal monthGrossProfit;

    @Schema(description = "单均利润（订单平均利润）")
    private BigDecimal averageProfitPerOrder;

    @Schema(description = "利润率（毛利/销售额）")
    private BigDecimal profitMargin;

    @Schema(description = "亏本订单预警数（利润 < 0）")
    private Integer lossOrderCount;

    /**
     * 用于接收数据库原始查询结果的内部类
     */
    @Data
    public static class MetricsVO {
        private BigDecimal todayProfit;
        private BigDecimal monthProfit;
        private BigDecimal totalSales;
    }
}