package com.erp.oms.controller;

import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.dto.pageQuery.RfmQueryDTO;
import com.erp.oms.dto.rfmAnalysis.RfmAnalysisVO;
import com.erp.oms.dto.rfmAnalysis.RfmDashboardVO;
import com.erp.oms.dto.rfmAnalysis.RfmResultDTO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.service.RfmAnalysis.RfmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rfm")
@Tag(name = "用户价值分层 (RFM) 接口", description = "基于 K-Means 聚类算法自动划分客户等级")
@Validated
public class RfmAnalysisController {

    @Resource
    private RfmService rfmService;

    /**
     * 环节 1：启动 RFM 聚类分析任务
     * 作用：聚合全量用户 RFM 向量，异步推入算法队列
     */
    @PostMapping("/analyze")
    @Operation(summary = "启动 RFM 分析任务", description = "提取全量有效订单进行 R/F/M 建模，返回任务 ID")
    public ResultVO<String> startRfmAnalysis() {
        String taskId = rfmService.initRfmAnalysis();
        return ResultVO.success(taskId);
    }

    /**
     * 环节 2：查询分析任务状态
     */
    @GetMapping("/status/{taskId}")
    @Operation(summary = "查询分析状态", description = "用于前端进度条展示")
    public ResultVO<WorkStatusVO> checkStatus(@PathVariable String taskId) {
        return ResultVO.success(rfmService.getTaskWorkStatus(taskId));
    }

    /**
     * 环节 3 & 4：FastAPI 算法结果回调
     * 作用：接收 Python 端计算出的 Cluster 标签并映射为业务等级
     */
    @PostMapping("/callback")
    @Operation(summary = "RFM 结果回调", description = "由 FastAPI 聚类完成后调用，执行结果入库")
    public ResultVO<Void> callback(@RequestBody RfmResultDTO result) {
        rfmService.handleAlgorithmCallback(result);
        return ResultVO.success();
    }

    /**
     * 环节 5-A：获取 RFM 数据看板
     * 作用：展示饼图统计（各等级占比）和 Top 100 用户
     */
    @GetMapping("/dashboard")
    @Operation(summary = "获取 RFM 看板数据", description = "返回各等级人数占比、平均指标及高价值用户 Top 100")
    public ResultVO<RfmDashboardVO> getDashboard() {
        return ResultVO.success(rfmService.getRfmDashboard());
    }

    /**
     * 环节 5-B：分页查询用户明细 (带搜索)
     * 作用：支持按等级筛选、按手机号/昵称模糊搜索
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询用户价值明细", description = "支持按等级过滤和关键词搜索")
    public ResultVO<PageResultVO<RfmAnalysisVO>> getPage(@RequestBody RfmQueryDTO queryDTO) {
        return ResultVO.success(rfmService.getRfmDetailsPage(queryDTO));
    }
}