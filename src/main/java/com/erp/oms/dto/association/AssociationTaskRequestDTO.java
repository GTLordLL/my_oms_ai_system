package com.erp.oms.dto.association;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AssociationTaskRequestDTO {
    // 任务 ID，用于 Redis 状态跟踪
    private String taskId;

    /**
     * 事务列表（由 oms_order_item 聚合得到）
     * 结构示例：[["SKU_A", "SKU_B"], ["SKU_A", "SKU_C", "SKU_D"], ...]
     */
    private List<List<String>> transactions;

    /**
     * 最小支持度阈值 (0.0 - 1.0)
     * 默认建议 0.05 (5% 的订单中出现)
     */
    private Double minSupport;

    /**
     * 最小置信度阈值 (0.0 - 1.0)
     * 默认建议 0.3 (买了 A 后有 30% 概率买 B)
     */
    private Double minConfidence;
}