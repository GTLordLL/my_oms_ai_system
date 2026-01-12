package com.erp.oms.controller;

import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.dto.demandForecast.ForecastResultDTO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.dto.demandForecast.SKUForecastVO;
import com.erp.oms.service.DemandForecast.ForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forecast")
@Tag(name = "Prophet算法结果展示接口", description = "FastAPI负责Prophet算法运行")
public class ForecastController {

    @Resource
    private ForecastService forecastService;

    // 环节 1：启动预测
    @PostMapping("/start/{skuId}")
    public ResultVO<String> startForecast(@PathVariable Long skuId) {
        return ResultVO.success(forecastService.initManualForecast(skuId));
    }

    // 环节 2：前端轮询状态
    @GetMapping("/status/{taskId}")
    @Operation(summary = "查询预测任务状态", description = "Redis 状态机：none, processing, success, error")
    public ResultVO<WorkStatusVO> checkStatus(@PathVariable String taskId) {
        return ResultVO.success(forecastService.getTaskWorkStatus(taskId));
    }

    // 环节 3：FastAPI 的回调接口 (由 Python 端在算完后异步 POST)
    @PostMapping("/callback")
    public ResultVO<Void> callback(@RequestBody ForecastResultDTO result) {
        // 此时 taskId 已经在 result 对象里了
        forecastService.handleAlgorithmCallback(result);
        return ResultVO.success();
    }

    // 环节 5：手动单点展示
    @GetMapping("/view/{skuId}")
    public ResultVO<SKUForecastVO> getView(@PathVariable Long skuId) {
        return ResultVO.success(forecastService.getForecastView(skuId));
    }

    // 全量展示
    @GetMapping("/list")
    public ResultVO<List<SKUForecastVO>> getList() {
        return ResultVO.success(forecastService.getAllForecastDashboard());
    }
}
