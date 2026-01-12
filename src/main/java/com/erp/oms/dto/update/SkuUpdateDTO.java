package com.erp.oms.dto.update;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "SKU修改请求参数")
public class SkuUpdateDTO {

    @Schema(description = "SkuID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "SKU编码")
    private String skuCode;

    @Schema(description = "规格属性: 颜色-尺寸")
    private String specs;

    @Schema(description = "采购成本单价")
    @DecimalMin(value = "0.0", inclusive = false, message = "价格必须大于0")
    private BigDecimal costPrice;

    @Schema(description = "吊牌价")
    @DecimalMin(value = "0.0", inclusive = true, message = "价格不能为负数")
    private BigDecimal originPrice;

    @Schema(description = "库存预警线")
    @Min(value = 0, message = "预警线不能为负数")
    private Integer alertQuantity;

    // 注意：这里绝对不要包含 stockQuantity 字段
}