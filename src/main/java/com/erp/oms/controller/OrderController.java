package com.erp.oms.controller;

import com.erp.oms.dto.add.OrderCreateDTO;
import com.erp.oms.dto.api.PageResultVO;
import com.erp.oms.dto.api.ResultVO;
import com.erp.oms.dto.orderManager.CancelOrderDTO;
import com.erp.oms.dto.orderManager.SimulateRequestDTO;
import com.erp.oms.dto.pageQuery.OrderQueryDTO;
import com.erp.oms.dto.update.OrderUpdateDTO;
import com.erp.oms.dto.viewObject.CompositeVO.OrderFullVO;
import com.erp.oms.dto.viewObject.OrderVO;
import com.erp.oms.dto.viewObject.WorkStatusVO;
import com.erp.oms.service.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "订单管理页")
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "通过id查询订单")
    @GetMapping("/get/{id}")
    public ResultVO<OrderVO> getById(@PathVariable Long id) {
        OrderVO orderVO = orderService.getByOrderId(id);
        return ResultVO.success(orderVO);
    }

    @Operation(summary = "查询订单详情(含明细)")
    @GetMapping("/details/{id}")
    public ResultVO<OrderFullVO> getOrderDetails(@PathVariable Long id) {
        return ResultVO.success(orderService.getOrderDetails(id));
    }

    @Operation(summary = "分页查询订单")
    @PostMapping("/page")
    public ResultVO<PageResultVO<OrderVO>> pageOrders(@RequestBody @Validated OrderQueryDTO query) {
        return ResultVO.success(orderService.pageByQuery(query));
    }

    @Operation(summary = "手动创建新订单")
    @PostMapping("/create")
    public ResultVO<Void> create(@Validated @RequestBody OrderCreateDTO orderCreateDTO) {
        orderService.createManualOrder(orderCreateDTO);
        return ResultVO.success();
    }

    @Operation(summary = "手动修改订单状态")
    @PutMapping("/update")
    public ResultVO<Void> update(@Validated @RequestBody OrderUpdateDTO dto){
        orderService.updateOrderManual(dto);
        return ResultVO.success();
    }

    @Operation(summary = "手动取消关闭订单")
    @PutMapping("/cancel/{orderId}")
    public ResultVO<Void> cancelOrder(@PathVariable Long orderId, @RequestBody CancelOrderDTO cancelOrderDTO){
        orderService.closeOrder(orderId,cancelOrderDTO.getOperator());
        return ResultVO.success();
    }

    @Operation(summary = "手动执行订单发货")
    @PutMapping("/ship/{id}")
    public ResultVO<Void> ship(@PathVariable Long id, @RequestParam @NotBlank(message = "物流单号不能为空") String logisticsNo) {
        // 假设当前操作员信息从上下文获取，手动录入暂传 "ADMIN"
        orderService.shipOrder(id, logisticsNo, "ADMIN");
        return ResultVO.success();
    }

    @PostMapping("/ai/simulate")
    @Operation(summary = "ai模拟顾客下单", description = "异步触发 Dify 工作流，前端需轮询状态接口")
    public ResultVO<String> triggerSimulate(@RequestBody @Valid SimulateRequestDTO request) {
        // 1. 获取状态对象
        WorkStatusVO statusVO = orderService.checkWorkflowStatus();

        // 2. 只有在正在处理时才拦截
        if ("processing".equals(statusVO.getStatus())) {
            return ResultVO.error(429, "ai正在模拟顾客下单中，请勿重复点击");
        }

        // 3. 触发异步任务
        orderService.trigSimCustomer(request.getTimes());

        return ResultVO.success("已提交 " + request.getTimes() + " 个模拟任务");
    }

    @GetMapping("/ai/simulate/check-status")
    @Operation(summary = "检查ai模拟顾客下单状态", description = " ")
    public ResultVO<WorkStatusVO> checkStatus() {
        // 调用 service，service 内部调用 orchestratorService.getStatusAndData(WORKFLOW_NAME)
        WorkStatusVO statusVO = orderService.checkWorkflowStatus();
        return ResultVO.success(statusVO);
    }
}
