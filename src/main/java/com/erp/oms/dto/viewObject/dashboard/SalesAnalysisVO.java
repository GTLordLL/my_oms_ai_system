package com.erp.oms.dto.viewObject.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "销售分析汇总数据")
public class SalesAnalysisVO {

    @Schema(description = "近七日趋势数据")
    private List<DailySalesVO> dailyTrends;

    @Schema(description = "平台销售分布（饼图数据）")
    private List<PlatformStatVO> platformDistribution;

    @Schema(description = "热销商品Top10")
    private List<HotProductVO> hotProducts;

    // --- 内部类：近七日每日明细 ---
    @Data
    public static class DailySalesVO {
        @Schema(description = "日期 (如: 03-10)")
        private String date;
        @Schema(description = "销售额")
        private BigDecimal salesAmount;
        @Schema(description = "订单量")
        private Integer orderCount;
    }

    // --- 内部类：平台分布 ---
    @Data
    public static class PlatformStatVO {
        @Schema(description = "平台名称 (如: TAOBAO)")
        private String platform;
        @Schema(description = "该平台销售总额")
        private BigDecimal value;
    }

    // --- 内部类：热销商品 ---
    @Data
    public static class HotProductVO {
        @Schema(description = "商品名称")
        private String productName;
        @Schema(description = "总销量")
        private Integer totalQuantity;
        @Schema(description = "总销售额")
        private BigDecimal totalAmount;
    }
}