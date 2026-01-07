package com.erp.oms.controller;

import com.erp.oms.dto.stockManager.InboundRequest;
import com.erp.oms.dto.stockManager.OutboundRequest;
import com.erp.oms.dto.stockManager.SkuStockDTO;
import com.erp.oms.dto.stockManager.StockChangeRequest;
import com.erp.oms.entity.Sku;
import com.erp.oms.enums.StockChangeType;
import com.erp.oms.service.sku.SkuService;
import com.erp.oms.service.stock.StockService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    final private SkuService skuService;

    final private StockService stockService;

    public StockController(StockService stockService, SkuService skuService) {
        this.stockService = stockService;
        this.skuService = skuService;
    }

    @GetMapping("/list")
    public List<SkuStockDTO> getStockList() {
        // 1. 调用 MyBatis-Plus 查出所有数据
        List<Sku> skuList = skuService.list();

        // 2. 将 Entity 转换为 DTO（体现设计模式：解耦）
        return skuList.stream().map(sku -> {
            SkuStockDTO dto = new SkuStockDTO();
            dto.setSkuCode(sku.getSkuCode());
            dto.setSpecs(sku.getSpecs());
            dto.setStockQuantity(sku.getStockQuantity());
            // 业务逻辑：如果当前库存 <= 预警线，标记为低库存
            dto.setLowStock(sku.getStockQuantity() <= sku.getAlertQuantity());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 入库接口 - 采购入库
     */
    @PostMapping("/inbound")
    public ResponseEntity<String> inbound(@Valid @RequestBody InboundRequest req) {
        StockChangeRequest changeReq = StockChangeRequest.builder()
                .skuId(req.getSkuId())
                .changeCount(req.getCount()) // 入库为正数
                .type(StockChangeType.PURCHASE_IN)
                .relationId(req.getBillNo())
                .operator("system")
                .build();

        stockService.changeStock(changeReq);
        return ResponseEntity.ok("入库成功");
    }

    /**
     * 出库接口 - 销售出库
     */
    @PostMapping("/outbound")
    public ResponseEntity<String> outbound(@Valid @RequestBody OutboundRequest req) {
        StockChangeRequest changeReq = StockChangeRequest.builder()
                .skuId(req.getSkuId())
                .changeCount(- Math.abs(req.getCount())) // 出库以负数表示
                .type(StockChangeType.SALE_OUT)
                .relationId(req.getOrderSn())
                .operator("system")
                .build();

        stockService.changeStock(changeReq);
        return ResponseEntity.ok("出库成功");
    }
}