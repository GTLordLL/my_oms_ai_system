package com.erp.oms.controller;

import com.erp.oms.dto.add.AiCreateReportDTO;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.dto.viewObject.AiAnalysis.AiReportVO;
import com.erp.oms.dto.viewObject.AiAnalysis.BusinessInsightVO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.service.AiAnalysis.AiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 智能分析接口", description = "集成大模型提供经营诊断与报告管理")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    /**
     * 1. 提供给 AI 工具调用的脱水数据接口
     */
    @GetMapping("/statistical-analysis")
    @Operation(summary = "获取经营脱水指标", description = "聚合五个维度的核心 KPI，专为大模型上下文优化，降低 Token 消耗")
    public ResultVO<BusinessInsightVO> getStatisticalAnalysis() {
        // 调用你之前写好的聚合逻辑
        return ResultVO.success(aiAnalysisService.getFullBusinessInsight());
    }

    /**
     * 2. 接收并保存 AI 生成的分析报告
     * 场景：Dify 工作流执行完毕后，通过 HTTP 节点回调此接口
     */
    @PostMapping("/report/save")
    @Operation(summary = "持久化 AI 分析报告", description = "接收 AI 生成的 Markdown 内容及推理链，并存储至数据库")
    public ResultVO<Long> saveAiReport(@RequestBody @Validated AiCreateReportDTO dto) {
        // 调用你刚才在 Service 中实现的保存逻辑
        Long reportId = aiAnalysisService.saveAiAnalysisReport(dto);
        return ResultVO.success(reportId);
    }

    /**
     * 3. 获取历史报告
     */
    @GetMapping("/report/latest")
    @Operation(summary = "获取最新的分析报告")
    public ResultVO<AiReportVO> getLatestReport() {
        return ResultVO.success(aiAnalysisService.getLatestReportVO());
    }

    @GetMapping("/report/page")
    @Operation(summary = "分页查询历史报告")
    public ResultVO<PageResultVO<AiReportVO>> getReportPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer size) {
        // 这里的返回结构变成了 ResultVO -> PageResultVO -> List<AiReportVO>
        return ResultVO.success(aiAnalysisService.getReportPageVO(page, size));
    }

    @PostMapping("/report/generate")
    @Operation(summary = "触发 AI 报告生成", description = "异步触发 Dify 工作流，前端需轮询状态接口")
    public ResultVO<String> triggerReport() {
        // 1. 获取状态对象
        WorkStatusVO statusVO = aiAnalysisService.checkStatus();

        // 2. 只有在正在处理时才拦截
        if ("processing".equals(statusVO.getStatus())) {
            return ResultVO.error(429, "报告正在生成中，请勿重复点击");
        }

        // 3. 触发异步任务
        aiAnalysisService.triggerReport();

        return ResultVO.success("任务已提交至 AI 分析队列");
    }

    @GetMapping("/report/check-status")
    @Operation(summary = "检查报告生成状态", description = "成功时 data 字段只有Long reportId")
    public ResultVO<WorkStatusVO> checkStatus() {
        // 调用 service，service 内部调用 orchestratorService.getStatusAndData(WORKFLOW_NAME)
        WorkStatusVO statusVO = aiAnalysisService.checkStatus();
        return ResultVO.success(statusVO);
    }

    @GetMapping("/report/get/{id}")
    @Operation(summary = "根据 ID 获取分析报告")
    public ResultVO<AiReportVO> getReportById(@PathVariable Long id) {
        return ResultVO.success(aiAnalysisService.getReportById(id));
    }
}