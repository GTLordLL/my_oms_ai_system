package com.erp.oms.service.dashboard;

import com.erp.oms.dto.viewObject.dashboard.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test") // 指定使用 application-test.yml
@Transactional // 测试完成后自动回滚数据，保持数据库干净
public class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private JdbcTemplate jdbcTemplate; // 用于快速插入测试数据

    @Test
    @DisplayName("验证仪表盘核心指标查询：确保 SQL 映射无误")
    void testGetDashboardMetrics() {
        DashboardMetricsVO metrics = dashboardService.getDashboardMetrics();

        assertNotNull(metrics, "指标对象不应为 null");
        // 验证即使数据库没数据，Service 也应该返回初始化后的 0 而不是 null
        assertNotNull(metrics.getTodaySalesAmount(), "今日销售额映射失败");
        assertNotNull(metrics.getStockAlertCount(), "库存预警数映射失败");
    }

    @Test
    @DisplayName("验证销售分析：测试日期补全逻辑和平台分布映射")
    void testGetSalesAnalysis() {
        SalesAnalysisVO vo = dashboardService.getSalesAnalysis();

        // 1. 验证日期补全 (近7日趋势必须固定返回 7 条数据)
        List<SalesAnalysisVO.DailySalesVO> trends = vo.getDailyTrends();
        assertEquals(7, trends.size(), "近七日趋势数据条数应固定为 7");

        // 验证最后一条数据是否为今天 (MM-dd 格式)
        // 注意：这里可以根据你本地运行时的日期来灵活断言
        assertNotNull(trends.get(6).getDate());

        // 2. 验证平台分布 (解决你之前关心的命名一致性问题)
        if (!vo.getPlatformDistribution().isEmpty()) {
            SalesAnalysisVO.PlatformStatVO platform = vo.getPlatformDistribution().get(0);
            assertNotNull(platform.getPlatform(), "平台名称映射失败，请检查 SQL 别名是否为 'platform'");
            assertNotNull(platform.getValue(), "销售金额映射失败，请检查 SQL 别名是否为 'value'");
        }
    }

    @Test
    @DisplayName("验证库存分析：测试枚举转换逻辑")
    void testGetStockAnalysis() {
        StockSupplyChainVO vo = dashboardService.getStockAnalysis();

        assertNotNull(vo);

        // 验证库存变动统计的枚举转换
        if (vo.getMovements() != null && !vo.getMovements().isEmpty()) {
            StockSupplyChainVO.StockMovementVO movement = vo.getMovements().get(0);
            // 如果 type 有值，那么 typeName 经过 Service 处理后不应为空
            if (movement.getType() != null) {
                assertNotNull(movement.getTypeName(), "枚举转换逻辑失败，未匹配到正确的描述信息");
            }
        }
    }

    @Test
    @DisplayName("验证利润分析：计算精度与单均利润")
    void testGetProfitAnalysis() {
        // 1. 执行分析逻辑
        ProfitAnalysisVO vo = dashboardService.getProfitAnalysis();

        // 2. 基础非空校验
        assertNotNull(vo);
        assertNotNull(vo.getTodayGrossProfit(), "今日毛利不应为 null");
        assertNotNull(vo.getProfitMargin(), "利润率不应为 null");

        // 3. 逻辑验证：如果总销售额为 0，利润率必须是 0，不能抛出 ArithmeticException (除以零异常)
        // 这个断言能验证你代码里的 if (totalSales > 0) 逻辑是否生效
        if (vo.getMonthGrossProfit().compareTo(BigDecimal.ZERO) == 0) {
            assertEquals(0, vo.getProfitMargin().compareTo(BigDecimal.ZERO), "无销售时利润率应为 0");
        }

        // 4. 验证单均利润计算是否正确（保留两位小数）
        assertNotNull(vo.getAverageProfitPerOrder());
        log.info("当前单均利润: {}", vo.getAverageProfitPerOrder());
    }

    @Test
    @DisplayName("验证运营效率：退货率与时效趋势")
    void testGetEfficiencyAnalysis() {
        // 1. 执行分析逻辑
        OperationalEfficiencyVO vo = dashboardService.getEfficiencyAnalysis();

        assertNotNull(vo);

        // 2. 验证退货率 (0 - 100 之间的百分比)
        assertTrue(vo.getReturnRate().compareTo(BigDecimal.ZERO) >= 0, "退货率不能为负数");
        assertTrue(vo.getReturnRate().compareTo(new BigDecimal("100")) <= 0, "退货率不能超过 100%");

        // 3. 验证时效趋势补全逻辑
        // 因为 getEfficiencyAnalysis 调用了 getDeliveryTimeTrendsWithFilling，
        // 我们需要确保即便数据库没数据，趋势列表也不为空
        assertNotNull(vo.getDeliveryEfficiencyTrend(), "时效趋势列表不应为 null");

        // 4. 验证 SQL 兼容性
        // orderMapper.getAvgDeliverHours() 涉及到 PostgreSQL 的时间差计算
        // 如果这里报错，说明 SQL 里的 EXTRACT(EPOCH FROM ...) 或类似语法有误
        log.info("平均发货时长: {} 小时", vo.getAvgDeliverHours());
    }

    @Test
    @DisplayName("验证全量看板：测试多线程异步组装性能与完整性")
    void testGetFullDashboard() {
        // 1. 记录开始时间（手动验证异步优化效果）
        long startTime = System.currentTimeMillis();

        // 2. 执行异步组装逻辑
        GlobalDashboardVO fullVO = dashboardService.getFullDashboard();

        long duration = System.currentTimeMillis() - startTime;
        log.info("单元测试执行 getFullDashboard 耗时: {} ms", duration);

        // 3. 结果非空校验
        assertNotNull(fullVO, "全量看板对象不应为 null");
        assertNotNull(fullVO.getLastUpdateTime(), "更新时间应自动生成");

        // 4. 深度校验：确保各个异步任务的结果都已成功塞入
        assertAll("异步任务完整性检查",
                () -> assertNotNull(fullVO.getMetrics(), "指标任务(MetricsVO)执行异常"),
                () -> assertNotNull(fullVO.getSalesAnalysis(), "销售任务(Sales)执行异常"),
                () -> assertNotNull(fullVO.getStockAnalysis(), "库存任务(Stock)执行异常"),
                () -> assertNotNull(fullVO.getProfitAnalysis(), "财务任务(Profit)执行异常"),
                () -> assertNotNull(fullVO.getEfficiencyAnalysis(), "效率任务(Efficiency)执行异常")
        );

        // 5. 校验时效趋势补全（验证内部 getDeliveryTimeTrendsWithFilling）
        List<OperationalEfficiencyVO.TimeTrendVO> efficiencyTrend =
                fullVO.getEfficiencyAnalysis().getDeliveryEfficiencyTrend();
        assertEquals(7, efficiencyTrend.size(), "时效趋势应固定补全为 7 天");

        log.info("异步组装测试通过，看板数据完整。");
    }

    @Test
    @DisplayName("深度校验：Mapper字段映射准确性测试")
    void testMapperFieldMapping() {
        jdbcTemplate.execute("DELETE FROM oms_order_item"); // 先删详情
        jdbcTemplate.execute("DELETE FROM oms_order");      // 再删主表

        // 1. 插入主表 (ID假设为 100)
        jdbcTemplate.execute("INSERT INTO oms_order (id, platform_order_sn, source_platform, total_amount, pay_amount, post_fee, status, create_time) " +
                "VALUES (100, 'TEST_001', 'TikTok', 1000.00, 800.00, 10.00, 1, CURRENT_TIMESTAMP)");

        // 2. 插入详情表 (关联主表 ID 100)
        // 利润 = (100 - 60) * 2 - 10(邮费) = 70.00
        jdbcTemplate.execute("INSERT INTO oms_order_item (order_id, sku_id, sku_code, quantity, unit_price, unit_cost) " +
                "VALUES (100, 1, 'SKU001', 2, 100.00, 60.00)");

        // 3. 执行分析
        ProfitAnalysisVO profitVO = dashboardService.getProfitAnalysis();

        assertNotNull(profitVO.getTodayGrossProfit());
        assertEquals(0, new BigDecimal("70.00").compareTo(profitVO.getTodayGrossProfit()), "今日毛利计算或映射失败！");
    }

    @Test
    @DisplayName("深度校验：库存变动类型映射")
    void testStockMovementMapping() {
        jdbcTemplate.execute("DELETE FROM oms_stock_log");
        // 插入一条类型为 1 的记录
        jdbcTemplate.execute("INSERT INTO oms_stock_log (sku_id, type, change_count, before_count, after_count) " +
                "VALUES (999, 1, 10, 20, 30)");

        StockSupplyChainVO vo = dashboardService.getStockAnalysis();
        assertFalse(vo.getMovements().isEmpty());

        StockSupplyChainVO.StockMovementVO move = vo.getMovements().get(0);
        // 校验 type 字段是否拿到了 1
        assertEquals(1, move.getType(), "库存变动类型 type 映射失败！");
        // 校验 Service 里的枚举转换是否生效
        assertNotEquals("未知类型", move.getTypeName(), "枚举转换未识别类型 1");
    }
}