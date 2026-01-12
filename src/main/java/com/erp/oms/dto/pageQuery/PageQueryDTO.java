package com.erp.oms.dto.pageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
@Schema(description = "分页参数")
public class PageQueryDTO {
    /**
     * 当前页，从 1 开始
     */
    @Schema(description = "当前页，从1开始", example = "1")
    @Min(value = 1, message = "页码不能小于1")
    private Long page = 1L;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数", example = "10")
    @Range(min = 1, max = 100, message = "每页条数需在1-100之间")
    private Long size = 10L;
}
