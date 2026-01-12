package com.erp.oms.service.dashboard;

import com.erp.oms.dto.viewObject.dashboard.*;


public interface DashboardService {
    DashboardMetricsVO getDashboardMetrics();
    SalesAnalysisVO getSalesAnalysis();
    StockSupplyChainVO getStockAnalysis();
    ProfitAnalysisVO getProfitAnalysis();
    OperationalEfficiencyVO getEfficiencyAnalysis();
    GlobalDashboardVO getFullDashboard();
}
