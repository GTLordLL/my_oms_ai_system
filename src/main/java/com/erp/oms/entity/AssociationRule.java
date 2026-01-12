package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物篮关联规则记录实体
 * 对应表: oms_association_rule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("oms_association_rule")
public class AssociationRule {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 前项商品ID集合 (多个ID以逗号分隔，例如 "101,102")
     */
    private String anteSkuIds;

    /**
     * 前项商品名称冗余 (方便前端直接展示，例如 "可乐, 薯片")
     */
    private String anteNames;

    /**
     * 后项商品ID集合 (推荐购买的商品，例如 "103")
     */
    private String consSkuIds;

    /**
     * 后项商品名称冗余 (例如 "炸鸡")
     */
    private String consNames;

    /**
     * 支持度 (Support)：A和B同时出现的概率
     */
    private BigDecimal support;

    /**
     * 置信度 (Confidence)：买了A之后买B的条件概率
     */
    private BigDecimal confidence;

    /**
     * 提升度 (Lift)：反映关联强度的核心指标，必须 > 1
     */
    private BigDecimal lift;

    /**
     * 规则维度：SKU 或 CATEGORY (品类)
     */
    private String ruleType;

    /**
     * 该规则在历史订单中出现的实际次数
     */
    private Integer occurrenceCount;

    /**
     * 计算生成时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime computeTime;
}