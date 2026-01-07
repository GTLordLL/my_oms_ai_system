package com.erp.oms.dto.stockManager;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OutboundRequest {
    @NotNull(message = "skuId 不能为空")
    private Long skuId;

    @NotNull(message = "count 不能为空")
    @Min(value = 1, message = "count 必须大于等于 1")
    private Integer count;

    @Size(max = 100)
    private String orderSn;
}
