package com.erp.oms.dto.update;

import com.erp.oms.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "订单更新请求参数")
public class OrderUpdateDTO {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long id;

    @Schema(description = "收件人姓名")
    private String receiverName;

    @Schema(description = "收件人手机号")
    private String receiverMobile;

    @Schema(description = "详细地址")
    private String receiverAddress;

    @Schema(description = "快递单号（发货时填写）")
    private String logisticsNo;

    @Schema(description = "订单状态（用于手动流转状态，如发货）")
    private OrderStatus status;
}