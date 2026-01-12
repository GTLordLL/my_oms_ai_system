package com.erp.oms.dto.pageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单分页查询参数")
public class OrderQueryDTO extends PageQueryDTO {
    /**
     * 平台订单号（精确）
     */
    @Schema(description = "平台订单号（精确查询）")
    private String platformOrderSn;

    /**
     * 来源平台（TAOBAO / JD / PDD）
     */
    @Schema(description = "来源平台")
    private String sourcePlatform;

    @Schema(description = "订单状态: 1-待发货, 2-已发货, 3-已完成, 4-已关闭, 5-售后中")
    private Integer status;

    @Schema(description = "综合关键字（订单号/买家昵称/收件人手机号/快递单号）")
    private String keyword;

    @Schema(description = "支付时间范围")
    private LocalDateTime payTimeStart;
    private LocalDateTime payTimeEnd;

    @Schema(description = "创建时间范围")
    private LocalDateTime createTimeStart;
    private LocalDateTime createTimeEnd;

    @Schema(description = "实付金额范围")
    private BigDecimal minPayAmount;
    private BigDecimal maxPayAmount;
}
