package com.erp.oms.dto.add;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "新增SKU请求参数")
public class SkuAddDTO {
    @Schema(description = "SKU编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "skuCode 不能为空")
    private String skuCode;

    @Schema(description = "规格属性: 颜色-尺寸")
    private String specs;

    @Schema(description = "采购成本单价", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购成本单价不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "成本价必须大于0")
    private BigDecimal costPrice;

    @Schema(description = "吊牌价")
    @DecimalMin(value = "0.0", inclusive = true, message = "吊牌价不能为负数")
    private BigDecimal originPrice;

    @Schema(description = "初始库存", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "初始库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stockQuantity;

    @Schema(description = "库存预警线")
    @NotNull(message = "预警线不能为空")
    @Min(value = 0, message = "预警线不能小于0")
    private Integer alertQuantity;
}