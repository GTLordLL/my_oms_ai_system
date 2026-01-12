package com.erp.oms.dto.rfmAnalysis;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RfmResultDTO {
    private String taskId;
    private List<UserClusterResult> results;

    /**
     * 聚类特征描述 (可选)
     * Key: Cluster ID (0, 1, 2)
     * Value: 算法建议的标签 (如 "高价值", "低频次")
     */
    private Map<Integer, String> clusterLabels;

    @Data
    public static class UserClusterResult {
        private String identifier;
        private Integer cluster;
    }
}