package com.erp.oms.controller;

import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.dto.viewObject.dashboard.*;
import com.erp.oms.service.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard - 仪表盘数据中心")
@Validated
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 获取全量看板数据
     * 场景：用户首次打开首页时调用，一次性初始化所有图表
     */
    @GetMapping("/full")
    @Operation(summary = "全量看板聚合", description = "异步组装：核心指标、销售趋势、库存、财务、运营五个维度")
    public ResultVO<GlobalDashboardVO> getFullDashboard() {
        return ResultVO.success(dashboardService.getFullDashboard());
    }

    /**
     * 获取核心统计卡片
     * 场景：用户手动点击“刷新指标”或页面定时轮询
     */
    @GetMapping("/metrics")
    @Operation(summary = "实时核心指标卡片", description = "今日销量、销售额、待发货、库存预警")
    public ResultVO<DashboardMetricsVO> getMetrics() {
        return ResultVO.success(dashboardService.getDashboardMetrics());
    }

    /**
     * 局部刷新：销售趋势与构成分析模块
     */
    @GetMapping("/sales-analysis")
    @Operation(summary = "销售趋势与构成分析模块", description = "包含近七日销售趋势图（折线图）、平台销售占比（饼图）、热销商品排行 (Top 10)")
    public ResultVO<SalesAnalysisVO> getSalesAnalysis() {
        return ResultVO.success(dashboardService.getSalesAnalysis());
    }

    @GetMapping("/stock-analysis")
    @Operation(summary = "库存与供应链分析模块", description = "包含库存周转分析（漏斗图/柱状图）、库存变动构成（堆叠柱状图）")
    public ResultVO<StockSupplyChainVO> getStockAnalysis() {
        return ResultVO.success(dashboardService.getStockAnalysis());
    }

    @GetMapping("/profit-analysis")
    @Operation(summary = "财务利润分析模块", description = "包含今日/本月毛利额、单均利润分析")
    public ResultVO<ProfitAnalysisVO> getProfitAnalysis() {
        return ResultVO.success(dashboardService.getProfitAnalysis());
    }

    @GetMapping("/efficiency-analysis")
    @Operation(summary = "运营时效监控模块", description = "包含平均发货时长、退货率监控")
    public ResultVO<OperationalEfficiencyVO> getEfficiencyAnalysis() {
        return ResultVO.success(dashboardService.getEfficiencyAnalysis());
    }
}