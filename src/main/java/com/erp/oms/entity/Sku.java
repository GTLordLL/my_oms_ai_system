package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("oms_sku")
public class Sku {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "productId 不能为空")
    private Long productId;

    @NotBlank(message = "skuCode 不能为空")
    private String skuCode;
    private String specs;

    @NotNull(message = "costPrice 不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "costPrice 必须大于 0")
    private BigDecimal costPrice;   // 成本价

    @DecimalMin(value = "0.0", inclusive = true, message = "originPrice 不能为负数")
    private BigDecimal originPrice; // 吊牌价

    @NotNull(message = "stockQuantity 不能为空")
    @PositiveOrZero(message = "stockQuantity 不能为负数")
    private Integer stockQuantity; // 当前库存

    @NotNull(message = "alertQuantity 不能为空")
    @PositiveOrZero(message = "alertQuantity 不能为负数")
    private Integer alertQuantity; // 预警线
}
