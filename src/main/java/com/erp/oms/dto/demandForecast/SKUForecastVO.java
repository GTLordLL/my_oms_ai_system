package com.erp.oms.dto.demandForecast;

import com.erp.oms.entity.ForecastRecord;
import lombok.Data;

import java.util.List;

/**
 * 前端看板展示对象：支持排序和补货预警
 */
@Data
public class SKUForecastVO {
    // --- 基础信息 ---
    private Long skuId;
    private String skuCode;
    private String productName;
    private String category;

    // --- 实物状态 (来自 oms_sku) ---
    private Integer currentStock = 0;     // 当前库存
    private Integer alertQuantity = 0;    // 库存预警阈值

    // --- 预测结果 (来自 oms_forecast_record) ---
    private Integer demandNext7Days = 0;  // 未来7天预计消耗
    private Integer demandNext30Days = 0; // 未来30天预计消耗

    // --- 业务策略 (核心逻辑) ---
    // 计算逻辑：(7天预测值 + 安全缓冲) - 当前库存
    private Integer suggestedReplenishment = 0;

    // 趋势评估：UP(增长), DOWN(下降), STABLE(平稳)
    private String trend = "STABLE";

    // 缺货风险等级：CRITICAL(立即补货), WARNING(注意), SAFE(安全)
    private String riskLevel = "SAFE";

    // --- 新增：明细记录，专门给前端图表使用 ---
    private List<ForecastRecord> records;
}