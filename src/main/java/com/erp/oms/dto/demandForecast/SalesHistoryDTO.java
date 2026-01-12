package com.erp.oms.dto.demandForecast;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 销售历史序列 DTO
 * 对应 Prophet 所需的 ds 和 y 结构
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesHistoryDTO {
    // 销售日期 (格式: yyyy-MM-dd)
    private LocalDate ds;

    // 销售总量 (该 SKU 在该日的累计销量)
    private Integer y;
}