package com.erp.oms.dto.rfmAnalysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class RfmDashboardVO {
    private Integer totalUsers;
    private List<LevelStat> levelStats;

    // 每一个用户的明细列表 (分页展示)
    private List<RfmAnalysisVO> userDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LevelStat {
        private String levelName;
        private Integer count;
        private Double percentage;
        // 增加维度的平均值，便于前端画雷达图
        private Double avgR;
        private Double avgF;
        private Double avgM;
    }
}