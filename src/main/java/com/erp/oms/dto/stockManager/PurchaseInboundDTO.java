package com.erp.oms.dto.stockManager;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "采购入库请求参数")
public class PurchaseInboundDTO {

    @Schema(description = "SKU ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "SKU ID 不能为空")
    private Long skuId;

    @Schema(description = "入库数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入库数量不能为空")
    @Positive(message = "入库数量必须大于0")
    private Integer changeCount;

    @Schema(description = "采购单据号")
    @NotBlank(message = "关联单据号不能为空")
    private String relationId;

    @Schema(description = "操作人名称")
    private String operator;
}