package com.erp.oms.dto.add;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "商品及SKU一站式创建请求")
public class ProductCreateRequest {

    @Valid // 嵌套校验
    private ProductAddDTO product;

    @Valid
    @NotEmpty(message = "至少需要添加一个SKU规格")
    private List<SkuAddDTO> skus;
}