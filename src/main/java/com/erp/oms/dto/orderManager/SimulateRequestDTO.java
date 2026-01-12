package com.erp.oms.dto.orderManager;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "模拟下单请求参数")
public class SimulateRequestDTO {
    @Schema(description = "模拟执行次数", example = "5")
    @Min(value = 1, message = "次数至少为1次")
    @Max(value = 10, message = "单次模拟不建议超过10次，防止Token超限")
    private Integer times;
}