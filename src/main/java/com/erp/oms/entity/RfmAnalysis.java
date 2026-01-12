package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("oms_rfm_analysis")
public class RfmAnalysis {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户唯一标识 (手机号或买家昵称)
     */
    private String buyerIdentifier;

    /**
     * 买家昵称
     */
    private String buyerNick;

    /**
     * R: 最近一次消费天数距离现在的间隔
     */
    private Integer recency;

    /**
     * F: 累计消费频率 (订单量)
     */
    private Integer frequency;

    /**
     * M: 累计消费金额
     */
    private BigDecimal monetary;

    /**
     * K-Means 聚类簇标签 (0, 1, 2...)
     */
    private Integer clusterLabel;

    /**
     * 客户分层名称 (例如：重要价值客户)
     */
    private String customerLevel;

    /**
     * 最近一次订单时间
     */
    private LocalDateTime lastOrderTime;

    /**
     * 计算执行时间
     */
    private LocalDateTime computeTime;
}