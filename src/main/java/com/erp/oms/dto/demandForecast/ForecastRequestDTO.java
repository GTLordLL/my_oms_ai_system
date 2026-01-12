package com.erp.oms.dto.demandForecast;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 核心请求对象：包含历史序列和预测参数
 */
@Data
@Builder
public class ForecastRequestDTO {
    private Long skuId;
    private String skuCode;

    // 预测步长 (例如输入 30，表示预测未来 30 天)
    private Integer horizon;

    // 历史销售序列 (由 SpringBoot 聚合 oms_order 得到)
    private List<SalesHistoryDTO> history;
}