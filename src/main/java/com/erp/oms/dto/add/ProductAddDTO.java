package com.erp.oms.dto.add;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
@Schema(description = "新增商品请求参数")
public class ProductAddDTO {

    @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过100个字符")
    private String name;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "主图URL")
    private String mainImage;

    @Schema(description = "状态: 0-下架, 1-上架")
    @Range(min = 0, max = 1, message = "状态值非法")
    private Integer status;
}