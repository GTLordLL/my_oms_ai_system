package com.erp.oms.service.stock;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.stockManager.StockChangeRequest;
import com.erp.oms.entity.Sku;
import com.erp.oms.entity.StockLog;
import com.erp.oms.exception.StockException;
import com.erp.oms.mapper.SkuMapper;
import com.erp.oms.service.stockLog.StockLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class StockServiceImpl extends ServiceImpl<SkuMapper, Sku> implements StockService{

    private final StockLogService stockLogService;

    public StockServiceImpl(StockLogService stockLogService) {
        this.stockLogService = stockLogService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStock(StockChangeRequest req) {
        // 1. 基本参数校验
        if (req == null) throw new IllegalArgumentException("请求不能为空");
        if (req.getSkuId() == null) throw new IllegalArgumentException("skuId 不能为空");
        if (req.getChangeCount() == null) throw new IllegalArgumentException("changeCount 不能为空");
        int delta = req.getChangeCount();
        if (delta == 0) throw new IllegalArgumentException("changeCount 不能为 0");
        if (req.getType() == null) throw new IllegalArgumentException("type 不能为空");

        Long skuId = req.getSkuId();

        // 2. 原子更新库存（SQL 层计算，避免读写覆盖）
        boolean success = this.lambdaUpdate()
                .setSql("stock_quantity = stock_quantity + " + delta)
                .eq(Sku::getId, skuId)
                // 如果是出库(delta < 0)，确保库存充足
                .ge(Sku::getStockQuantity, delta < 0 ? Math.abs(delta) : 0)
                .update();

        if (!success) {
            // 说明没有行被更新：可能 sku 不存在，或者出库但库存不足
            throw new StockException("更新库存失败：sku 不存在或库存不足（skuId=" + skuId + ", delta=" + delta + "）");
        }

        // 3. 写库存流水（同一事务）
        StockLog log = StockLog.builder()
                .skuId(skuId)
                .changeCount(delta)
                .type(req.getType().getCode())
                .relationId(req.getRelationId())
                .operator(req.getOperator() == null ? "system" : req.getOperator())
                .createTime(LocalDateTime.now())
                .build();

        // 记录库存流水
        stockLogService.record(log);

        // (可选) 这里可以发事件/消息通知其他子系统（如更新缓存、同步库存到搜索引擎等）
    }
}
