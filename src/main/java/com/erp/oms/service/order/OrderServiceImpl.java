package com.erp.oms.service.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.oms.dto.query.OrderQuery;
import com.erp.oms.entity.Order;
import com.erp.oms.exception.BizException;
import com.erp.oms.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    @Override
    public IPage<Order> pageByQuery(OrderQuery query) {
        Page<Order> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Order> w = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getPlatformOrderSn())) {
            w.eq(Order::getPlatformOrderSn, query.getPlatformOrderSn());
        }
        if (StringUtils.hasText(query.getStatus())) {
            w.eq(Order::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getBuyerNick())) {
            w.like(Order::getBuyerNick, query.getBuyerNick());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            w.and(q -> q.like(Order::getPlatformOrderSn, query.getKeyword())
                    .or().like(Order::getBuyerNick, query.getKeyword()));
        }
        if (query.getPayTimeStart() != null && query.getPayTimeEnd() != null) {
            w.between(Order::getPayTime, query.getPayTimeStart(), query.getPayTimeEnd());
        }

        w.orderByDesc(Order::getCreateTime);

        return this.page(page, w);
    }

    @Override
    public Order getByIdOrThrow(Long id) {
        Order order = this.getById(id);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        return order;
    }

    @Override
    public Order create(Order order) {
        this.save(order);
        return order;
    }

    @Override
    public Order updateOrder(Long id, Order order) {
        order.setId(id);
        if (!this.updateById(order)) {
            throw BizException.notFound("订单不存在");
        }
        return this.getById(id);
    }

    @Override
    public void deleteById(Long id) {
        if (!this.removeById(id)) {
            throw BizException.notFound("订单不存在");
        }
    }
}
