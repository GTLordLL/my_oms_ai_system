package com.erp.oms.dto.pageQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "库存流水查询参数")
public class StockLogQueryDTO extends PageQueryDTO {

    @Schema(description = "关联 SKU ID")
    private Long skuId;

    @Schema(description = "SKU编码/关联单据号（模糊查询）")
    private String keyword;

    @Schema(description = "变动类型: 1-采购入库, 2-销售出库, 3-退货入库, 4-盘点调整")
    private Integer type;

    @Schema(description = "操作人名称")
    private String operator;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}