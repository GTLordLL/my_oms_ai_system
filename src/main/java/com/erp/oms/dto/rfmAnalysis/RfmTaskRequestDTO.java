package com.erp.oms.dto.rfmAnalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RfmTaskRequestDTO {
    private String taskId;
    private List<UserRfmMetric> metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRfmMetric {
        private String identifier;
        private Integer r;
        private Integer f;
        private BigDecimal m; // 使用 BigDecimal 保证金额严谨
    }
}
