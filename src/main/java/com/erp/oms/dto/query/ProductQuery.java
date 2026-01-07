package com.erp.oms.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品分页查询参数")
public class ProductQuery extends PageQuery{
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
    private Integer status;
}
