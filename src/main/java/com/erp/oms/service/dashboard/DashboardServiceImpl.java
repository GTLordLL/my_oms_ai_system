package com.erp.oms.service.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.oms.dto.viewObject.dashboard.*;
import com.erp.oms.entity.Order;
import com.erp.oms.enums.OrderStatus;
import com.erp.oms.enums.StockChangeType;
import com.erp.oms.mapper.OrderItemMapper;
import com.erp.oms.mapper.OrderMapper;
import com.erp.oms.mapper.SkuMapper;
import com.erp.oms.mapper.StockLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 仪表盘业务实现：负责全站核心经营数据的聚合
 */
@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {
    private final SkuMapper skuMapper;
    private final OrderMapper orderMapper;
    private final StockLogMapper stockLogMapper;
    private final OrderItemMapper orderItemMapper;

    public DashboardServiceImpl(SkuMapper skuMapper, OrderMapper orderMapper, StockLogMapper stockLogMapper, OrderItemMapper orderItemMapper) {
        this.skuMapper = skuMapper;
        this.orderMapper = orderMapper;
        this.stockLogMapper = stockLogMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public DashboardMetricsVO getDashboardMetrics() {
        // 1. 获取订单相关指标（今日销售额、单量、待发货）
        DashboardMetricsVO metrics = orderMapper.getOrderMetrics();

        // 2. 补充库存预警指标
        if (metrics != null) {
            metrics.setStockAlertCount(skuMapper.countStockAlert());
        } else {
            // 防止数据库为空时返回 null，初始化一个空对象
            metrics = new DashboardMetricsVO();
            metrics.setTodaySalesAmount(BigDecimal.ZERO);
            metrics.setTodayOrderCount(0);
            metrics.setWaitDeliverCount(0);
            metrics.setStockAlertCount(skuMapper.countStockAlert());
        }

        return metrics;
    }

    @Override
    public SalesAnalysisVO getSalesAnalysis() {
        SalesAnalysisVO vo = new SalesAnalysisVO();

        // 1. 获取近七日趋势（含日期补全）
        vo.setDailyTrends(this.getSevenDaysTrends());

        // 2. 获取平台分布
        vo.setPlatformDistribution(orderMapper.getPlatformDistribution());

        // 3. 获取热销排行
        vo.setHotProducts(orderItemMapper.getHotProductsTop10());

        return vo;
    }

    /**
     * 核心逻辑：获取近七日趋势并补全缺失日期
     */
    private List<SalesAnalysisVO.DailySalesVO> getSevenDaysTrends() {
        // A. 从数据库查出有数据的天数
        List<SalesAnalysisVO.DailySalesVO> dbData = orderMapper.getRecentSevenDaysSales();
        Map<String, SalesAnalysisVO.DailySalesVO> dataMap = dbData.stream()
                .collect(Collectors.toMap(SalesAnalysisVO.DailySalesVO::getDate, d -> d));

        // B. 生成最近 7 天的日期序列 (MM-dd)
        List<SalesAnalysisVO.DailySalesVO> fullTrends = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            String dateStr = today.minusDays(i).format(formatter);
            // C. 如果 Map 里有就用数据库的，没有就新建一个 0 数据的 VO
            fullTrends.add(dataMap.getOrDefault(dateStr, createEmptyDaily(dateStr)));
        }
        return fullTrends;
    }

    private SalesAnalysisVO.DailySalesVO createEmptyDaily(String date) {
        SalesAnalysisVO.DailySalesVO daily = new SalesAnalysisVO.DailySalesVO();
        daily.setDate(date);
        daily.setSalesAmount(BigDecimal.ZERO);
        daily.setOrderCount(0);
        return daily;
    }

    @Override
    public StockSupplyChainVO getStockAnalysis() {
        StockSupplyChainVO vo = new StockSupplyChainVO();

        // 1. 获取 SKU 数据 (已适配下划线别名)
        vo.setHighTurnoverSkus(skuMapper.getHighTurnoverSkus());
        vo.setStagnantSkus(skuMapper.getStagnantSkus());

        // 2. 获取变动统计 (直接返回 VO 列表)
        List<StockSupplyChainVO.StockMovementVO> movements = stockLogMapper.getStockMovementStats();

        if (movements != null) {
            // 只需要处理枚举类型的文字转换
            movements.forEach(item -> {
                StockChangeType typeEnum = StockChangeType.fromCode(item.getType());
                item.setTypeName(typeEnum != null ? typeEnum.getDesc() : "未知类型");
            });
            vo.setMovements(movements);
        }

        return vo;
    }

    @Override
    public ProfitAnalysisVO getProfitAnalysis() {
        ProfitAnalysisVO vo = new ProfitAnalysisVO();

        // 1. 获取基础利润数据 (直接映射到对象)
        ProfitAnalysisVO.MetricsVO metricsVO = orderMapper.getProfitMetrics();

        // 如果没有数据，使用默认值初始化，避免后续计算 NPE
        if (metricsVO == null) {
            metricsVO = new ProfitAnalysisVO.MetricsVO();
            metricsVO.setTodayProfit(BigDecimal.ZERO);
            metricsVO.setMonthProfit(BigDecimal.ZERO);
            metricsVO.setTotalSales(BigDecimal.ZERO);
        }

        BigDecimal todayProfit = (metricsVO.getTodayProfit() == null) ? BigDecimal.ZERO : metricsVO.getTodayProfit();
        BigDecimal monthProfit = (metricsVO.getMonthProfit() == null) ? BigDecimal.ZERO : metricsVO.getMonthProfit();
        BigDecimal totalSales = (metricsVO.getTotalSales() == null) ? BigDecimal.ZERO : metricsVO.getTotalSales();

        vo.setTodayGrossProfit(todayProfit);
        vo.setMonthGrossProfit(monthProfit);

        // 2. 亏本订单数
        vo.setLossOrderCount(orderMapper.countLossOrders());

        // 3. 计算单均利润和利润率
        Long todayOrderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .apply("create_time::date = CURRENT_DATE")
                .ne(Order::getStatus, 4)); // 假设 4 是 CLOSED

        // 计算单均利润
        if (todayOrderCount != null && todayOrderCount > 0) {
            vo.setAverageProfitPerOrder(todayProfit.divide(new BigDecimal(todayOrderCount), 2, RoundingMode.HALF_UP));
        } else {
            vo.setAverageProfitPerOrder(BigDecimal.ZERO);
        }

        // 计算利润率 (月利润 / 总销售额)
        if (totalSales != null && totalSales.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = monthProfit.divide(totalSales, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            vo.setProfitMargin(margin);
        } else {
            vo.setProfitMargin(BigDecimal.ZERO);
        }

        return vo;
    }

    // 整合数据并计算退货率，同时处理日期趋势的补全（防止某天没发货导致图表断点）
    @Override
    public OperationalEfficiencyVO getEfficiencyAnalysis() {
        OperationalEfficiencyVO vo = new OperationalEfficiencyVO();

        // 1. 基础指标：平均时长与超时单量
        vo.setAvgDeliverHours(orderMapper.getAvgDeliverHours());
        vo.setOvertimeOrderCount(orderMapper.countOvertimeOrders());

        // 2. 计算近30天退货率
        // A. 获取近30天退货订单数
        Integer refundCount = stockLogMapper.countRefundOrderIn30Days();
        // B. 获取近30天总订单数（排除已关闭）
        Long totalCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, LocalDateTime.now().minusDays(30))
                .ne(Order::getStatus, OrderStatus.CLOSED));

        if (totalCount > 0) {
            vo.setReturnRate(new BigDecimal(refundCount)
                    .divide(new BigDecimal(totalCount), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
        } else {
            vo.setReturnRate(BigDecimal.ZERO);
        }

        // 3. 时效趋势（处理日期补全）
        vo.setDeliveryEfficiencyTrend(this.getDeliveryTimeTrendsWithFilling());

        return vo;
    }

    private List<OperationalEfficiencyVO.TimeTrendVO> getDeliveryTimeTrendsWithFilling() {
        List<OperationalEfficiencyVO.TimeTrendVO> dbData = orderMapper.getDeliveryTimeTrend();

        // 1. 健壮的转换逻辑
        Map<String, Double> dataMap = (dbData == null) ? new HashMap<>() : dbData.stream()
                // 过滤掉 Key 为空的记录，防止 toMap 崩溃
                .filter(vo -> vo != null && vo.getDate() != null)
                .collect(Collectors.toMap(
                        OperationalEfficiencyVO.TimeTrendVO::getDate,
                        // 如果 Value 为空，给个默认值 0.0，防止 toMap 崩溃
                        vo -> vo.getHours() == null ? 0.0 : vo.getHours(),
                        // 如果出现重复 Key（虽然概率低），保留第一个，防止 Duplicate Key 错误
                        (v1, v2) -> v1
                ));

        List<OperationalEfficiencyVO.TimeTrendVO> fullTrend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            String dateStr = today.minusDays(i).format(formatter);
            OperationalEfficiencyVO.TimeTrendVO point = new OperationalEfficiencyVO.TimeTrendVO();
            point.setDate(dateStr);
            // 2. 从 Map 取值，不存在则设为 0.0
            point.setHours(dataMap.getOrDefault(dateStr, 0.0));
            fullTrend.add(point);
        }
        return fullTrend;
    }

    // 在 Dashboard 这种涉及多表、大量聚合计算的场景下，使用 CompletableFuture 进行异步并行组装是性能优化的最佳手段
    @Override
    public GlobalDashboardVO getFullDashboard() {
        long startTime = System.currentTimeMillis();

        // 1. 定义异步任务 (并行执行)

        // 任务 A: 核心指标
        CompletableFuture<DashboardMetricsVO> metricsFuture = CompletableFuture.supplyAsync(this::getDashboardMetrics);

        // 任务 B: 销售分析 (含趋势、分布、热销榜)
        CompletableFuture<SalesAnalysisVO> salesFuture = CompletableFuture.supplyAsync(this::getSalesAnalysis);

        // 任务 C: 库存分析
        CompletableFuture<StockSupplyChainVO> stockFuture = CompletableFuture.supplyAsync(this::getStockAnalysis);

        // 任务 D: 财务利润
        CompletableFuture<ProfitAnalysisVO> profitFuture = CompletableFuture.supplyAsync(this::getProfitAnalysis);

        // 任务 E: 运营时效
        CompletableFuture<OperationalEfficiencyVO> efficiencyFuture = CompletableFuture.supplyAsync(this::getEfficiencyAnalysis);

        // 2. 等待所有任务完成
        CompletableFuture.allOf(metricsFuture, salesFuture, stockFuture, profitFuture, efficiencyFuture).join();

        try {
            // 3. 组装最终结果
            GlobalDashboardVO vo = GlobalDashboardVO.builder()
                    .metrics(metricsFuture.get())
                    .salesAnalysis(salesFuture.get())
                    .stockAnalysis(stockFuture.get())
                    .profitAnalysis(profitFuture.get())
                    .efficiencyAnalysis(efficiencyFuture.get())
                    .lastUpdateTime(LocalDateTime.now())
                    .build();

            log.info("Dashboard 全量数据组装完成，耗时: {} ms", System.currentTimeMillis() - startTime);
            return vo;

        } catch (Exception e) {
            log.error("Dashboard 异步组装发生异常: ", e);
            throw new RuntimeException("看板数据加载失败");
        }
    }
}
