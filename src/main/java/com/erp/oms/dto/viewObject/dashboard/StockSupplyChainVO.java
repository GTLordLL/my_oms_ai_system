package com.erp.oms.dto.viewObject.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "库存与供应链分析VO")
public class StockSupplyChainVO {

    @Schema(description = "高周转SKU排行（前5或前10）")
    private List<SkuTurnoverVO> highTurnoverSkus;

    @Schema(description = "积压库存预警（库存高且久未销售）")
    private List<SkuTurnoverVO> stagnantSkus;

    @Schema(description = "库存变动构成（堆叠柱状图数据）")
    private List<StockMovementVO> movements;

    @Data
    public static class SkuTurnoverVO {
        private String skuCode;
        private String specs;
        private Integer stockQuantity;
        private Integer salesCount30Days; // 近30天出库量
    }

    @Data
    public static class StockMovementVO {
        // 映射数据库原始 code (1, 2, 3...)
        private Integer type;

        // 映射 SQL 中的 total_change
        private Long totalChange;

        // 用于存储转换后的中文描述
        @Schema(description = "入库、出库、退货等")
        private String typeName;
    }
}