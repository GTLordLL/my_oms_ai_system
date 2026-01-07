package com.erp.oms.service.stockLog;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.entity.StockLog;
import com.erp.oms.exception.StockException;
import com.erp.oms.mapper.StockLogMapper;
import org.springframework.stereotype.Service;

@Service
public class StockLogServiceImpl extends ServiceImpl<StockLogMapper, StockLog> implements StockLogService{
    @Override
    public void record(StockLog log) {
        boolean success = this.save(log);
        if (!success) {
            throw new StockException("写库存流水失败");
        }
    }
}
