package com.erp.oms.dto.rfmAnalysis;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对应列表显示的 VO
 */
@Data
public class RfmAnalysisVO {
    private String buyerIdentifier;
    private String buyerNick;
    private Integer recency;
    private Integer frequency;
    private BigDecimal monetary;
    private String customerLevel; // "重要价值客户" 等
    private LocalDateTime lastOrderTime;
}