package com.erp.oms.service.stockLog;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.pageQuery.StockLogQueryDTO;
import com.erp.oms.dto.viewObject.StockLogVO;
import com.erp.oms.entity.StockLog;
import com.erp.oms.enums.StockChangeType;

public interface StockLogService extends IService<StockLog> {
    PageResultVO<StockLogVO> pageByQuery(StockLogQueryDTO query);
    void recordLog(Long skuId, Integer changeCount, Integer beforeCount,
                   StockChangeType type, String relationId, String operator);
}
