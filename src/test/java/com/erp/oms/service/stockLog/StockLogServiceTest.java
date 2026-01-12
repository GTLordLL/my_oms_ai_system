package com.erp.oms.service.stockLog;

import com.erp.oms.entity.StockLog; // 确保导入了你的实体类
import com.erp.oms.enums.StockChangeType;
import com.erp.oms.mapper.StockLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 重点：必须手动添加以下静态导入
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockLogServiceTest {

    @InjectMocks
    private StockLogServiceImpl stockLogService;

    @Mock
    private StockLogMapper stockLogMapper;

    @Test
    void testRecordLog_SaleOut_ShouldBeNegative() {
        // 1. 准备数据
        Long skuId = 1L;
        Integer changeCount = 10;
        Integer beforeCount = 100;

        // 2. 执行业务逻辑
        stockLogService.recordLog(skuId, changeCount, beforeCount,
                StockChangeType.SALE_OUT, "ORDER123", "Admin");

        // 3. 断言验证
        // 关键修正点 1: 将 insert 强转为单个对象的调用，解决重载歧义
        // 关键修正点 2: lambda 参数显式声明 (StockLog log)，解决泛型推导失败
        verify(stockLogMapper).insert(argThat((StockLog log) -> {
            // 逻辑验证
            Integer realChange = log.getChangeCount();
            Integer afterCount = log.getAfterCount();

            // 销售出库变动应为 -10，变动后应为 90
            return realChange != null && realChange == -10
                    && afterCount != null && afterCount == 90;
        }));
    }
}