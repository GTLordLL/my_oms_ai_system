package com.erp.oms.dto.stockManager;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "退货入库请求参数")
public class RefundInboundDTO {
    @Schema(description = "内部订单主表ID (可选)")
    private Long orderId;

    @Schema(description = "平台订单号 (推荐传入)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "平台订单号不能为空")
    private String platformOrderSn;

    @Schema(description = "售后单号/退货单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "单据号不能为空")
    private String relationId;

    @Schema(description = "SKU ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @Schema(description = "退货数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "退货数量不能为空")
    @Positive(message = "退货数量必须大于0")
    private Integer changeCount;

    @Schema(description = "操作人名称")
    private String operator;

    @Schema(description = "备注")
    private String remark;
}