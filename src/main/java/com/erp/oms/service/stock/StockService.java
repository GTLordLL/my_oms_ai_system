package com.erp.oms.service.stock;

import com.erp.oms.dto.stockManager.StockChangeRequest;
import com.erp.oms.exception.StockException;

public interface StockService {
    /**
     * 按照请求变更库存（入库/出库/调整），并写库存流水。
     *
     * 实现要求：
     *  - 要在同一事务内完成库存表更新与 stock_log 插入
     *  - 出库时需保证不会把库存变为负数（原子 SQL 条件）
     *
     * @param req 请求
     * @throws IllegalArgumentException 请求参数非法
     * @throws StockException 变更失败（如库存不足）
     */
    void changeStock(StockChangeRequest req);
}
