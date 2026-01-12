package com.erp.oms.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.oms.enums.StockChangeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Schema(description = "库存流水表")
@TableName("oms_stock_log")
public class StockLog {
    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联SKU ID")
    private Long skuId;

    @Schema(description = "库存变动类型: 1-商品上新, 2-销售出库, 3-退货入库, 4-盘点调整, 5-采购补货, 6-订单取消回库")
    private StockChangeType type;

    @Schema(description = "库存变化量（带正负号）")
    private Integer changeCount;

    @Schema(description = "变动前快照数量")
    private Integer beforeCount;

    @Schema(description = "变动后快照数量")
    private Integer afterCount;

    @Schema(description = "关联单据号（如订单号、入库单号）")
    private String relationId;

    @Schema(description = "操作人名称/ID")
    private String operator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}