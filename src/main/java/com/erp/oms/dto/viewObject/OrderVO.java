package com.erp.oms.dto.viewObject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单显示对象")
public class OrderVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "平台订单号")
    private String platformOrderSn;

    @Schema(description = "来源平台")
    private String sourcePlatform;

    @Schema(description = "订单总额")
    private BigDecimal totalAmount;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "状态码")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName; // 在 Service 中根据 status 转换，如 1 -> "待发货"

    @Schema(description = "买家昵称")
    private String buyerNick;

    @Schema(description = "收件人姓名")
    private String receiverName;

    @Schema(description = "收件人手机号（脱敏）")
    private String receiverMobile;

    @Schema(description = "详细地址")
    private String receiverAddress;

    @Schema(description = "快递单号")
    private String logisticsNo;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}