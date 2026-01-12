package com.erp.oms.dto.viewObject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkStatusVO {
    @Schema(description = "Redis 状态机：none, processing, success, error")
    private String status;

    @Schema(description = "关联数据（如成功时的报告ID或对象）")
    private Object data;
}