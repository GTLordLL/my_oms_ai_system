package com.erp.oms.controller;

import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.dto.association.AssociationResultDTO;
import com.erp.oms.dto.association.AssociationRuleVO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.service.AssociationRule.AssociationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/association")
@Tag(name = "购物篮关联分析接口", description = "基于 FP-Growth 算法挖掘商品关联规则")
public class AssociationController {

    @Resource
    private AssociationService associationService;

    /**
     * 环节 1：启动全量关联分析任务
     * 作用：提取所有订单事务，异步推入 Redis 队列
     */
    @PostMapping("/analyze")
    @Operation(summary = "启动关联分析任务", description = "触发后立即返回 taskId，后台开始异步计算")
    public ResultVO<String> startAnalysis() {
        String taskId = associationService.initAssociationAnalysis();
        return ResultVO.success(taskId);
    }

    /**
     * 环节 2：查询分析任务状态
     * 作用：供前端 Vue 组件轮询，直到状态变为 success 或 error
     */
    @GetMapping("/status/{taskId}")
    @Operation(summary = "查询分析任务状态", description = "Redis 状态机：processing, success, error")
    public ResultVO<WorkStatusVO> checkStatus(@PathVariable String taskId) {
        return ResultVO.success(associationService.getTaskWorkStatus(taskId));
    }

    /**
     * 环节 3：FastAPI 算法结果回调
     * 作用：接收 Python 端计算出的强关联规则并持久化
     */
    @PostMapping("/callback")
    @Operation(summary = "算法结果回调", description = "由 FastAPI 调用，将规则结果回传给 SpringBoot")
    public ResultVO<Void> callback(@RequestBody AssociationResultDTO result) {
        associationService.handleAlgorithmCallback(result);
        return ResultVO.success();
    }

    /**
     * 环节 5：获取关联规则列表 (View)
     * 作用：当任务成功后，前端调用此接口展示分析出的强关联规则
     */
    @GetMapping("/list")
    @Operation(summary = "获取关联规则列表", description = "获取数据库中已持久化的强关联规则(Lift > 1)")
    public ResultVO<List<AssociationRuleVO>> getRuleList() {
        return ResultVO.success(associationService.getAllRulesVO());
    }

    /**
     * 扩展：手动清理所有规则
     */
    /*
    @DeleteMapping("/clear")
    @Operation(summary = "清空关联规则", description = "手动清空库中所有已生成的关联分析记录")
    public ResultVO<Void> clearRules() {
        associationService.remove(new LambdaQueryWrapper<>());
        return ResultVO.success();
    }
    */
}