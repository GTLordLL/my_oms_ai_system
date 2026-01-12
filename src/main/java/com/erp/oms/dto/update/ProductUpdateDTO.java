package com.erp.oms.dto.update;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.util.List;

@Data
@Schema(description = "商品修改请求参数")
public class ProductUpdateDTO {

    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品ID 不能为空")
    private Long id;

    @Schema(description = "商品名称")
    @Size(max = 100, message = "名称长度不能超过100")
    private String name;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "主图URL")
    private String mainImage;

    @Schema(description = "状态: 0-下架, 1-上架")
    @Range(min = 0, max = 1, message = "状态值非法")
    private Integer status;

    @Schema(description = "SKU列表")
    @Valid // 开启嵌套校验
    private List<SkuUpdateDTO> skus;
}