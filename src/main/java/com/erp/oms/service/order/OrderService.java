package com.erp.oms.service.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.oms.dto.query.OrderQuery;
import com.erp.oms.entity.Order;


public interface OrderService extends IService<Order> {
    IPage<Order> pageByQuery(OrderQuery query);
    Order getByIdOrThrow(Long id);
    Order create(Order order);
    Order updateOrder(Long id, Order order);
    void deleteById(Long id);
}
