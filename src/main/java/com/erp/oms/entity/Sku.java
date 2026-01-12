package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Data
@Builder // 设计模式：建造者模式，方便链式赋值
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SKU库存规格表")
@TableName("oms_sku")
public class Sku {
    @Schema(description = "SkuID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID 不能为空")
    private Long productId;

    @Schema(description = "内部编码/条码")
    @NotBlank(message = "skuCode 不能为空")
    private String skuCode;

    @Schema(description = "规格属性: 颜色-尺寸")
    private String specs;

    @Schema(description = "采购成本单价")
    @NotNull(message = "采购成本单价 不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "采购成本单价 必须大于 0")
    private BigDecimal costPrice;

    @Schema(description = "吊牌价")
    @DecimalMin(value = "0.0", inclusive = true, message = "吊牌价 不能为负数")
    private BigDecimal originPrice;

    @Schema(description = "当前库存")
    @NotNull(message = "当前库存 不能为空")
    @PositiveOrZero(message = "当前库存 不能为负数")
    private Integer stockQuantity;

    @Schema(description = "库存预警线")
    @NotNull(message = "库存预警线 不能为空")
    @PositiveOrZero(message = "库存预警线 不能为负数")
    private Integer alertQuantity;

    @Schema(description = "逻辑删除: 0-正常, 1-已删除", hidden = true)
    @TableLogic
    private Integer isDeleted;
}
