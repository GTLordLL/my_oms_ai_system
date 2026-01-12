package com.erp.oms.dto.viewObject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "库存流水显示对象")
public class StockLogVO {

    @Schema(description = "流水ID")
    private Long id;

    @Schema(description = "关联SKU ID")
    private Long skuId;

    @Schema(description = "SKU 编码")
    private String skuCode;

    @Schema(description = "变动类型编码")
    private Integer type;

    @Schema(description = "变动类型名称")
    private String typeName;

    @Schema(description = "库存变化量")
    private Integer changeCount;

    @Schema(description = "变动前数量")
    private Integer beforeCount;

    @Schema(description = "变动后数量")
    private Integer afterCount;

    @Schema(description = "关联单据号")
    private String relationId;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "记录时间")
    private LocalDateTime createTime;
}