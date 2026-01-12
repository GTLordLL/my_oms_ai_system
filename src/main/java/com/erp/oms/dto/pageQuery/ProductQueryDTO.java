package com.erp.oms.dto.pageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品分页查询请求DTO,用于接收 GET/POST 查询请求")
public class ProductQueryDTO extends PageQueryDTO {
    /**
     * 关键字（商品名称模糊搜索）
     */
    @Schema(description = "商品名称关键字")
    private String keyword;

    /**
     * 商品分类
     */
    @Schema(description = "商品分类")
    private String category;

    /**
     * 商品状态：0-下架，1-上架
     */
    @Schema(description = "商品状态：0-下架，1-上架")
    @Range(min = 0, max = 1, message = "状态值非法")
    private Integer status;

    @Schema(description = "查询起始时间")
    private LocalDateTime startTime;

    @Schema(description = "查询结束时间")
    private LocalDateTime endTime;
}
