package com.erp.oms.dto.viewObject.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;


@Data
@Schema(description = "实时核心指标卡片")
public class DashboardMetricsVO {

    @Schema(description = "今日销售额")
    private BigDecimal todaySalesAmount;

    @Schema(description = "今日订单量")
    private Integer todayOrderCount;

    @Schema(description = "待发货订单数")
    private Integer waitDeliverCount;

    @Schema(description = "库存预警SKU数")
    private Integer stockAlertCount;
}