package com.erp.oms.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页参数")
public class PageQuery {
    /**
     * 当前页，从 1 开始
     */
    @Schema(description = "当前页，从1开始", example = "1")
    private Long page = 1L;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;
}
