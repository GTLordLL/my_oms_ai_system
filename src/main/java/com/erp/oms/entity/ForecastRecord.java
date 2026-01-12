package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销量预测结果实体类
 * 对应表: oms_forecast_record
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("oms_forecast_record")
public class ForecastRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * SKU 内部 ID
     */
    private Long skuId;

    /**
     * SKU 外部编码
     */
    private String skuCode;

    /**
     * 预测的目标日期 (ds)
     */
    private LocalDate forecastDate;

    /**
     * 预测值 (yhat)
     */
    private BigDecimal forecastValue;

    /**
     * 预测区间上限 (yhat_upper)
     */
    private BigDecimal upperBound;

    /**
     * 预测区间下限 (yhat_lower)
     */
    private BigDecimal lowerBound;

    /**
     * 计算生成时间
     * fill = FieldFill.INSERT 表示插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime computeTime;
}