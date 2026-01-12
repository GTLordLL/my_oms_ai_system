package com.erp.oms.dto.viewObject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

// View Object：专门给前端展示用的对象，隐藏了数据库的敏感字段,代替对应的实体类
@Data
@Schema(description = "商品显示对象VO")
public class ProductVO {
    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "主图URL")
    private String mainImage;

    @Schema(description = "状态: 0-下架, 1-上架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // 如果有需要，甚至可以增加一个冗余字段方便前端显示
    // @Schema(description = "状态描述")
    // private String statusDesc;
}
