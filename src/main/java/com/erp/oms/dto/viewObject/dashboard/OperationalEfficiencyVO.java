package com.erp.oms.dto.viewObject.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "运营时效监控VO")
public class OperationalEfficiencyVO {

    @Schema(description = "平均发货时长（单位：小时）")
    private Double avgDeliverHours;

    @Schema(description = "近30天退货率（退货单/总单）")
    private BigDecimal returnRate;

    @Schema(description = "异常单统计（如：超48小时未发货）")
    private Integer overtimeOrderCount;

    @Schema(description = "时效趋势（按日统计发货时长变化）")
    private List<TimeTrendVO> deliveryEfficiencyTrend;

    @Data
    public static class TimeTrendVO {
        private String date;
        private Double hours;
    }
}