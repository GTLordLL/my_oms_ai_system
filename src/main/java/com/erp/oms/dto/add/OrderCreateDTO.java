package com.erp.oms.dto.add;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "手动创建订单请求参数")
public class OrderCreateDTO {

    @Schema(description = "平台订单号（可选，手动创建可自动生成）")
    private String platformOrderSn;

    @Schema(description = "来源平台", example = "MANUAL_CREATE")
    @NotBlank(message = "订单来源不能为空")
    private String sourcePlatform;

    @Schema(description = "订单总金额（商品原价总和）")
    @NotNull(message = "总金额不能为空")
    private BigDecimal totalAmount;

    @Schema(description = "实付金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "实付金额不能为空")
    @DecimalMin(value = "0.0", message = "实付金额不能为负数")
    private BigDecimal payAmount;

    @Schema(description = "运费")
    private BigDecimal postFee;

    @Schema(description = "收件人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "收件人姓名不能为空")
    private String receiverName;

    @Schema(description = "收件人手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    private String receiverMobile;

    @Schema(description = "详细地址")
    private String receiverAddress;

    @Schema(description = "订单明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "订单至少需要包含一个商品")
    @Valid
    private List<OrderItemDTO> items;

    @Data
    @Schema(description = "订单明细DTO")
    public static class OrderItemDTO {
        @Schema(description = "SKU ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "SKU ID不能为空")
        private Long skuId;

        @Schema(description = "购买数量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量至少为1")
        private Integer quantity;

        @Schema(description = "成交单价", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "单价不能为空")
        private BigDecimal unitPrice;
    }
}