package com.erp.oms.service.AiAnalysis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.oms.dto.add.AiCreateReportDTO;
import com.erp.oms.dto.api.DifyTaskContextDTO;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.association.AssociationRuleVO;
import com.erp.oms.dto.demandForecast.SKUForecastVO;
import com.erp.oms.dto.viewObject.AiAnalysis.AiReportVO;
import com.erp.oms.dto.viewObject.AiAnalysis.BusinessInsightVO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.dto.viewObject.dashboard.*;
import com.erp.oms.entity.AiReport;
import com.erp.oms.mapper.AiReportMapper;
import com.erp.oms.service.AssociationRule.AssociationService;
import com.erp.oms.service.DemandForecast.ForecastService;
import com.erp.oms.service.DifyOrchestrator.OrchestratorService;
import com.erp.oms.service.RfmAnalysis.RfmService;
import com.erp.oms.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {
    private final DashboardService dashboardService;
    private final AiReportMapper aiReportMapper;
    private final OrchestratorService orchestratorService;
    private final ForecastService forecastService;
    private final AssociationService associationService;
    private final RfmService rfmService;

    private static final String WORKFLOW_NAME = "DAILY_REPORT";

    @Value("${dify.workflow.api-key.generate-report}")
    private String difyApiKey;


    @Override
    public BusinessInsightVO getFullBusinessInsight() {
        // 1. 并行或串行获取已有的 5 个模块数据
        DashboardMetricsVO metrics = dashboardService.getDashboardMetrics();
        SalesAnalysisVO sales = dashboardService.getSalesAnalysis();
        StockSupplyChainVO stock = dashboardService.getStockAnalysis();
        ProfitAnalysisVO profit = dashboardService.getProfitAnalysis();
        OperationalEfficiencyVO efficiency = dashboardService.getEfficiencyAnalysis();

        // 2. AI 增强模块数据 (来自你之前实现的 3 个 Service)
        List<SKUForecastVO> highRiskForecasts = forecastService.getHighRiskSkus(5); // 拿风险最高的5个
        List<AssociationRuleVO> strongRules = associationService.getStrongRules(3);  // 拿最强的3个
        List<BusinessInsightVO.RfmLevelInsight> rfmInsights = rfmService.getAILevelInsights();

        BusinessInsightVO insight = new BusinessInsightVO();

        // --- A. 组装摘要 (Summary) ---
        insight.setSummary(BusinessInsightVO.SummaryDTO.builder()
                .todaySales(metrics.getTodaySalesAmount())
                .todayOrders(metrics.getTodayOrderCount())
                .monthGrossProfit(profit.getMonthGrossProfit())
                .overallProfitMargin(profit.getProfitMargin().doubleValue())
                .build());

        // --- B. 组装 AI 模块特征包 (AI Modules) ---
        // 这一步至关重要，AI 报告的内容主要来源于此
        BusinessInsightVO.AIModuleData aiData = new BusinessInsightVO.AIModuleData();
        aiData.setHighRiskForecasts(highRiskForecasts);
        aiData.setKeyAssociationRules(strongRules);
        aiData.setRfmInsights(rfmInsights);
        insight.setAiModules(aiData);

        // --- C. 智能风险扫描 (Risks) - 增加了基于算法预测的预警 ---
        List<BusinessInsightVO.RiskAlertDTO> risks = new ArrayList<>();

        // C1. 基础库存预警
        if (metrics.getStockAlertCount() > 0) {
            risks.add(new BusinessInsightVO.RiskAlertDTO("STOCK", "WARNING", "实物库存：有 " + metrics.getStockAlertCount() + " 件商品低于安全阈值。"));
        }

        // C2. AI 预测断货风险 (新增：跨维度逻辑)
        highRiskForecasts.stream()
                .filter(f -> "CRITICAL".equals(f.getRiskLevel()) || "OUT_OF_STOCK".equals(f.getRiskLevel()))
                .findFirst()
                .ifPresent(f -> risks.add(new BusinessInsightVO.RiskAlertDTO("FORECAST", "CRITICAL",
                        "智能预测：[" + f.getProductName() + "] 未来需求激增，预计库存缺口达 " + f.getSuggestedReplenishment() + " 件。")));

        // C3. 客户流失风险 (新增：基于 RFM 的变动)
        rfmInsights.stream()
                .filter(r -> "流失/边缘客户".equals(r.getLevelName()) && r.getPercentage() > 40.0)
                .findFirst()
                .ifPresent(r -> risks.add(new BusinessInsightVO.RiskAlertDTO("RFM", "WARNING",
                        "客户危机：流失类客户占比已达 " + r.getPercentage() + "%，品牌粘性下降显著。")));

        // C4. 财务与运营风险
        if (profit.getLossOrderCount() > 0) {
            risks.add(new BusinessInsightVO.RiskAlertDTO("PROFIT", "CRITICAL", "亏损预警：今日产生 " + profit.getLossOrderCount() + " 笔亏损订单，需核查定价策略。"));
        }
        // 时效风险
        if (efficiency.getAvgDeliverHours() > 24) {
            risks.add(new BusinessInsightVO.RiskAlertDTO("EFFICIENCY", "WARNING", "物流滞后：平均发货时长已达" + efficiency.getAvgDeliverHours() + "小时。"));
        }
        // 退货风险
        if (efficiency.getReturnRate().doubleValue() > 10.0) {
            risks.add(new BusinessInsightVO.RiskAlertDTO("RETURN", "CRITICAL", "售后预警：退货率升至" + efficiency.getReturnRate() + "，需排查质量问题。"));
        }
        insight.setRisks(risks);

        // 4. 表现评估 (Performance)
        BusinessInsightVO.GrowthPerformanceDTO perf = new BusinessInsightVO.GrowthPerformanceDTO();
        // 提取热销 Top3 名称
        perf.setTopProducts(sales.getHotProducts().stream()
                .limit(3).map(SalesAnalysisVO.HotProductVO::getProductName).collect(Collectors.toList()));

        // 判定销售趋势 (取最近两天对比)
        perf.setSalesTrend(calculateTrend(sales.getDailyTrends()));

        // 提取积压 SKU 编号
        perf.setStagnantSkuCodes(stock.getStagnantSkus().stream()
                .limit(5).map(StockSupplyChainVO.SkuTurnoverVO::getSkuCode).collect(Collectors.toList()));
        insight.setPerformance(perf);

        // 5. 运营时效 (Efficiency)
        insight.setEfficiency(BusinessInsightVO.EfficiencyDTO.builder()
                .avgDeliverHours(efficiency.getAvgDeliverHours())
                .returnRate(efficiency.getReturnRate().doubleValue())
                .isDeliveryDelayed(efficiency.getOvertimeOrderCount() > 0)
                .build());

        return insight;
    }

    /**
     * 内部逻辑：简单对比近两天趋势
     */
    private String calculateTrend(List<SalesAnalysisVO.DailySalesVO> trends) {
        if (trends == null || trends.size() < 2) return "STABLE";
        BigDecimal yesterday = trends.get(trends.size() - 2).getSalesAmount();
        BigDecimal today = trends.getLast().getSalesAmount();

        if (today.compareTo(yesterday) > 0) return "UP";
        if (today.compareTo(yesterday) < 0) return "DOWN";
        return "STABLE";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAiAnalysisReport(AiCreateReportDTO dto) {
        AiReport report = AiReport.builder()
                .reportDate(dto.getReportDate())
                .reportType(dto.getReportType() != null ? dto.getReportType() : 1)
                .content(dto.getContent())
                .reasoning(dto.getReasoning())
                .summaryData(dto.getSummaryData())
                .modelName(dto.getModelName())
                .tokensUsed(dto.getTotalTokens())
                .operator("SYSTEM_AI")
                // 💡 手动设置创建时间，确保数据库里有值可排
                .createTime(LocalDateTime.now())
                .build();

        // 2. 执行入库
        aiReportMapper.insert(report);

        Long reportId = report.getId();

        // 3. 更新状态
        orchestratorService.updateStatusWithData(WORKFLOW_NAME, "success", reportId);

        return reportId;
    }

    @Override
    public AiReportVO getLatestReportVO() {
        // 1. 调用底层的实体查询逻辑
        AiReport entity = aiReportMapper.selectOne(new LambdaQueryWrapper<AiReport>()
                .orderByDesc(AiReport::getCreateTime)
                .last("LIMIT 1"));

        return entity == null ? null : convertToVO(entity);
    }

    @Override
    public PageResultVO<AiReportVO> getReportPageVO(int page, int size) {
        // 1. 构建分页参数
        Page<AiReport> pageParam = new Page<>(page, size);

        // 2. 执行查询
        IPage<AiReport> entityPage = aiReportMapper.selectPage(pageParam,
                new LambdaQueryWrapper<AiReport>()
                        .orderByDesc(AiReport::getCreateTime)); // 按创建时间升序排列

        // 3. 转换：先转换 records 列表
        List<AiReportVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 4. 使用你的静态方法 of 封装结果
        return PageResultVO.of(entityPage.getTotal(), voList);
    }

    @Override
    public AiReportVO getReportById(Long id) {
        // 1. 从数据库查询
        AiReport report = aiReportMapper.selectById(id);

        // 2. 判空处理
        if (report == null) {
            return null;
        }

        // 3. 转换为 VO (建议使用 BeanUtils 或 MapStruct)
        return convertToVO(report);
    }

    /**
     * 提取统一的转换逻辑
     */
    private AiReportVO convertToVO(AiReport entity) {
        if (entity == null) return null;

        AiReportVO vo = new AiReportVO();

        // 1. 自动拷贝（content, reasoning, summaryData, reportDate 等）
        BeanUtils.copyProperties(entity, vo);

        // 2. 处理需要逻辑转换的字段
        // 如果 entity.reportType 是 Integer (1), 转为 vo.reportTypeName ("日报")
        vo.setReportTypeName(convertType(entity.getReportType()));

        // 3. 关于 summaryData：
        // 如果已经在第 1 步拷贝成功，这里可以省略。
        // 如果字段名不一致（比如 entity 里叫 data，VO 里叫 summaryData），则手动赋值：
        // vo.setSummaryData(entity.getSummaryData());

        return vo;
    }

    private String convertType(Integer type) {
        if (type == null) return "未知";
        return switch (type) {
            case 1 -> "经营日报";
            case 2 -> "经营周报";
            case 3 -> "季度分析";
            default -> "自定义报告";
        };
    }

    @Override
    public void triggerReport() {
        DifyTaskContextDTO context = DifyTaskContextDTO.builder()
                .workflowName(WORKFLOW_NAME)
                .apiKey(difyApiKey) // 从配置或数据库读取
                .inputs(Map.of("date", LocalDate.now().toString()))
                .build();

        orchestratorService.executeWorkflow(context);
    }

    @Override
    public WorkStatusVO checkStatus() {
        return orchestratorService.getStatusAndData(WORKFLOW_NAME);
    }
}
