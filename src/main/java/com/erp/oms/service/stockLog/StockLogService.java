package com.erp.oms.service.stockLog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.oms.entity.StockLog;

public interface StockLogService extends IService<StockLog> {
    void record(StockLog log);
}
