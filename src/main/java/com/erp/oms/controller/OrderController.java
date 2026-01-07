package com.erp.oms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.oms.dto.api.Result;
import com.erp.oms.dto.query.OrderQuery;
import com.erp.oms.entity.Order;
import com.erp.oms.service.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 创建订单
    @PostMapping
    public Order add(@Valid @RequestBody Order order) {
        return orderService.create(order);
    }

    // 通过id查询订单
    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.getByIdOrThrow(id);
    }

    // 通过条件查询订单
    @PostMapping("/page")
    public Result<IPage<Order>> pageOrders(@RequestBody OrderQuery query) {

        return Result.success(orderService.pageByQuery(query));
    }

    // 通过id修改订单
    @PutMapping("/{id}")
    public Order update(@PathVariable Long id, @Valid @RequestBody Order order) {
        return orderService.updateOrder(id, order);
    }

    // 通过id删除订单
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        orderService.deleteById(id);
    }

}
