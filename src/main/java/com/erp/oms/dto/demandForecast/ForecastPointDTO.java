package com.erp.oms.dto.demandForecast;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 预测点位明细
 */
@Data
public class ForecastPointDTO {
    private LocalDate ds;          // 预测日期
    private BigDecimal yhat;       // 预测中心值
    private BigDecimal yhatUpper;  // 预测上限 (考虑波动后的最高可能需求)
    private BigDecimal yhatLower;  // 预测下限 (最保守需求)
}