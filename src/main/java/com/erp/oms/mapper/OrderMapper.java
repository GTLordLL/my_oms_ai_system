package com.erp.oms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.oms.dto.viewObject.dashboard.DashboardMetricsVO;
import com.erp.oms.dto.viewObject.dashboard.OperationalEfficiencyVO;
import com.erp.oms.dto.viewObject.dashboard.ProfitAnalysisVO;
import com.erp.oms.dto.viewObject.dashboard.SalesAnalysisVO;
import com.erp.oms.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 1. 核心指标卡片 */
    @Select("SELECT " +
            "  COALESCE(SUM(CASE WHEN create_time::date = CURRENT_DATE AND status != 4 THEN pay_amount ELSE 0 END), 0) as todaySalesAmount, " +
            "  COUNT(CASE WHEN create_time::date = CURRENT_DATE AND status != 4 THEN id END) as todayOrderCount, " +
            "  COUNT(CASE WHEN status = 1 THEN id END) as waitDeliverCount " +
            "FROM oms_order " +
            "WHERE is_deleted = 0")
    DashboardMetricsVO getOrderMetrics();

    /** 2. 近7日销售统计 */
    @Select("SELECT TO_CHAR(create_time, 'MM-DD') as date, " +
            "COALESCE(SUM(pay_amount), 0) as salesAmount, " + // 别名由 sales_amount 改为 salesAmount
            "COUNT(id) as orderCount " + // 别名由 order_count 改为 orderCount
            "FROM oms_order " +
            "WHERE create_time >= CURRENT_DATE - INTERVAL '6 days' " +
            "AND status != 4 AND is_deleted = 0 " +
            "GROUP BY TO_CHAR(create_time, 'MM-DD') " +
            "ORDER BY date ASC")
    List<SalesAnalysisVO.DailySalesVO> getRecentSevenDaysSales();

    /** 3. 平台销售分布 */
    @Select("SELECT source_platform as platform, " + // 别名由 platform_name 改为 platform
            "SUM(pay_amount) as value " + // 别名由 sales_value 改为 value
            "FROM oms_order " +
            "WHERE status != 4 AND is_deleted = 0 " +
            "GROUP BY source_platform")
    List<SalesAnalysisVO.PlatformStatVO> getPlatformDistribution();

    /** 4. 财务核心指标 (已修复 NULL 运算风险与别名映射) */
    @Select("WITH order_base AS (" +
            "  SELECT id, pay_amount, post_fee, create_time " +
            "  FROM oms_order " +
            "  WHERE status != 4 AND is_deleted = 0" +
            "), " +
            "profit_base AS (" +
            "  SELECT order_id, SUM((unit_price - unit_cost) * quantity) as gross_profit " +
            "  FROM oms_order_item " +
            "  GROUP BY order_id" +
            ") " +
            "SELECT " +
            "  COALESCE(SUM(CASE WHEN b.create_time::date = CURRENT_DATE THEN COALESCE(p.gross_profit, 0) - b.post_fee ELSE 0 END), 0) as todayProfit, " +
            "  COALESCE(SUM(CASE WHEN TO_CHAR(b.create_time, 'YYYY-MM') = TO_CHAR(CURRENT_DATE, 'YYYY-MM') THEN COALESCE(p.gross_profit, 0) - b.post_fee ELSE 0 END), 0) as monthProfit, " +
            "  COALESCE(SUM(b.pay_amount), 0) as totalSales " +
            "FROM order_base b " +
            "LEFT JOIN profit_base p ON b.id = p.order_id")
    ProfitAnalysisVO.MetricsVO getProfitMetrics();

    /** 5. 亏本订单预警 */
    @Select("SELECT COUNT(*) FROM (" +
            "  SELECT o.id " +
            "  FROM oms_order o " +
            "  JOIN oms_order_item oi ON o.id = oi.order_id " +
            "  WHERE o.status != 4 AND o.is_deleted = 0 " +
            "  GROUP BY o.id " +
            "  HAVING (SUM(oi.unit_price * oi.quantity) - SUM(oi.unit_cost * oi.quantity) - MAX(o.post_fee)) < 0" +
            ") as loss_orders")
    Integer countLossOrders();

    /** 6. 发货时效趋势 */
    @Select("SELECT TO_CHAR(deliver_time, 'MM-DD') as date, " +
            "AVG(EXTRACT(EPOCH FROM (deliver_time - create_time))/3600) as hours " + // 别名对准 VO 属性名
            "FROM oms_order " +
            "WHERE status IN (2, 3) " +
            "AND deliver_time >= CURRENT_DATE - INTERVAL '6 days' " +
            "AND is_deleted = 0 " +
            "GROUP BY TO_CHAR(deliver_time, 'MM-DD') " +
            "ORDER BY date ASC")
    List<OperationalEfficiencyVO.TimeTrendVO> getDeliveryTimeTrend();

    /** 7. 超时未发货 (48小时) */
    @Select("SELECT COUNT(*) FROM oms_order " +
            "WHERE status = 1 AND is_deleted = 0 " +
            "AND create_time < NOW() - INTERVAL '48 hours'")
    Integer countOvertimeOrders();

    /** 8. 平均发货时长 */
    @Select("SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (deliver_time - create_time))/3600), 0) " +
            "FROM oms_order " +
            "WHERE status IN (2, 3) AND deliver_time IS NOT NULL AND is_deleted = 0")
    Double getAvgDeliverHours();
}