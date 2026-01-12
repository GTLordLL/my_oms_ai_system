package com.erp.oms.dto.demandForecast;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 算法返回结果：包含明细点位和聚合指标
 */
@Data
public class ForecastResultDTO {
    private String taskId;
    private Long skuId;
    private String skuCode;

    // 未来每一天的预测明细
    private List<ForecastPointDTO> predictions;

    // 算法自动聚合的短期/长期指标 (可选，由 Python 端算好或 Java 端算)
    private BigDecimal totalForecast7Days;
    private BigDecimal totalForecast30Days;
}

