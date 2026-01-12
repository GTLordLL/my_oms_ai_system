package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品主表")
@TableName("oms_product")
public class Product {
    @Schema(description = "商品ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "商品名称")
    @NotBlank(message = "商品名称不能为空")
    private String name;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "主图URL")
    private String mainImage;

    @Schema(description = "状态: 0-下架, 1-上架")
    private Integer status;

    @Schema(description = "0-正常, 1-已逻辑删除")
    @TableLogic // 这样当你调用 deleteById 时，MyBatis-Plus 会自动执行逻辑删除（将 0 改为 1），而不是物理删除。
    private Integer isDeleted;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

